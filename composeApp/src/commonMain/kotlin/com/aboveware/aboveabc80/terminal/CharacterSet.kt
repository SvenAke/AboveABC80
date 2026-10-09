package com.aboveware.aboveabc80.terminal

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.Abc80MonitorCharacterMap
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Handles the VT320 terminal character set (font) ROM.
 * This implementation uses the interleaved bit format for 80/132 column modes
 * and includes per-glyph metadata for vertical alignment.
 */
object CharacterSet {
    private var rawData: ByteArray? = null
    private var rawData52: ByteArray? = null
    private var rawData2513u: ByteArray? = null
    private var rawData2513l: ByteArray? = null
    private var activeSet: BaseCharacterSet? = null

    val fallbackSet = object : BaseCharacterSet() {}

    var space: Glyph? = null
    var e: Glyph? = null
    var error: Glyph? = null
    private var _characterSets: CharacterSets? = null
    val characterSets: CharacterSets
        get() = _characterSets ?: throw IllegalStateException("CharacterSet not loaded")

    fun isLoaded() = _characterSets != null

    /**
     * Used for unit tests where Compose resources might not be available.
     */
    fun initForTests(
        data: ByteArray,
        data52: ByteArray,
        data2513u: ByteArray? = null,
        data2513l: ByteArray? = null
    ) {
        rawData = data
        rawData52 = data52
        rawData2513u = data2513u
        rawData2513l = data2513l
        _characterSets =
            CharacterSets(data, data52, data2513u ?: byteArrayOf(), data2513l ?: byteArrayOf())
        activeSet = characterSets.asciiCharacterSet
        space = Glyph(' ', GLYPH_0X000, data)
        e = Glyph('E', GLYPH_0X045, data)
        error = GlyphGraphics('⸮', GLYPH_0X126, data)
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        try {
            rawData = Res.readBytes("files/23-054E7.bin")
            rawData52 = Res.readBytes("files/vt52rom.bin")
            rawData2513u = Res.readBytes("files/2513u.bin")
            rawData2513l = Res.readBytes("files/2513l.bin")
            _characterSets = CharacterSets(rawData!!, rawData52!!, rawData2513u!!, rawData2513l!!)
            activeSet = characterSets.asciiCharacterSet
            space = Glyph(' ', GLYPH_0X000, rawData!!)
            e = Glyph('E', GLYPH_0X045, rawData!!)
            error = GlyphGraphics('⸮', GLYPH_0X126, rawData!!)

            Abc80Log.terminal("CharacterSet: load() COMPLETED SUCCESSFULLY. _characterSets is not null.")
        } catch (e: Exception) {
            Abc80Log.wtf("CharacterSet: load() FAILED: ${e.message}")
            e.printStackTrace()
        }
    }

    class CharacterSets(
        data: ByteArray,
        data52: ByteArray,
        data2513u: ByteArray,
        data2513l: ByteArray
    ) {
        val frenchCanadianCharacterSet = FrenchCanadian(data)
        val norwegianDanishCharacterSet = DenmarkNorway(data)
        val swissCharacterSet = Swiss(data)
        val swedishCharacterSet = Swedish(data)
        val spanishCharacterSet = Spanish(data)
        val italianCharacterSet = Italian(data)
        val germanCharacterSet = German(data)
        val frenchCharacterSet = French(data)
        val finnishCharacterSet = Finnish(data)
        val portugueseCharacterSet = Portuguese(data)
        val dutchCharacterSet = Dutch(data)
        val britishCharacterSet = UnitedKingdom(data)
        val decSpecialGraphicsCharacterSet = SpecialGraphics(data)
        val asciiCharacterSet = ASCIICharacterSet(data)
        val decSupplementalCharacterSet = DECSupplementalCharacterSet(data)
        val isoLatinAlphabetNr1SupplementCharacterSet =
            ISOLatinAlphabetNr1SupplementCharacterSet(data)

        fun preferredSupplementalCharacterSet() = decSupplementalCharacterSet

        val drcsFontBuffer = DRCSFontBuffer(data)
        val vt52CharacterSet = VT52CharacterSet(data52)
        val vt52SpecialGraphics = VT52SpecialGraphics(data52)
        val abc80CharacterSet = ABC80CharacterSet()

        fun all() = listOf(
            frenchCanadianCharacterSet,
            norwegianDanishCharacterSet,
            swissCharacterSet,
            swedishCharacterSet,
            spanishCharacterSet,
            italianCharacterSet,
            germanCharacterSet,
            frenchCharacterSet,
            finnishCharacterSet,
            portugueseCharacterSet,
            dutchCharacterSet,
            britishCharacterSet,
            decSpecialGraphicsCharacterSet,
            asciiCharacterSet,
            decSupplementalCharacterSet,
            isoLatinAlphabetNr1SupplementCharacterSet,
            drcsFontBuffer,
            abc80CharacterSet
        )
    }


    fun getGlyph(char: Char): Glyph? {
        return activeSet?.getGlyph(char)
    }

    // --- Core Classes ---

    open class Glyph(
        val char: Char,
        val address: Int = 0,
        var data: ByteArray = byteArrayOf(0x00),
        val columnRange80: IntRange = 2..13,
        val columnRange132: IntRange = 0..7,
        val glyphWidth80: Int = 15,
        val glyphWidth132: Int = 9,
        val height: Int = 12,
        val isVT52: Boolean = false,
    ) {
        var parsed = false

        private var _rowRange: IntRange? = null
        val rowRange: IntRange
            get() {
                if (_rowRange == null) {
                    _rowRange = if (isVT52) {
                        0..9
                    } else {
                        val b = if (address < data.size) data[address].toInt() and 0xFF else 0
                        val start = (b shr 4) and 0x0F
                        val end = b and 0x0F
                        // DEC format: bits 7-4 is start scanline, 3-0 is end scanline
                        // If start is 1 and end is 12, it means scanlines 0 to 11
                        (start - 1)..<end
                    }
                }
                return _rowRange!!
            }

        private var _length: Int? = null
        private val length: Int
            get() {
                if (_length == null) {
                    val rows = (rowRange.last - rowRange.first)
                    _length = if (isVT52) {
                        32
                    } else {
                        // All VT320 ROM characters are interleaved (Table 1 or Table 2)
                        // Table 1 (Standard): 12 (80-col) + 8 (132-col) = 20 bits/row
                        // Table 2 (Graphics): 15 (80-col) + 9 (132-col) = 24 bits/row
                        val bitsPerRow = if (this is GlyphGraphics) 24 else 20
                        bitsPerRow * rows
                    }
                }
                return _length!!
            }

        val data80 = Array(height) { BooleanArray(glyphWidth80) { false } }
        val data132 = Array(height) { BooleanArray(glyphWidth132) { false } }

        open fun parse() {
            if (parsed) return

            if (data.isEmpty()) {
                val globalData = if (isVT52) rawData52 else rawData
                if (globalData != null) {
                    data = globalData
                    _rowRange = null
                    _length = null
                } else {
                    return
                }
            }

            parsed = true

            if (isVT52) {
                parseVT52()
                return
            }

            val bits = mutableListOf<Boolean>()
            var addr = address
            var remainingLength = length

            while (remainingLength > 0 && addr + 1 < data.size) {
                var byte = data[++addr].toInt()
                repeat(8) {
                    bits.add((0x80 and byte) != 0)
                    byte = byte shl 1
                }
                remainingLength -= 8
            }

            var index = 0
            val bitsPerRow = if (this is GlyphGraphics) 24 else 20

            for (row in rowRange.first until rowRange.last) {
                // Must always consume ALL bits in the interleaved row (Table 1=20, Table 2=24)
                // to stay synchronized with the bitstream, even if the row is outside [0, height)

                if (row !in 0 until height) {
                    index += bitsPerRow
                    continue
                }

                for (col in columnRange80) {
                    if (index < bits.size) {
                        data80[row][col] = bits[index++]
                    }
                }
                for (col in columnRange132) {
                    if (index < bits.size) {
                        data132[row][col] = bits[index++]
                    }
                }
            }
        }

        private fun parseVT52() {
            var addr = address
            for (row in 0..7) {
                if (addr + 3 >= data.size) break
                val o1 = data[addr++].toInt().toChar()
                val o2 = data[addr++].toInt().toChar()
                val o3 = data[addr++].toInt().toChar()
                var byte = try {
                    ("$o1$o2$o3".toInt(8).inv() and 0x7F)
                } catch (_: Exception) {
                    0
                }

                for (bit in 1..9) { // columnRange80 for VT52
                    if (bit < glyphWidth80) {
                        data80[row][bit] = (0x80 and byte) != 0
                    }
                    byte = byte shl 1
                }
                addr++ // Skip space
            }
        }

        fun data80(): Array<BooleanArray> {
            parse()
            return data80
        }

        fun data132(): Array<BooleanArray> {
            parse()
            return data132
        }
    }

    class GlyphGraphics(char: Char, address: Int, data: ByteArray) :
        Glyph(char, address, data, 0..<15, 0..<9, height = 12)

    class GlyphVT52(char: Char, address: Int, data: ByteArray) : Glyph(
        char, address, data, (1..9), glyphWidth80 = 10, height = 10, isVT52 = true
    )

    class Glyph2513(char: Char, address: Int, data: ByteArray, private val yOffset: Int = 0) :
        Glyph(
            char, address, data, (0..4), glyphWidth80 = 5, height = 10
        ) {
        override fun parse() {
            if (parsed) return
            parsed = true
            for (row in 0 until 8) {
                val targetRow = row + yOffset
                if (targetRow >= height) break

                val addr = address + row
                if (addr >= data.size) break
                val byte = data[addr].toInt()
                for (col in 0 until 5) {
                    // 2513 usually has 5 bits in lower positions.
                    // Assume bit 4 is leftmost of the 5-bit pattern.
                    data80[targetRow][col] = (byte and (1 shl (4 - col))) != 0
                }
            }
        }
    }

    abstract class BaseCharacterSet {
        val chars = mutableMapOf<Int, Glyph>()

        fun getGlyph(char: Char): Glyph? {
            val g = chars[char.code]
            g?.parse()
            return g
        }

        /**
         * Returns the internal code (7-bit or 8-bit) for a given Unicode character.
         * Useful for mapping keyboard input back to terminal codes in NRC mode.
         */
        fun getCode(char: Char): Int {
            val result = chars.entries.find { it.value.char == char }?.key ?: char.code
            if (char == '~' || char == '`') {
                Abc80Log.keyboard("CharacterSet: getCode for '$char' returned $result")
            }
            return result
        }
    }

    class DRCSFontBuffer(val data: ByteArray) : ASCIICharacterSet(data) {
        private var designator = ""

        init {
            clear(designator)
        }

        class SoftGlyph(char: Char, characterMatrixHeight: Int, characterMatrixWidth: Int) :
            Glyph(
                char,
                height = characterMatrixHeight,
                glyphWidth80 = characterMatrixWidth,
                glyphWidth132 = characterMatrixWidth
            ) {
            init {
                parsed = true
            }
        }

        fun clear(designator: String) {
            if (designator == this.designator) {
                this.designator = ""
                val err = error ?: Glyph(' ', GLYPH_0X000, byteArrayOf())
                (0x00..0xff).forEach {
                    chars[it] = err
                }
            }
        }

        fun designator() = designator

        fun load(soft: SoftCharacterSet) {
            designator = soft.dscs
            val is94 = soft.characterSetSize == 0
            when (soft.eraseControl) {
                0 -> {
                    chars.forEach { entry ->
                        if (soft.fontWidth == 2) {
                            entry.value.data132().forEach { row -> row.fill(false) }
                        } else {
                            entry.value.data80().forEach { row -> row.fill(false) }
                        }
                    }
                }

                2 -> {
                    chars.forEach { entry ->
                        entry.value.data132().forEach { row -> row.fill(false) }
                        entry.value.data80().forEach { row -> row.fill(false) }
                    }
                }

                else -> {
                }
            }
            val startIndex = if (is94) {
                if (soft.startingCharacter <= 1) 0x21 else 0x20 + soft.startingCharacter
            } else {
                0x20 + soft.startingCharacter
            }
            soft.forEachIndexed { index, sixel ->
                val char = (startIndex + index).toChar()
                if (char.code > 0xFF) return@forEachIndexed

                // Track actual max width seen for this character to enable sharp centering
                var actualWidth = 0
                sixel.forEach { row ->
                    actualWidth = maxOf(actualWidth, row.size)
                }
                if (actualWidth == 0) actualWidth = soft.characterMatrixWidth

                val softGlyph =
                    SoftGlyph(char, soft.characterMatrixHeight, actualWidth)

                sixel.forEachIndex { lineIndex, line ->
                    if (lineIndex < soft.characterMatrixHeight) {
                        val targetRow = if (soft.fontWidth == 2)
                            softGlyph.data132()[lineIndex]
                        else
                            softGlyph.data80()[lineIndex]

                        for (col in 0 until minOf(line.size, targetRow.size)) {
                            targetRow[col] = line[col]
                        }
                    }
                }
                chars[char.code] = softGlyph
            }
        }
    }

    class SoftCharacterSet {
        /**
         * Down-Line-Loading a Soft Character Set (DECDLD)
         * DCS Pfn ; Pcn ; Pe ; Pcmw ; Pw ; Pt ; Pcmh ; Pcss { Dscs Sxbp1 ; Sxbp2 ; ... ; Sxbpn ST
         *
         * DECDLD Parameter Characters
         *      Name	            Description
         * Pfn  Font number         Selects the DRCS font buffer to load.
         *                          The VT320 has one DRCS font buffer.
         *                          Pfn has two valid values, 0 and 1.
         *                          Both values refer to the same DRCS buffer.
         *
         * Pcn	Starting character  Selects where to load the first character in
         *                          the DRCS font buffer. The location corresponds
         *                          to a location in the ASCII code table (Section 2).
         *
         *                          Pcn is affected by the character set size.
         *                          (See Pcss below.) In a 94-character set, a
         *                          Pcn value of 0 or 1 means that the first soft
         *                          character is loaded into position 2/1 of the
         *                          character table. In a 96-character set, a Pcn
         *                          value of 0 means the first character is loaded
         *                          into position 2/0 of the character table. The
         *                          greatest Pcn value is 95 (position 7/15).
         *
         * Pe	    Erase control   Selects which characters to erase from the DRCS
         *                          buffer before loading the new font
         *                          0 =	erase all characters in the DRCS buffer with
         *                              this number, width and rendition.
         *                          1 =	erase only characters in locations being
         *                              reloaded.
         *                          2 =	erase all renditions of the soft character
         *                              set (normal, bold, 80-column, 132-column).
         * Pcmw	    Character matrix width Selects the maximum character cell width.
         *
         *                          VT300 modes
         *                          0  = 15 pixels wide for 80 columns, 9 pixels wide
         *                               for 132 columns. (D)
         *                          1  = illegal.
         *                          2  = 5 x 10 pixel cell	|	VT220 compatible
         *                          3  = 6 x 10 pixel cell	|
         *                          4  = 7 x 10 pixel cell	|
         *                          5  = 5 pixels wide.
         *                          6  = 6 pixels wide.
         *                          15= 15 pixels wide.
         * If you omit a Pcmw value, the terminal uses the default character width.
         * Any Pcmw value over 15 is illegal.
         *
         * Use Pcmw values 2 through 4 with VT220 compatible software. Remember that
         * VT220 fonts appear different on the VT320. Fonts designed specifically for
         * the VT320 should use values 5 through 15.
         *
         * Pw	Font width	        Selects the number of columns per line (font set size).
         *                          0 =	80 columns. (Default)
         *                          1 =	80 columns.
         *                          2 =	132 columns.
         * Pt   Text or full-cell	Defines the font as a text font or full-cell font.
         *                          0 =	text. (Default)
         *                          1 =	text.
         *                          2 =	full cell.
         *                          Full-cell fonts can individually address all pixels in a cell.
         *
         * Text fonts cannot individually address all pixels. If you specify a text cell, the
         * terminal automatically performs spacing and centering of the characters.
         *
         * Pcmh	    Character matrix height	Selects the maximum character cell height.
         *                          0 or omitted = 12 pixels high. (Default)
         *                          1 =	1 pixel high.
         *                          2 =	2 pixels high.
         *                          3 =	3 pixels high.
         *                          .
         *                          .
         *                          12=	12 pixels high.
         * Pcmh values over 12 are illegal. If the value of Pcmw is 2, 3 or 4, Pcmh is ignored.
         *
         * Pcss	Character set size	Defines the character set as a 94- or 96-character graphic set.
         *                          0 =	94-character set. (Default)
         *                          1 =	96-character set.
         * The value of Pcss changes the meaning of the Pcn (starting character) parameter above.
         *
         *                          If Pcss = 0 (94-character set)
         *                          The terminal ignores any attempt to load characters into
         *                          the 2/0 or 7/15 table positions.
         *                          Pcn	Specifies
         *                              1	column 2/row 1
         *                              .
         *                              .
         *                              94	column 7/row 14
         *                          If Pcss = 1 (96-character set)
         *                              Pcn	Specifies
         *                              0	column 2/row 0
         *                              .
         *                              .
         *                              95	column 7/row 15
         * Dscs defines the character set name. You use this name in the select character set
         * (SCS) escape sequence. You use the following format for the Dscs name
         *
         * I I F where
         *
         * I I are zero to two intermediate characters, from the range 2/0 to 2/15 in the ASCII
         * character set.
         * F is a final character in the range 3/0 to 7/14.
         *
         * Sxbp1 ; Sxbp2 ; ... ; Sxbpn are the sixel bit patterns for individual characters,
         * separated by semicolons (3/11). Your character set can have 1 to 94 patterns or 1 to
         * 96 patterns, depending on the setting of the character set size parameter (Pcss).
         * Each sixel bit pattern is in the following format:
         *
         * S...S/S...S
         *
         * where
         *
         * the first S...S represents the upper columns of sixels of the soft character.
         * / (2/5) advances the sixel pattern to the lower columns of the soft character.
         * the second S...S represents the lower columns of the soft character.
         *
         * Valid DECDLD Parameter Combinations
         * Pcmw	    Pt	    Pcmh	    Pw
         * 80-Column Fonts
         * 0 to 12	0, 1	0 to 12	    0, 1
         * 0 to 15	2	    0 to 12	    0, 1
         * 132-Column Fonts
         * 0 to 7	0, 1	0 to 12	    2
         * 0 to 9	2	    0 to 12	    2
         *
         * Clearing a Soft Character Set
         * You can clear a soft character set that you loaded into the terminal by using the following DECDLD control string.
         *
         * DCS 1;1;2 { sp @ ST
         *
         * Any of the following actions also clear the soft character set:
         *
         * Performing the power-up self-test.
         * Selecting the Recall or Reset features in the Set-Up Directory.
         * Using a reset to initial state (RIS) or ESC c sequence.
         */


        class Sixel {
            private val sixels = mutableListOf<MutableList<Int>>()
            private var currentSixelRow = mutableListOf<Int>()

            private var repeatCount = 0
            private var inRepeat = false

            fun add(code: Char): Boolean {
                if (code == '!') {
                    inRepeat = true
                    repeatCount = 0
                    return true
                }

                if (inRepeat) {
                    if (code in '0'..'9') {
                        repeatCount = repeatCount * 10 + (code - '0')
                        return true
                    } else {
                        // The character after the count is the one to repeat
                        val bits = code.code - 0x3f
                        repeat(maxOf(0, repeatCount)) {
                            currentSixelRow.add(bits)
                        }
                        inRepeat = false
                        repeatCount = 0
                        return true
                    }
                }

                currentSixelRow.add(code.code - 0x3f)
                return true
            }

            fun next(): String {
                sixels.add(currentSixelRow)
                currentSixelRow = mutableListOf()
                inRepeat = false
                return ""
            }

            fun forEachIndex(action: (Int, BooleanArray) -> Unit) {
                var index = 0
                forEach { action(index++, it) }
            }

            fun forEach(action: (BooleanArray) -> Unit) {
                sixels.forEach { sits ->
                    (0..5).forEach { bit ->
                        val bool = mutableListOf<Boolean>()
                        sits.forEach { bits ->
                            bool.add(((bits shr bit) and 0x01) != 0)
                        }
                        action(BooleanArray(bool.size) { i -> bool[i] })
                    }
                }
            }
        }

        private val sixels = mutableListOf<Sixel>()
        fun forEach(action: (Sixel) -> Unit) {
            sixels.forEach { action(it) }
        }

        fun forEachIndexed(action: (Int, Sixel) -> Unit) {
            var index = 0
            sixels.forEach { action(index++, it) }
        }

        private var sixel: Sixel? = null
        var dscs = ""
        var fontNumber = 0
        var startingCharacter = 0
        var eraseControl = 0
        var characterMatrixWidth = 0
        var fontWidth = 0
        var textOrFullCell = 0
        var characterMatrixHeight = 0
        var characterSetSize = 0

        /*
         * Dscs defines the name for the soft character set. You use this name in the
         * select character set (SCS) escape sequence. You use the following format
         * for the Dscs name:
         *      I F
         *      I is 0, 1 or 2 intermediate characters from the range 0x20 to 0x2F in
         *          the ASCII character set.
         *      F is a final character in the range 0x30 to 0x7E.
         */
        fun name(code: Char): Boolean {
            dscs += sixel?.next() ?: code
            return dscs.length < 3
        }

        /*
         * Add final character to the dscs or more data to the sixel
         */
        fun add(code: Char): Boolean {
            if (sixel != null) {
                if (code == '/') {
                    sixel!!.next()
                    return true
                }
                if (code in '\u0021'..'\u007e') {
                    return sixel!!.add(code)
                }
                return true // Ignore other characters inside data
            }
            if (code in '\u0020'..'\u002F') {
                dscs += code
                return true
            }
            if (code in '\u0030'..'\u007E') {
                dscs += code
                sixel = Sixel()
                return true
            }
            return false
        }

        fun done(): Boolean {
            sixel?.let {
                it.next()
                sixels.add(it)
            }
            sixel = Sixel()
            return true
        }

        fun finish() {
            done()
        }

        fun isEmpty() = sixels.isEmpty()

        /**
         * Parse the parameters and return true if they are OK.
         * Pfn ; Pcn ; Pe ; Pcmw ; Pw ; Pt ; Pcmh ; Pcss
         */
        /*        fun parse(parameters: Parameters): Boolean {
                    characterSetSize = parameters[Pcss].value
                    fontNumber = parameters[Pfn].value
                    startingCharacter = parameters[Pcn].value
                    eraseControl = parameters[Pe].value
                    characterMatrixWidth = parameters[Pcmw, if (characterSetSize == 2) "6" else "10"].value
                    fontWidth = parameters[Pw].value
                    textOrFullCell = parameters[Pt].value
                    characterMatrixHeight = parameters[Pcmh, "16"].value

                    return (0..1).contains(fontNumber) &&
                            (0..95).contains(startingCharacter) &&
                            (0..2).contains(eraseControl) &&
                            // setOf(0, 2..6, 15).contains(characterMatrixWidth) &&
                            (0..2).contains(fontWidth) &&
                            (0..2).contains(textOrFullCell) &&
                            // (0..12).contains(characterMatrixHeight) &&
                            (0..1).contains(characterSetSize) &&
                            characterMatrixWidth <= 10 &&
                            characterMatrixHeight <= 16
                }*/

        fun print() {
            forEach { sixel ->
                sixel.forEach {
                    it.forEach { bit ->
                        if (bit) print("*") else print(" ")
                    }
                    println()
                }
            }
        }

        fun designator() = dscs
    }

    open class ASCIICharacterSet(data: ByteArray) : BaseCharacterSet() {
        init {
            val space = Glyph(' ', GLYPH_0X000, data)
            val e = Glyph('E', GLYPH_0X045, data)

            chars[0x00] = Glyph('␀', GLYPH_0X000, data)
            chars[0x01] = Glyph('␁', GLYPH_0X001, data)
            chars[0x02] = Glyph('␂', GLYPH_0X002, data)
            chars[0x03] = Glyph('␂', GLYPH_0X003, data)
            chars[0x04] = Glyph('␄', GLYPH_0X004, data)
            chars[0x05] = Glyph('␅', GLYPH_0X005, data)
            chars[0x06] = Glyph('␆', GLYPH_0X006, data)
            chars[0x07] = Glyph('␇', GLYPH_0X007, data)
            chars[0x08] = Glyph('␈', GLYPH_0X008, data)
            chars[0x09] = Glyph('␉', GLYPH_0X009, data)
            chars[0x0A] = Glyph('\u000a', GLYPH_0X00A, data)
            chars[0x0B] = Glyph('\u000b', GLYPH_0X00B, data)
            chars[0x0C] = Glyph('\u000c', GLYPH_0X00C, data)
            chars[0x0D] = Glyph('\u000d', GLYPH_0X00D, data)
            chars[0x0E] = Glyph('\u000e', GLYPH_0X00E, data)
            chars[0x0F] = Glyph('␏', GLYPH_0X00F, data)
            chars[0x10] = Glyph('␐', GLYPH_0X010, data)
            chars[0x11] = Glyph('␑', GLYPH_0X011, data)
            chars[0x12] = Glyph('␒', GLYPH_0X012, data)
            chars[0x13] = Glyph('␓', GLYPH_0X013, data)
            chars[0x14] = Glyph('␔', GLYPH_0X014, data)
            chars[0x15] = Glyph('␕', GLYPH_0X015, data)
            chars[0x16] = Glyph('␖', GLYPH_0X016, data)
            chars[0x17] = Glyph('␗', GLYPH_0X017, data)
            chars[0x18] = Glyph('␘', GLYPH_0X018, data)
            chars[0x19] = Glyph('␙', GLYPH_0X019, data)
            chars[0x1A] = Glyph('␚', GLYPH_0X01A, data)
            chars[0x1B] = Glyph('␛', GLYPH_0X01B, data)
            chars[0x1C] = Glyph('␜', GLYPH_0X01C, data)
            chars[0x1D] = Glyph('␝', GLYPH_0X01D, data)
            chars[0x1E] = Glyph('␞', GLYPH_0X01E, data)
            chars[0x1F] = Glyph('␟', GLYPH_0X01F, data)
            chars[0x20] = space
            chars[0x21] = Glyph('!', GLYPH_0X021, data)
            chars[0x22] = Glyph('"', GLYPH_0X022, data)
            chars[0x23] = Glyph('#', GLYPH_0X023, data)
            chars[0x24] = Glyph('$', GLYPH_0X024, data)
            chars[0x25] = Glyph('%', GLYPH_0X025, data)
            chars[0x26] = Glyph('&', GLYPH_0X026, data)
            chars[0x27] = Glyph('\'', GLYPH_0X027, data)
            chars[0x28] = Glyph('(', GLYPH_0X028, data)
            chars[0x29] = Glyph(')', GLYPH_0X029, data)
            chars[0x2A] = Glyph('*', GLYPH_0X02A, data)
            chars[0x2B] = Glyph('+', GLYPH_0X02B, data)
            chars[0x2C] = Glyph(',', GLYPH_0X02C, data)
            chars[0x2D] = Glyph('-', GLYPH_0X02D, data)
            chars[0x2E] = Glyph('.', GLYPH_0X02E, data)
            chars[0x2F] = Glyph('/', GLYPH_0X02F, data)
            chars[0x30] = Glyph('0', GLYPH_0X030, data)
            chars[0x31] = Glyph('1', GLYPH_0X031, data)
            chars[0x32] = Glyph('2', GLYPH_0X032, data)
            chars[0x33] = Glyph('3', GLYPH_0X033, data)
            chars[0x34] = Glyph('4', GLYPH_0X034, data)
            chars[0x35] = Glyph('5', GLYPH_0X035, data)
            chars[0x36] = Glyph('6', GLYPH_0X036, data)
            chars[0x37] = Glyph('7', GLYPH_0X037, data)
            chars[0x38] = Glyph('8', GLYPH_0X038, data)
            chars[0x39] = Glyph('9', GLYPH_0X039, data)
            chars[0x3A] = Glyph(':', GLYPH_0X03A, data)
            chars[0x3B] = Glyph(';', GLYPH_0X03B, data)
            chars[0x3C] = Glyph('<', GLYPH_0X03C, data)
            chars[0x3D] = Glyph('=', GLYPH_0X03D, data)
            chars[0x3E] = Glyph('>', GLYPH_0X03E, data)
            chars[0x3F] = Glyph('?', GLYPH_0X03F, data)
            chars[0x40] = Glyph('@', GLYPH_0X040, data)
            chars[0x41] = Glyph('A', GLYPH_0X041, data)
            chars[0x42] = Glyph('B', GLYPH_0X042, data)
            chars[0x43] = Glyph('C', GLYPH_0X043, data)
            chars[0x44] = Glyph('D', GLYPH_0X044, data)
            chars[0x45] = e
            chars[0x46] = Glyph('F', GLYPH_0X046, data)
            chars[0x47] = Glyph('G', GLYPH_0X047, data)
            chars[0x48] = Glyph('H', GLYPH_0X048, data)
            chars[0x49] = Glyph('I', GLYPH_0X049, data)
            chars[0x4A] = Glyph('J', GLYPH_0X04A, data)
            chars[0x4B] = Glyph('K', GLYPH_0X04B, data)
            chars[0x4C] = Glyph('L', GLYPH_0X04C, data)
            chars[0x4D] = Glyph('M', GLYPH_0X04D, data)
            chars[0x4E] = Glyph('N', GLYPH_0X04E, data)
            chars[0x4F] = Glyph('O', GLYPH_0X04F, data)
            chars[0x50] = Glyph('P', GLYPH_0X050, data)
            chars[0x51] = Glyph('Q', GLYPH_0X051, data)
            chars[0x52] = Glyph('R', GLYPH_0X052, data)
            chars[0x53] = Glyph('S', GLYPH_0X053, data)
            chars[0x54] = Glyph('T', GLYPH_0X054, data)
            chars[0x55] = Glyph('U', GLYPH_0X055, data)
            chars[0x56] = Glyph('V', GLYPH_0X056, data)
            chars[0x57] = Glyph('W', GLYPH_0X057, data)
            chars[0x58] = Glyph('X', GLYPH_0X058, data)
            chars[0x59] = Glyph('Y', GLYPH_0X059, data)
            chars[0x5A] = Glyph('Z', GLYPH_0X05A, data)
            chars[0x5B] = Glyph('[', GLYPH_0X05B, data)
            chars[0x5C] = Glyph('\\', GLYPH_0X05C, data)
            chars[0x5D] = Glyph(']', GLYPH_0X05D, data)
            chars[0x5E] = Glyph('^', GLYPH_0X05E, data)
            chars[0x5F] = Glyph('_', GLYPH_0X05F, data)
            chars[0x60] = Glyph('`', GLYPH_0X060, data)
            chars[0x61] = Glyph('a', GLYPH_0X061, data)
            chars[0x62] = Glyph('b', GLYPH_0X062, data)
            chars[0x63] = Glyph('c', GLYPH_0X063, data)
            chars[0x64] = Glyph('d', GLYPH_0X064, data)
            chars[0x65] = Glyph('e', GLYPH_0X065, data)
            chars[0x66] = Glyph('f', GLYPH_0X066, data)
            chars[0x67] = Glyph('g', GLYPH_0X067, data)
            chars[0x68] = Glyph('h', GLYPH_0X068, data)
            chars[0x69] = Glyph('i', GLYPH_0X069, data)
            chars[0x6A] = Glyph('j', GLYPH_0X06A, data)
            chars[0x6B] = Glyph('k', GLYPH_0X06B, data)
            chars[0x6C] = Glyph('l', GLYPH_0X06C, data)
            chars[0x6D] = Glyph('m', GLYPH_0X06D, data)
            chars[0x6E] = Glyph('n', GLYPH_0X06E, data)
            chars[0x6F] = Glyph('o', GLYPH_0X06F, data)
            chars[0x70] = Glyph('p', GLYPH_0X070, data)
            chars[0x71] = Glyph('q', GLYPH_0X071, data)
            chars[0x72] = Glyph('r', GLYPH_0X072, data)
            chars[0x73] = Glyph('s', GLYPH_0X073, data)
            chars[0x74] = Glyph('t', GLYPH_0X074, data)
            chars[0x75] = Glyph('u', GLYPH_0X075, data)
            chars[0x76] = Glyph('v', GLYPH_0X076, data)
            chars[0x77] = Glyph('w', GLYPH_0X077, data)
            chars[0x78] = Glyph('x', GLYPH_0X078, data)
            chars[0x79] = Glyph('y', GLYPH_0X079, data)
            chars[0x7A] = Glyph('z', GLYPH_0X07A, data)
            chars[0x7B] = Glyph('{', GLYPH_0X07B, data)
            chars[0x7C] = Glyph('|', GLYPH_0X07C, data)
            chars[0x7D] = Glyph('}', GLYPH_0X07D, data)
            chars[0x7E] = Glyph('~', GLYPH_0X07E, data)
            chars[0x7F] = Glyph('␡', GLYPH_0X07F, data) // DEL (Rubout)

            chars[0x80] = Glyph('\u0080', GLYPH_0X080, data)
            chars[0x81] = Glyph('\u0081', GLYPH_0X081, data)
            chars[0x82] = Glyph('\u0082', GLYPH_0X082, data)
            chars[0x83] = Glyph('\u0083', GLYPH_0X083, data)
            chars[0x84] = Glyph('\u0084', GLYPH_0X084, data)
            chars[0x85] = Glyph('\u0085', GLYPH_0X085, data)
            chars[0x86] = Glyph('\u0086', GLYPH_0X086, data)
            chars[0x87] = Glyph('\u0087', GLYPH_0X087, data)
            chars[0x88] = Glyph('\u0088', GLYPH_0X088, data)
            chars[0x89] = Glyph('\u0089', GLYPH_0X089, data)
            chars[0x8A] = Glyph('\u008a', GLYPH_0X08A, data)
            chars[0x8B] = Glyph('\u008b', GLYPH_0X08B, data)
            chars[0x8C] = Glyph('\u008c', GLYPH_0X08C, data)
            chars[0x8D] = Glyph('\u008d', GLYPH_0X08D, data)
            chars[0x8E] = Glyph('\u008e', GLYPH_0X08E, data)
            chars[0x8F] = Glyph('\u008f', GLYPH_0X08F, data)
            chars[0x90] = Glyph('\u0090', GLYPH_0X090, data)
            chars[0x91] = Glyph('\u0091', GLYPH_0X091, data)
            chars[0x92] = Glyph('\u0092', GLYPH_0X092, data)
            chars[0x93] = Glyph('\u0093', GLYPH_0X093, data)
            chars[0x94] = Glyph('\u0094', GLYPH_0X094, data)
            chars[0x95] = Glyph('\u0095', GLYPH_0X095, data)
            chars[0x96] = Glyph('\u0096', GLYPH_0X096, data)
            chars[0x97] = Glyph('\u0097', GLYPH_0X097, data)
            chars[0x98] = Glyph('\u0098', GLYPH_0X098, data)
            chars[0x99] = Glyph('\u0099', GLYPH_0X099, data)
            chars[0x9A] = Glyph('\u009a', GLYPH_0X09A, data)
            chars[0x9B] = Glyph('\u009b', GLYPH_0X09B, data)
            chars[0x9C] = Glyph('\u009c', GLYPH_0X09C, data)
            chars[0x9D] = Glyph('\u009d', GLYPH_0X09D, data)
            chars[0x9E] = Glyph('\u009e', GLYPH_0X09E, data)
            chars[0x9F] = Glyph('\u009f', GLYPH_0X09F, data)
            chars[0xA0] = Glyph('\u00a0', GLYPH_0X0A0, data)
            chars[0xA1] = Glyph('\u00a1', GLYPH_0X0A1, data)
            chars[0xA2] = Glyph('\u00a2', GLYPH_0X0A2, data)
            chars[0xA3] = Glyph('\u00a3', GLYPH_0X0A3, data)
            chars[0xA4] = Glyph('\u00a4', GLYPH_0X0A4, data)
            chars[0xA5] = Glyph('\u00a5', GLYPH_0X0A5, data)
            chars[0xA6] = Glyph('\u00a6', GLYPH_0X0A6, data)
            chars[0xA7] = Glyph('\u00a7', GLYPH_0X0A7, data)
            chars[0xA8] = Glyph('\u00a8', GLYPH_0X0A8, data)
            chars[0xA9] = Glyph('\u00a9', GLYPH_0X0A9, data)
            chars[0xAA] = Glyph('\u00aa', GLYPH_0X0AA, data)
            chars[0xAB] = Glyph('\u00ab', GLYPH_0X0AB, data)
            chars[0xAC] = Glyph('\u00ac', GLYPH_0X0AC, data)
            chars[0xAD] = Glyph('\u00ad', GLYPH_0X0AD, data)
            chars[0xAE] = Glyph('\u00ae', GLYPH_0X0AE, data)
            chars[0xAF] = Glyph('\u00af', GLYPH_0X0AF, data)
            chars[0xB0] = Glyph('\u00b0', GLYPH_0X0B0, data)
            chars[0xB1] = Glyph('\u00b1', GLYPH_0X0B1, data)
            chars[0xB2] = Glyph('\u00b2', GLYPH_0X0B2, data)
            chars[0xB3] = Glyph('\u00b3', GLYPH_0X0B3, data)
            chars[0xB4] = Glyph('\u00b4', GLYPH_0X0B4, data)
            chars[0xB5] = Glyph('\u00b5', GLYPH_0X0B5, data)
            chars[0xB6] = Glyph('\u00b6', GLYPH_0X0B6, data)
            chars[0xB7] = Glyph('\u00b7', GLYPH_0X0B7, data)
            chars[0xB8] = Glyph('\u00b8', GLYPH_0X0B8, data)
            chars[0xB9] = Glyph('\u00b9', GLYPH_0X0B9, data)
            chars[0xBA] = Glyph('\u00ba', GLYPH_0X0BA, data)
            chars[0xBB] = Glyph('\u00bb', GLYPH_0X0BB, data)
            chars[0xBC] = Glyph('\u00bc', GLYPH_0X0BC, data)
            chars[0xBD] = Glyph('\u00bd', GLYPH_0X0BD, data)
            chars[0xBE] = Glyph('\u00be', GLYPH_0X0BE, data)
            chars[0xBF] = Glyph('\u00bf', GLYPH_0X0BF, data)
            chars[0xC0] = Glyph('\u00c0', GLYPH_0X0C0, data)
            chars[0xC1] = Glyph('\u00c1', GLYPH_0X0C1, data)
            chars[0xC2] = Glyph('\u00c2', GLYPH_0X0C2, data)
            chars[0xC3] = Glyph('\u00c3', GLYPH_0X0C3, data)
            chars[0xC4] = Glyph('\u00c4', GLYPH_0X0C4, data)
            chars[0xC5] = Glyph('\u00c5', GLYPH_0X0C5, data)
            chars[0xC6] = Glyph('\u00c6', GLYPH_0X0C6, data)
            chars[0xC7] = Glyph('\u00c7', GLYPH_0X0C7, data)
            chars[0xC8] = Glyph('\u00c8', GLYPH_0X0C8, data)
            chars[0xC9] = Glyph('\u00c9', GLYPH_0X0C9, data)
            chars[0xCA] = Glyph('\u00ca', GLYPH_0X0CA, data)
            chars[0xCB] = Glyph('\u00cb', GLYPH_0X0CB, data)
            chars[0xCC] = Glyph('\u00cc', GLYPH_0X0CC, data)
            chars[0xCD] = Glyph('\u00cd', GLYPH_0X0CD, data)
            chars[0xCE] = Glyph('\u00ce', GLYPH_0X0CE, data)
            chars[0xCF] = Glyph('\u00cf', GLYPH_0X0CF, data)
            chars[0xD0] = Glyph('\u00d0', GLYPH_0X0D0, data)
            chars[0xD1] = Glyph('\u00d1', GLYPH_0X0D1, data)
            chars[0xD2] = Glyph('\u00d2', GLYPH_0X0D2, data)
            chars[0xD3] = Glyph('\u00d3', GLYPH_0X0D3, data)
            chars[0xD4] = Glyph('\u00d4', GLYPH_0X0D4, data)
            chars[0xD5] = Glyph('\u00d5', GLYPH_0X0D5, data)
            chars[0xD6] = Glyph('\u00d6', GLYPH_0X0D6, data)
            chars[0xD7] = Glyph('\u00d7', GLYPH_0X0D7, data)
            chars[0xD8] = Glyph('\u00d8', GLYPH_0X0D8, data)
            chars[0xD9] = Glyph('\u00d9', GLYPH_0X0D9, data)
            chars[0xDA] = Glyph('\u00da', GLYPH_0X0DA, data)
            chars[0xDB] = Glyph('\u00db', GLYPH_0X0DB, data)
            chars[0xDC] = Glyph('\u00dc', GLYPH_0X0DC, data)
            chars[0xDD] = Glyph('\u00dd', GLYPH_0X0DD, data)
            chars[0xDE] = Glyph('\u00de', GLYPH_0X0DE, data)
            chars[0xDF] = Glyph('\u00df', GLYPH_0X0DF, data)
            chars[0xE0] = Glyph('\u00e0', GLYPH_0X0E0, data)
            chars[0xE1] = Glyph('\u00e1', GLYPH_0X0E1, data)
            chars[0xE2] = Glyph('\u00e2', GLYPH_0X0E2, data)
            chars[0xE3] = Glyph('\u00e3', GLYPH_0X0E3, data)
            chars[0xE4] = Glyph('\u00e4', GLYPH_0X0E4, data)
            chars[0xE5] = Glyph('\u00e5', GLYPH_0X0E5, data)
            chars[0xE6] = Glyph('\u00e6', GLYPH_0X0E6, data)
            chars[0xE7] = Glyph('\u00e7', GLYPH_0X0E7, data)
            chars[0xE8] = Glyph('\u00e8', GLYPH_0X0E8, data)
            chars[0xE9] = Glyph('\u00e9', GLYPH_0X0E9, data)
            chars[0xEA] = Glyph('\u00ea', GLYPH_0X0EA, data)
            chars[0xEB] = Glyph('\u00eb', GLYPH_0X0EB, data)
            chars[0xEC] = Glyph('\u00ec', GLYPH_0X0EC, data)
            chars[0xED] = Glyph('\u00ed', GLYPH_0X0ED, data)
            chars[0xEE] = Glyph('\u00ee', GLYPH_0X0EE, data)
            chars[0xEF] = Glyph('\u00ef', GLYPH_0X0EF, data)
            chars[0xF0] = Glyph('\u00f0', GLYPH_0X0F0, data)
            chars[0xF1] = Glyph('\u00f1', GLYPH_0X0F1, data)
            chars[0xF2] = Glyph('\u00f2', GLYPH_0X0F2, data)
            chars[0xF3] = Glyph('\u00f3', GLYPH_0X0F3, data)
            chars[0xF4] = Glyph('\u00f4', GLYPH_0X0F4, data)
            chars[0xF5] = Glyph('\u00f5', GLYPH_0X0F5, data)
            chars[0xF6] = Glyph('\u00f6', GLYPH_0X0F6, data)
            chars[0xF7] = Glyph('\u00f7', GLYPH_0X0F7, data)
            chars[0xF8] = Glyph('\u00f8', GLYPH_0X0F8, data)
            chars[0xF9] = Glyph('\u00f9', GLYPH_0X0F9, data)
            chars[0xFA] = Glyph('\u00fa', GLYPH_0X0FA, data)
            chars[0xFB] = Glyph('\u00fb', GLYPH_0X0FB, data)
            chars[0xFC] = Glyph('\u00fc', GLYPH_0X0FC, data)
            chars[0xFD] = Glyph('\u00fd', GLYPH_0X0FD, data)
            chars[0xFE] = Glyph('\u00fe', GLYPH_0X0FE, data)
            chars[0xFF] = Glyph('\u00ff', GLYPH_0X0FF, data)

            // Some aliases for glyphs not existing
            chars[0x2C6] = Glyph('^', GLYPH_0X05E, data)     // ˆ
        }
    }

    open class DECSupplementalCharacterSet(data: ByteArray) : ASCIICharacterSet(data) {
        init {
            chars[0x80] = Glyph('\u0080', GLYPH_0X080, data)
            chars[0x81] = Glyph('\u0081', GLYPH_0X081, data)
            chars[0x82] = Glyph('\u0082', GLYPH_0X082, data)
            chars[0x83] = Glyph('\u0083', GLYPH_0X083, data)
            chars[0x84] = Glyph('\u0084', GLYPH_0X084, data)
            chars[0x85] = Glyph('\u0085', GLYPH_0X085, data)
            chars[0x86] = Glyph('\u0086', GLYPH_0X086, data)
            chars[0x87] = Glyph('\u0087', GLYPH_0X087, data)
            chars[0x88] = Glyph('\u0088', GLYPH_0X088, data)
            chars[0x89] = Glyph('\u0089', GLYPH_0X089, data)
            chars[0x8A] = Glyph('\u008a', GLYPH_0X08A, data)
            chars[0x8B] = Glyph('\u008b', GLYPH_0X08B, data)
            chars[0x8C] = Glyph('\u008c', GLYPH_0X08C, data)
            chars[0x8D] = Glyph('\u008d', GLYPH_0X08D, data)
            chars[0x8E] = Glyph('\u008e', GLYPH_0X08E, data)
            chars[0x8F] = Glyph('\u008f', GLYPH_0X08F, data)
            chars[0x90] = Glyph('\u0090', GLYPH_0X090, data)
            chars[0x91] = Glyph('\u0091', GLYPH_0X091, data)
            chars[0x92] = Glyph('\u0092', GLYPH_0X092, data)
            chars[0x93] = Glyph('\u0093', GLYPH_0X093, data)
            chars[0x94] = Glyph('\u0094', GLYPH_0X094, data)
            chars[0x95] = Glyph('\u0095', GLYPH_0X095, data)
            chars[0x96] = Glyph('\u0096', GLYPH_0X096, data)
            chars[0x97] = Glyph('\u0097', GLYPH_0X097, data)
            chars[0x98] = Glyph('\u0098', GLYPH_0X098, data)
            chars[0x99] = Glyph('\u0099', GLYPH_0X099, data)
            chars[0x9A] = Glyph('\u009a', GLYPH_0X09A, data)
            chars[0x9B] = Glyph('\u009b', GLYPH_0X09B, data)
            chars[0x9C] = Glyph('\u009c', GLYPH_0X09C, data)
            chars[0x9D] = Glyph('\u009d', GLYPH_0X09D, data)
            chars[0x9E] = Glyph('\u009e', GLYPH_0X09E, data)
            chars[0x9F] = Glyph('\u009f', GLYPH_0X09F, data)
            chars[0xA0] = Glyph(' ', GLYPH_0X0A0, data)
            chars[0xA1] = Glyph('¡', GLYPH_0X0A1, data)
            chars[0xA2] = Glyph('¢', GLYPH_0X0A2, data)
            chars[0xA3] = Glyph('£', GLYPH_0X0A3, data)
            chars[0xA4] = Glyph('¤', GLYPH_0X0A4, data)
            chars[0xA7] = Glyph('§', GLYPH_0X0A7, data)
            chars[0xA8] = Glyph('¨', GLYPH_0X0A8, data)
            chars[0xA9] = Glyph('©', GLYPH_0X0A9, data)
            chars[0xAA] = Glyph('ª', GLYPH_0X0AA, data)
            chars[0xAB] = Glyph('«', GLYPH_0X0AB, data)
            chars[0xB0] = Glyph('°', GLYPH_0X0B0, data)
            chars[0xB1] = Glyph('±', GLYPH_0X0B1, data)
            chars[0xB2] = Glyph('²', GLYPH_0X0B2, data)
            chars[0xB3] = Glyph('³', GLYPH_0X0B3, data)
            chars[0xB5] = Glyph('µ', GLYPH_0X0B5, data)
            chars[0xB6] = Glyph('¶', GLYPH_0X0B6, data)
            chars[0xB7] = Glyph('·', GLYPH_0X0B7, data)
            chars[0xB9] = Glyph('¹', GLYPH_0X0B9, data)
            chars[0xBA] = Glyph('º', GLYPH_0X0BA, data)
            chars[0xBB] = Glyph('»', GLYPH_0X0BB, data)
            chars[0xBC] = Glyph('¼', GLYPH_0X0BC, data)
            chars[0xBD] = Glyph('½', GLYPH_0X0BD, data)
            chars[0xBF] = Glyph('¿', GLYPH_0X0BF, data)
            chars[0xC0] = Glyph('À', GLYPH_0X0C0, data)
            chars[0xC1] = Glyph('Á', GLYPH_0X0C1, data)
            chars[0xC2] = Glyph('Â', GLYPH_0X0C2, data)
            chars[0xC3] = Glyph('Ã', GLYPH_0X0C3, data)
            chars[0xC4] = Glyph('Ä', GLYPH_0X0C4, data)
            chars[0xC5] = Glyph('Å', GLYPH_0X0C5, data)
            chars[0xC6] = Glyph('Æ', GLYPH_0X0C6, data)
            chars[0xC7] = Glyph('Ç', GLYPH_0X0C7, data)
            chars[0xC8] = Glyph('È', GLYPH_0X0C8, data)
            chars[0xC9] = Glyph('É', GLYPH_0X0C9, data)
            chars[0xCA] = Glyph('Ê', GLYPH_0X0CA, data)
            chars[0xCB] = Glyph('Ë', GLYPH_0X0CB, data)
            chars[0xCC] = Glyph('Ì', GLYPH_0X0CC, data)
            chars[0xCD] = Glyph('Í', GLYPH_0X0CD, data)
            chars[0xCE] = Glyph('Î', GLYPH_0X0CE, data)
            chars[0xCF] = Glyph('Ï', GLYPH_0X0CF, data)
            chars[0xD0] = Glyph('Ð', GLYPH_0X0D0, data)
            chars[0xD1] = Glyph('Ñ', GLYPH_0X0D1, data)
            chars[0xD2] = Glyph('Ò', GLYPH_0X0D2, data)
            chars[0xD3] = Glyph('Ó', GLYPH_0X0D3, data)
            chars[0xD4] = Glyph('Ô', GLYPH_0X0D4, data)
            chars[0xD5] = Glyph('Õ', GLYPH_0X0D5, data)
            chars[0xD6] = Glyph('Ö', GLYPH_0X0D6, data)
            chars[0xD7] = Glyph('×', GLYPH_0X0D7, data)
            chars[0xD8] = Glyph('Ø', GLYPH_0X0D8, data)
            chars[0xD9] = Glyph('Ù', GLYPH_0X0D9, data)
            chars[0xDA] = Glyph('Ú', GLYPH_0X0DA, data)
            chars[0xDB] = Glyph('Û', GLYPH_0X0DB, data)
            chars[0xDC] = Glyph('Ü', GLYPH_0X0DC, data)
            chars[0xDD] = Glyph('Ÿ', GLYPH_0X0DD, data)
            chars[0xDF] = Glyph('ß', GLYPH_0X0DF, data)
            chars[0xE0] = Glyph('à', GLYPH_0X0E0, data)
            chars[0xE1] = Glyph('á', GLYPH_0X0E1, data)
            chars[0xE2] = Glyph('â', GLYPH_0X0E2, data)
            chars[0xE3] = Glyph('ã', GLYPH_0X0E3, data)
            chars[0xE4] = Glyph('ä', GLYPH_0X0E4, data)
            chars[0xE5] = Glyph('å', GLYPH_0X0E5, data)
            chars[0xE6] = Glyph('æ', GLYPH_0X0E6, data)
            chars[0xE7] = Glyph('ç', GLYPH_0X0E7, data)
            chars[0xE8] = Glyph('è', GLYPH_0X0E8, data)
            chars[0xE9] = Glyph('é', GLYPH_0X0E9, data)
            chars[0xEA] = Glyph('ê', GLYPH_0X0EA, data)
            chars[0xEB] = Glyph('ë', GLYPH_0X0EB, data)
            chars[0xEC] = Glyph('ì', GLYPH_0X0EC, data)
            chars[0xED] = Glyph('í', GLYPH_0X0ED, data)
            chars[0xEE] = Glyph('î', GLYPH_0X0EE, data)
            chars[0xEF] = Glyph('ï', GLYPH_0X0EF, data)
            chars[0xF1] = Glyph('ñ', GLYPH_0X0F1, data)
            chars[0xF2] = Glyph('ò', GLYPH_0X0F2, data)
            chars[0xF3] = Glyph('ó', GLYPH_0X0F3, data)
            chars[0xF4] = Glyph('ô', GLYPH_0X0F4, data)
            chars[0xF5] = Glyph('õ', GLYPH_0X0F5, data)
            chars[0xF6] = Glyph('ö', GLYPH_0X0F6, data)
            chars[0xF7] = Glyph('÷', GLYPH_0X0F7, data)
            chars[0xF8] = Glyph('ø', GLYPH_0X0F8, data)
            chars[0xF9] = Glyph('ù', GLYPH_0X0F9, data)
            chars[0xFA] = Glyph('ú', GLYPH_0X0FA, data)
            chars[0xFB] = Glyph('û', GLYPH_0X0FB, data)
            chars[0xFC] = Glyph('ü', GLYPH_0X0FC, data)
            chars[0xFD] = Glyph('ÿ', GLYPH_0X0FD, data)
            chars[0xFF] = Glyph(' ', GLYPH_0X0FF, data)
            chars['Œ'.code] = GlyphGraphics('Œ', GLYPH_0X121, data)
            chars['Ÿ'.code] = GlyphGraphics('Ÿ', GLYPH_0X122, data)
            chars['œ'.code] = GlyphGraphics('œ', GLYPH_0X123, data)
        }
    }

    open class ISOLatinAlphabetNr1SupplementCharacterSet(data: ByteArray) :
        DECSupplementalCharacterSet(data) {
        init {
            chars[0xA5] = Glyph('¥', GLYPH_0X0A5, data)
            chars[0xA6] = Glyph('¦', GLYPH_0X0A6, data)
            chars[0xAC] = Glyph('¬', GLYPH_0X0AC, data)
            chars[0xAD] = Glyph('\u00AD', GLYPH_0X0AD, data) // SHY
            chars['®'.code] = Glyph('®', GLYPH_0X0AE, data)
            chars[0xAF] = Glyph('¯', GLYPH_0X0AF, data)
            chars[0xB4] = Glyph('´', GLYPH_0X0B4, data)
            chars[0xB7] = Glyph('·', GLYPH_0X0B7, data)
            chars[0xB8] = Glyph('¸', GLYPH_0X0B8, data)
            chars[0xBE] = Glyph('¾', GLYPH_0X0BE, data)
            chars[0xBF] = Glyph('¿', GLYPH_0X0BF, data)
            chars[0xD0] = Glyph('Ð', GLYPH_0X0D0, data)
            chars[0xDD] = Glyph('Ý', GLYPH_0X0DD, data)
            chars[0xDE] = Glyph('Þ', GLYPH_0X0DE, data)
            chars[0xF0] = Glyph('ð', GLYPH_0X0F0, data)
            chars[0xFD] = Glyph('ý', GLYPH_0X0FD, data)
            chars[0xFE] = Glyph('þ', GLYPH_0X0FE, data)
            chars[0xFF] = Glyph('ÿ', GLYPH_0X0FF, data)
        }
    }

    class SpecialGraphics(data: ByteArray) : ASCIICharacterSet(data) {
        init {
            chars[0x00] = GlyphGraphics('\u0000', GLYPH_0X100, data)
            chars[0x60] = GlyphGraphics('◆', GLYPH_0X101, data)
            chars[0x61] = GlyphGraphics('▒', GLYPH_0X102, data)
            chars[0x62] = GlyphGraphics('␉', GLYPH_0X103, data)
            chars[0x63] = GlyphGraphics('␌', GLYPH_0X104, data)
            chars[0x64] = GlyphGraphics('␍', GLYPH_0X105, data)
            chars[0x65] = GlyphGraphics('␊', GLYPH_0X106, data)
            chars[0x66] = GlyphGraphics('°', GLYPH_0X107, data)
            chars[0x67] = GlyphGraphics('±', GLYPH_0X108, data)
            chars[0x68] = GlyphGraphics('␤', GLYPH_0X109, data)
            chars[0x69] = GlyphGraphics('␋', GLYPH_0X10A, data)
            chars[0x6a] = GlyphGraphics('┘', GLYPH_0X10B, data)
            chars[0x6b] = GlyphGraphics('┐', GLYPH_0X10C, data)
            chars[0x6c] = GlyphGraphics('┌', GLYPH_0X10D, data)
            chars[0x6d] = GlyphGraphics('└', GLYPH_0X10E, data)
            chars[0x6e] = GlyphGraphics('┼', GLYPH_0X10F, data)
            chars[0x6f] = GlyphGraphics('⎺', GLYPH_0X110, data)
            chars[0x70] = GlyphGraphics('⎻', GLYPH_0X111, data)
            chars[0x71] = GlyphGraphics('─', GLYPH_0X112, data)
            chars[0x72] = GlyphGraphics('⎼', GLYPH_0X113, data)
            chars[0x73] = GlyphGraphics('⎽', GLYPH_0X114, data)
            chars[0x74] = GlyphGraphics('├', GLYPH_0X115, data)
            chars[0x75] = GlyphGraphics('┤', GLYPH_0X116, data)
            chars[0x76] = GlyphGraphics('┴', GLYPH_0X117, data)
            chars[0x77] = GlyphGraphics('┬', GLYPH_0X118, data)
            chars[0x78] = GlyphGraphics('│', GLYPH_0X119, data)
            chars[0x79] = GlyphGraphics('≤', GLYPH_0X11A, data)
            chars[0x7a] = GlyphGraphics('≥', GLYPH_0X11B, data)
            chars[0x7b] = GlyphGraphics('π', GLYPH_0X11C, data)
            chars[0x7c] = GlyphGraphics('≠', GLYPH_0X11D, data)
            chars[0x7d] = GlyphGraphics('£', GLYPH_0X11E, data)
            chars[0x7e] = GlyphGraphics('·', GLYPH_0X11F, data)
            chars[0x7f] = GlyphGraphics(' ', GLYPH_0X120, data)

            // Double-index by Unicode character to allow direct usage in boot screens or from code
            val specialChars = chars.values.toList()
            specialChars.forEach { glyph ->
                if (glyph.char.code > 127) {
                    chars[glyph.char.code] = glyph
                }
            }
        }
    }

    open class VT52CharacterSet(data: ByteArray) : BaseCharacterSet() {
        init {
            chars[0x20] = GlyphVT52(' ', GLYPHVT52_20, data)
            chars[0x21] = GlyphVT52('!', GLYPHVT52_21, data)
            chars[0x22] = GlyphVT52('"', GLYPHVT52_22, data)
            chars[0x23] = GlyphVT52('#', GLYPHVT52_23, data)
            chars[0x24] = GlyphVT52('$', GLYPHVT52_24, data)
            chars[0x25] = GlyphVT52('%', GLYPHVT52_25, data)
            chars[0x26] = GlyphVT52('&', GLYPHVT52_26, data)
            chars[0x27] = GlyphVT52('\'', GLYPHVT52_27, data)
            chars[0x28] = GlyphVT52('(', GLYPHVT52_28, data)
            chars[0x29] = GlyphVT52(')', GLYPHVT52_29, data)
            chars[0x2A] = GlyphVT52('*', GLYPHVT52_2A, data)
            chars[0x2B] = GlyphVT52('+', GLYPHVT52_2B, data)
            chars[0x2C] = GlyphVT52(',', GLYPHVT52_2C, data)
            chars[0x2D] = GlyphVT52('-', GLYPHVT52_2D, data)
            chars[0x2E] = GlyphVT52('.', GLYPHVT52_2E, data)
            chars[0x2F] = GlyphVT52('/', GLYPHVT52_2F, data)
            chars[0x30] = GlyphVT52('0', GLYPHVT52_30, data)
            chars[0x31] = GlyphVT52('1', GLYPHVT52_31, data)
            chars[0x32] = GlyphVT52('2', GLYPHVT52_32, data)
            chars[0x33] = GlyphVT52('3', GLYPHVT52_33, data)
            chars[0x34] = GlyphVT52('4', GLYPHVT52_34, data)
            chars[0x35] = GlyphVT52('5', GLYPHVT52_35, data)
            chars[0x36] = GlyphVT52('6', GLYPHVT52_36, data)
            chars[0x37] = GlyphVT52('7', GLYPHVT52_37, data)
            chars[0x38] = GlyphVT52('8', GLYPHVT52_38, data)
            chars[0x39] = GlyphVT52('9', GLYPHVT52_39, data)
            chars[0x3A] = GlyphVT52(':', GLYPHVT52_3A, data)
            chars[0x3B] = GlyphVT52(';', GLYPHVT52_3B, data)
            chars[0x3C] = GlyphVT52('<', GLYPHVT52_3C, data)
            chars[0x3D] = GlyphVT52('=', GLYPHVT52_3D, data)
            chars[0x3E] = GlyphVT52('>', GLYPHVT52_3E, data)
            chars[0x3F] = GlyphVT52('?', GLYPHVT52_3F, data)
            chars[0x40] = GlyphVT52('@', GLYPHVT52_40, data)
            chars[0x41] = GlyphVT52('A', GLYPHVT52_41, data)
            chars[0x42] = GlyphVT52('B', GLYPHVT52_42, data)
            chars[0x43] = GlyphVT52('C', GLYPHVT52_43, data)
            chars[0x44] = GlyphVT52('D', GLYPHVT52_44, data)
            chars[0x45] = GlyphVT52('E', GLYPHVT52_45, data)
            chars[0x46] = GlyphVT52('F', GLYPHVT52_46, data)
            chars[0x47] = GlyphVT52('G', GLYPHVT52_47, data)
            chars[0x48] = GlyphVT52('H', GLYPHVT52_48, data)
            chars[0x49] = GlyphVT52('I', GLYPHVT52_49, data)
            chars[0x4A] = GlyphVT52('J', GLYPHVT52_4A, data)
            chars[0x4B] = GlyphVT52('K', GLYPHVT52_4B, data)
            chars[0x4C] = GlyphVT52('L', GLYPHVT52_4C, data)
            chars[0x4D] = GlyphVT52('M', GLYPHVT52_4D, data)
            chars[0x4E] = GlyphVT52('N', GLYPHVT52_4E, data)
            chars[0x4F] = GlyphVT52('O', GLYPHVT52_4F, data)
            chars[0x50] = GlyphVT52('P', GLYPHVT52_50, data)
            chars[0x51] = GlyphVT52('Q', GLYPHVT52_51, data)
            chars[0x52] = GlyphVT52('R', GLYPHVT52_52, data)
            chars[0x53] = GlyphVT52('S', GLYPHVT52_53, data)
            chars[0x54] = GlyphVT52('T', GLYPHVT52_54, data)
            chars[0x55] = GlyphVT52('U', GLYPHVT52_55, data)
            chars[0x56] = GlyphVT52('V', GLYPHVT52_56, data)
            chars[0x57] = GlyphVT52('W', GLYPHVT52_57, data)
            chars[0x58] = GlyphVT52('X', GLYPHVT52_58, data)
            chars[0x59] = GlyphVT52('Y', GLYPHVT52_59, data)
            chars[0x5A] = GlyphVT52('Z', GLYPHVT52_5A, data)
            chars[0x5B] = GlyphVT52('[', GLYPHVT52_5B, data)
            chars[0x5C] = GlyphVT52('\\', GLYPHVT52_5C, data)
            chars[0x5D] = GlyphVT52(']', GLYPHVT52_5D, data)
            chars[0x5E] = GlyphVT52('^', GLYPHVT52_5E, data)
            chars[0x5F] = GlyphVT52('_', GLYPHVT52_5F, data)
            chars[0x60] = GlyphVT52('`', GLYPHVT52_60, data)
            chars[0x61] = GlyphVT52('a', GLYPHVT52_61, data)
            chars[0x62] = GlyphVT52('b', GLYPHVT52_62, data)
            chars[0x63] = GlyphVT52('c', GLYPHVT52_63, data)
            chars[0x64] = GlyphVT52('d', GLYPHVT52_64, data)
            chars[0x65] = GlyphVT52('e', GLYPHVT52_65, data)
            chars[0x66] = GlyphVT52('f', GLYPHVT52_66, data)
            chars[0x67] = GlyphVT52('g', GLYPHVT52_67, data)
            chars[0x68] = GlyphVT52('h', GLYPHVT52_68, data)
            chars[0x69] = GlyphVT52('i', GLYPHVT52_69, data)
            chars[0x6A] = GlyphVT52('j', GLYPHVT52_6A, data)
            chars[0x6B] = GlyphVT52('k', GLYPHVT52_6B, data)
            chars[0x6C] = GlyphVT52('l', GLYPHVT52_6C, data)
            chars[0x6D] = GlyphVT52('m', GLYPHVT52_6D, data)
            chars[0x6E] = GlyphVT52('n', GLYPHVT52_6E, data)
            chars[0x6F] = GlyphVT52('o', GLYPHVT52_6F, data)
            chars[0x70] = GlyphVT52('p', GLYPHVT52_70, data)
            chars[0x71] = GlyphVT52('q', GLYPHVT52_71, data)
            chars[0x72] = GlyphVT52('r', GLYPHVT52_72, data)
            chars[0x73] = GlyphVT52('s', GLYPHVT52_73, data)
            chars[0x74] = GlyphVT52('t', GLYPHVT52_74, data)
            chars[0x75] = GlyphVT52('u', GLYPHVT52_75, data)
            chars[0x76] = GlyphVT52('v', GLYPHVT52_76, data)
            chars[0x77] = GlyphVT52('w', GLYPHVT52_77, data)
            chars[0x78] = GlyphVT52('x', GLYPHVT52_78, data)
            chars[0x79] = GlyphVT52('y', GLYPHVT52_79, data)
            chars[0x7A] = GlyphVT52('z', GLYPHVT52_7A, data)
            chars[0x7B] = GlyphVT52('{', GLYPHVT52_7B, data)
            chars[0x7C] = GlyphVT52('|', GLYPHVT52_7C, data)
            chars[0x7D] = GlyphVT52('}', GLYPHVT52_7D, data)
            chars[0x7E] = GlyphVT52('~', GLYPHVT52_7E, data)
            chars[0x7F] = GlyphVT52('~', GLYPHVT52_7F, data)
        }
    }

    class VT52SpecialGraphics(data: ByteArray) : VT52CharacterSet(data) {
        init {
            chars[0x5F] = GlyphVT52(' ', GLYPHVT52_00, data)
            chars[0x60] = GlyphVT52(' ', GLYPHVT52_01, data)
            chars[0x61] = GlyphVT52('▮', GLYPHVT52_02, data)
            chars[0x62] = GlyphVT52('⅟', GLYPHVT52_03, data)
            chars[0x63] = GlyphVT52(' ', GLYPHVT52_04, data)
            chars[0x64] = GlyphVT52(' ', GLYPHVT52_05, data)
            chars[0x65] = GlyphVT52(' ', GLYPHVT52_06, data)
            chars[0x66] = GlyphVT52('°', GLYPHVT52_07, data)
            chars[0x67] = GlyphVT52('±', GLYPHVT52_08, data)
            chars[0x68] = GlyphVT52('→', GLYPHVT52_09, data)
            chars[0x69] = GlyphVT52('…', GLYPHVT52_0A, data)
            chars[0x6a] = GlyphVT52('÷', GLYPHVT52_0B, data)
            chars[0x6b] = GlyphVT52('↓', GLYPHVT52_0C, data)
            chars[0x6c] = GlyphVT52('⎺', GLYPHVT52_0D, data)
            chars[0x6d] = GlyphVT52('⎺', GLYPHVT52_0E, data)
            chars[0x6e] = GlyphVT52('⎻', GLYPHVT52_0F, data)
            chars[0x6f] = GlyphVT52('⎻', GLYPHVT52_10, data)
            chars[0x70] = GlyphVT52('⎼', GLYPHVT52_11, data)
            chars[0x71] = GlyphVT52('⎼', GLYPHVT52_12, data)
            chars[0x72] = GlyphVT52('⎽', GLYPHVT52_13, data)
            chars[0x73] = GlyphVT52('⎽', GLYPHVT52_14, data)
            chars[0x74] = GlyphVT52('₀', GLYPHVT52_15, data)
            chars[0x75] = GlyphVT52('₁', GLYPHVT52_16, data)
            chars[0x76] = GlyphVT52('₂', GLYPHVT52_17, data)
            chars[0x77] = GlyphVT52('₃', GLYPHVT52_18, data)
            chars[0x78] = GlyphVT52('₄', GLYPHVT52_19, data)
            chars[0x79] = GlyphVT52('₅', GLYPHVT52_1A, data)
            chars[0x7a] = GlyphVT52('₆', GLYPHVT52_1B, data)
            chars[0x7b] = GlyphVT52('₇', GLYPHVT52_1C, data)
            chars[0x7c] = GlyphVT52('₈', GLYPHVT52_1D, data)
            chars[0x7d] = GlyphVT52('₉', GLYPHVT52_1E, data)
            chars[0x7e] = GlyphVT52('¶', GLYPHVT52_1F, data)
        }
    }

    open class NationalCharacterSet(data: ByteArray) : ASCIICharacterSet(data)

    class UnitedKingdom(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x23] = Glyph('£', GLYPH_0X0A3, data)
        }
    }

    class DenmarkNorway(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x40] = Glyph('Ä', GLYPH_0X0C4, data)
            chars[0x5B] = Glyph('Æ', GLYPH_0X0C6, data)
            chars[0x5C] = Glyph('Ø', GLYPH_0X0D8, data)
            chars[0x5D] = Glyph('Å', GLYPH_0X0C5, data)
            chars[0x5E] = Glyph('Ü', GLYPH_0X0DC, data)
            chars[0x60] = Glyph('ä', GLYPH_0X0E4, data)
            chars[0x7B] = Glyph('æ', GLYPH_0X0E6, data)
            chars[0x7C] = Glyph('ø', GLYPH_0X0F8, data)
            chars[0x7D] = Glyph('å', GLYPH_0X0E5, data)
            chars[0x7E] = Glyph('ü', GLYPH_0X0FC, data)
        }
    }

    class Dutch(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x23] = Glyph('£', GLYPH_0X0A3, data)
            chars[0x40] = Glyph('¾', GLYPH_0X0BE, data)
            chars[0x5B] = Glyph('ÿ', GLYPH_0X0FD, data)
            chars[0x5C] = Glyph('½', GLYPH_0X0BD, data)
            chars[0x5D] = Glyph('|', GLYPH_0X07C, data)
            chars[0x7B] = Glyph('¨', GLYPH_0X0A8, data)
            chars[0x7C] = Glyph('f', GLYPH_0X066, data)
            chars[0x7D] = Glyph('¼', GLYPH_0X0BC, data)
            chars[0x7E] = Glyph('´', GLYPH_0X0B4, data)
        }
    }

    class Finnish(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x5B] = Glyph('Ä', GLYPH_0X0C4, data)
            chars[0x5C] = Glyph('Ö', GLYPH_0X0D6, data)
            chars[0x5D] = Glyph('Å', GLYPH_0X0C5, data)
            chars[0x5E] = Glyph('Ü', GLYPH_0X0DC, data)
            chars[0x60] = Glyph('é', GLYPH_0X0E9, data)
            chars[0x7B] = Glyph('ä', GLYPH_0X0E4, data)
            chars[0x7C] = Glyph('ö', GLYPH_0X0F6, data)
            chars[0x7D] = Glyph('å', GLYPH_0X0E5, data)
            chars[0x7E] = Glyph('ü', GLYPH_0X0FC, data)
        }
    }

    class French(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x23] = Glyph('£', GLYPH_0X0A3, data)
            chars[0x40] = Glyph('à', GLYPH_0X0E0, data)
            chars[0x5B] = Glyph('°', GLYPH_0X0B0, data)
            chars[0x5C] = Glyph('ç', GLYPH_0X0E7, data)
            chars[0x5D] = Glyph('§', GLYPH_0X0A7, data)
            chars[0x7B] = Glyph('é', GLYPH_0X0E9, data)
            chars[0x7C] = Glyph('ù', GLYPH_0X0F9, data)
            chars[0x7D] = Glyph('è', GLYPH_0X0E8, data)
            chars[0x7E] = Glyph('¨', GLYPH_0X0A8, data)
        }
    }

    class FrenchCanadian(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x40] = Glyph('à', GLYPH_0X0E0, data)
            chars[0x5B] = Glyph('â', GLYPH_0X0E2, data)
            chars[0x5C] = Glyph('ç', GLYPH_0X0E7, data)
            chars[0x5D] = Glyph('ê', GLYPH_0X0EA, data)
            chars[0x5E] = Glyph('î', GLYPH_0X0EE, data)
            chars[0x60] = Glyph('ô', GLYPH_0X0F4, data)
            chars[0x7B] = Glyph('é', GLYPH_0X0E9, data)
            chars[0x7C] = Glyph('ù', GLYPH_0X0F9, data)
            chars[0x7D] = Glyph('è', GLYPH_0X0E8, data)
            chars[0x7E] = Glyph('û', GLYPH_0X0FB, data)
        }
    }

    class German(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x40] = Glyph('§', GLYPH_0X0A7, data)
            chars[0x5B] = Glyph('Ä', GLYPH_0X0C4, data)
            chars[0x5C] = Glyph('Ö', GLYPH_0X0D6, data)
            chars[0x5D] = Glyph('Ü', GLYPH_0X0DC, data)
            chars[0x7B] = Glyph('ä', GLYPH_0X0E4, data)
            chars[0x7C] = Glyph('ö', GLYPH_0X0F6, data)
            chars[0x7D] = Glyph('ü', GLYPH_0X0FC, data)
            chars[0x7E] = Glyph('ß', GLYPH_0X0DF, data)
        }
    }

    class Italian(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x23] = Glyph('£', GLYPH_0X0A3, data)
            chars[0x40] = Glyph('§', GLYPH_0X0A7, data)
            chars[0x5B] = Glyph('°', GLYPH_0X0B0, data)
            chars[0x5C] = Glyph('ç', GLYPH_0X0E7, data)
            chars[0x5D] = Glyph('é', GLYPH_0X0E9, data)
            chars[0x60] = Glyph('ù', GLYPH_0X0F9, data)
            chars[0x7B] = Glyph('à', GLYPH_0X0E0, data)
            chars[0x7C] = Glyph('ò', GLYPH_0X0F2, data)
            chars[0x7D] = Glyph('è', GLYPH_0X0E8, data)
            chars[0x7E] = Glyph('ì', GLYPH_0X0EC, data)
        }
    }

    class Portuguese(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x5B] = Glyph('Ã', GLYPH_0X0C3, data)
            chars[0x5C] = Glyph('Ç', GLYPH_0X0C7, data)
            chars[0x5D] = Glyph('Õ', GLYPH_0X0D5, data)
            chars[0x7B] = Glyph('ã', GLYPH_0X0E3, data)
            chars[0x7C] = Glyph('ç', GLYPH_0X0E7, data)
            chars[0x7D] = Glyph('õ', GLYPH_0X0F5, data)
        }
    }

    class Spanish(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x23] = Glyph('£', GLYPH_0X0A3, data)
            chars[0x40] = Glyph('§', GLYPH_0X0A7, data)
            chars[0x5B] = Glyph('¡', GLYPH_0X0A1, data)
            chars[0x5C] = Glyph('Ñ', GLYPH_0X0D1, data)
            chars[0x5D] = Glyph('¿', GLYPH_0X0BF, data)
            chars[0x7B] = Glyph('°', GLYPH_0X0B0, data)
            chars[0x7C] = Glyph('ñ', GLYPH_0X0F1, data)
            chars[0x7D] = Glyph('ç', GLYPH_0X0E7, data)
        }
    }

    class Swedish(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x40] = Glyph('É', GLYPH_0X0C9, data)
            chars[0x5B] = Glyph('Ä', GLYPH_0X0C4, data)
            chars[0x5C] = Glyph('Ö', GLYPH_0X0D6, data)
            chars[0x5D] = Glyph('Å', GLYPH_0X0C5, data)
            chars[0x5E] = Glyph('Ü', GLYPH_0X0DC, data)
            chars[0x60] = Glyph('é', GLYPH_0X0E9, data)
            chars[0x7B] = Glyph('ä', GLYPH_0X0E4, data)
            chars[0x7C] = Glyph('ö', GLYPH_0X0F6, data)
            chars[0x7D] = Glyph('å', GLYPH_0X0E5, data)
            chars[0x7E] = Glyph('ü', GLYPH_0X0FC, data)
        }
    }

    class Swiss(data: ByteArray) : NationalCharacterSet(data) {
        init {
            chars[0x23] = Glyph('ù', GLYPH_0X0F9, data)
            chars[0x40] = Glyph('à', GLYPH_0X0E0, data)
            chars[0x5B] = Glyph('é', GLYPH_0X0E9, data)
            chars[0x5C] = Glyph('ç', GLYPH_0X0E7, data)
            chars[0x5D] = Glyph('ê', GLYPH_0X0EA, data)
            chars[0x5E] = Glyph('î', GLYPH_0X0EE, data)
            chars[0x5F] = Glyph('è', GLYPH_0X0E8, data)
            chars[0x60] = Glyph('ô', GLYPH_0X0F4, data)
            chars[0x7B] = Glyph('ä', GLYPH_0X0E4, data)
            chars[0x7C] = Glyph('ö', GLYPH_0X0F6, data)
            chars[0x7D] = Glyph('ü', GLYPH_0X0FC, data)
            chars[0x7E] = Glyph('û', GLYPH_0X0FB, data)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    class GlyphABC80(char: Char, private val characterMap: Abc80MonitorCharacterMap) :
        Glyph(
            char, columnRange80 = 0..7, columnRange132 = 0..7,
            glyphWidth80 = 8, glyphWidth132 = 8, height = 14
        ) {
        override fun parse() {
            if (parsed) return
            characterMap.forEach(char.code) { bits, row ->
                characterMap.forEachBit(bits.toInt()) { on, column ->
                    data80[row][column] = on
                    data132[row][column] = on
                }
            }
            parsed = true
        }
    }

    class ABC80CharacterSet : BaseCharacterSet() {
        init {
            val characterMap = Abc80MonitorCharacterMap()
            for (i in 0..255) {
                chars[i] = GlyphABC80(i.toChar(), characterMap)
            }
        }
    }

// --- Hex Constants ---

    const val GLYPH_0X000 = 0x86bc
    const val GLYPH_0X001 = 0x86bd
    const val GLYPH_0X002 = 0x86d7
    const val GLYPH_0X003 = 0x86f1
    const val GLYPH_0X004 = 0x870b
    const val GLYPH_0X005 = 0x8725
    const val GLYPH_0X006 = 0x8742
    const val GLYPH_0X007 = 0x875c
    const val GLYPH_0X008 = 0x8776
    const val GLYPH_0X009 = 0x8790
    const val GLYPH_0X00A = 0x87aa
    const val GLYPH_0X00B = 0x87c4
    const val GLYPH_0X00C = 0x87de
    const val GLYPH_0X00D = 0x87f8
    const val GLYPH_0X00E = 0x8812
    const val GLYPH_0X00F = 0x882c
    const val GLYPH_0X010 = 0x8846
    const val GLYPH_0X011 = 0x8860
    const val GLYPH_0X012 = 0x887a
    const val GLYPH_0X013 = 0x8894
    const val GLYPH_0X014 = 0x88ae
    const val GLYPH_0X015 = 0x88c8
    const val GLYPH_0X016 = 0x88e2
    const val GLYPH_0X017 = 0x88fc
    const val GLYPH_0X018 = 0x8919
    const val GLYPH_0X019 = 0x8933
    const val GLYPH_0X01A = 0x894d
    const val GLYPH_0X01B = 0x896a
    const val GLYPH_0X01C = 0x8984
    const val GLYPH_0X01D = 0x899e
    const val GLYPH_0X01E = 0x89b8
    const val GLYPH_0X01F = 0x89d2
    const val GLYPH_0X021 = 0x8a09
    const val GLYPH_0X022 = 0x8a1c
    const val GLYPH_0X023 = 0x8a25
    const val GLYPH_0X024 = 0x8a35
    const val GLYPH_0X025 = 0x8a48
    const val GLYPH_0X026 = 0x8a5b
    const val GLYPH_0X027 = 0x8a6e
    const val GLYPH_0X028 = 0x8a77
    const val GLYPH_0X029 = 0x8a8f
    const val GLYPH_0X02A = 0x8aa7
    const val GLYPH_0X02B = 0x8ab5
    const val GLYPH_0X02C = 0x8ac3
    const val GLYPH_0X02D = 0x8ace
    const val GLYPH_0X02E = 0x8ad2
    const val GLYPH_0X02F = 0x8ad8
    const val GLYPH_0X030 = 0x8af0
    const val GLYPH_0X031 = 0x8b03
    const val GLYPH_0X032 = 0x8b16
    const val GLYPH_0X033 = 0x8b29
    const val GLYPH_0X034 = 0x8b3c
    const val GLYPH_0X035 = 0x8b4f
    const val GLYPH_0X036 = 0x8b62
    const val GLYPH_0X037 = 0x8b75
    const val GLYPH_0X038 = 0x8b88
    const val GLYPH_0X039 = 0x8b9b
    const val GLYPH_0X03A = 0x8bae
    const val GLYPH_0X03B = 0x8bbe
    const val GLYPH_0X03C = 0x8bd3
    const val GLYPH_0X03D = 0x8be6
    const val GLYPH_0X03E = 0x8bef
    const val GLYPH_0X03F = 0x8c02
    const val GLYPH_0X040 = 0x8c15
    const val GLYPH_0X041 = 0x8c2a
    const val GLYPH_0X042 = 0x8c3d
    const val GLYPH_0X043 = 0x8c50
    const val GLYPH_0X044 = 0x8c63
    const val GLYPH_0X045 = 0x8c76
    const val GLYPH_0X046 = 0x8c89
    const val GLYPH_0X047 = 0x8c9c
    const val GLYPH_0X048 = 0x8caf
    const val GLYPH_0X049 = 0x8cc2
    const val GLYPH_0X04A = 0x8cd5
    const val GLYPH_0X04B = 0x8ce8
    const val GLYPH_0X04C = 0x8cfb
    const val GLYPH_0X04D = 0x8d0e
    const val GLYPH_0X04E = 0x8d21
    const val GLYPH_0X04F = 0x8d34
    const val GLYPH_0X050 = 0x8d47
    const val GLYPH_0X051 = 0x8d5a
    const val GLYPH_0X052 = 0x8d72
    const val GLYPH_0X053 = 0x8d85
    const val GLYPH_0X054 = 0x8d98
    const val GLYPH_0X055 = 0x8dab
    const val GLYPH_0X056 = 0x8dbe
    const val GLYPH_0X057 = 0x8dd1
    const val GLYPH_0X058 = 0x8de4
    const val GLYPH_0X059 = 0x8df7
    const val GLYPH_0X05A = 0x8e0a
    const val GLYPH_0X05B = 0x8e1d
    const val GLYPH_0X05C = 0x8e35
    const val GLYPH_0X05D = 0x8e4d
    const val GLYPH_0X05E = 0x8e65
    const val GLYPH_0X05F = 0x8e70
    const val GLYPH_0X060 = 0x8e74
    const val GLYPH_0X061 = 0x8e7d
    const val GLYPH_0X062 = 0x8e8b
    const val GLYPH_0X063 = 0x8e9e
    const val GLYPH_0X064 = 0x8eac
    const val GLYPH_0X065 = 0x8ebf
    const val GLYPH_0X066 = 0x8ecd
    const val GLYPH_0X067 = 0x8ee0
    const val GLYPH_0X068 = 0x8ef3
    const val GLYPH_0X069 = 0x8f06
    const val GLYPH_0X06A = 0x8f19
    const val GLYPH_0X06B = 0x8f31
    const val GLYPH_0X06C = 0x8f44
    const val GLYPH_0X06D = 0x8f57
    const val GLYPH_0X06E = 0x8f65
    const val GLYPH_0X06F = 0x8f73
    const val GLYPH_0X070 = 0x8f81
    const val GLYPH_0X071 = 0x8f94
    const val GLYPH_0X072 = 0x8fa7
    const val GLYPH_0X073 = 0x8fb5
    const val GLYPH_0X074 = 0x8fc3
    const val GLYPH_0X075 = 0x8fd6
    const val GLYPH_0X076 = 0x8fe4
    const val GLYPH_0X077 = 0x8ff2
    const val GLYPH_0X078 = 0x9000
    const val GLYPH_0X079 = 0x900e
    const val GLYPH_0X07A = 0x9021
    const val GLYPH_0X07B = 0x902f
    const val GLYPH_0X07C = 0x9047
    const val GLYPH_0X07D = 0x905f
    const val GLYPH_0X07E = 0x9077
    const val GLYPH_0X07F = 0x9082
    const val GLYPH_0X080 = 0x909c
    const val GLYPH_0X081 = 0x90b6
    const val GLYPH_0X082 = 0x90d0
    const val GLYPH_0X083 = 0x90ea
    const val GLYPH_0X084 = 0x9107
    const val GLYPH_0X085 = 0x9121
    const val GLYPH_0X086 = 0x913b
    const val GLYPH_0X087 = 0x9155
    const val GLYPH_0X088 = 0x916f
    const val GLYPH_0X089 = 0x9189
    const val GLYPH_0X08A = 0x91a3
    const val GLYPH_0X08B = 0x91bd
    const val GLYPH_0X08C = 0x91d7
    const val GLYPH_0X08D = 0x91f1
    const val GLYPH_0X08E = 0x920b
    const val GLYPH_0X08F = 0x9225
    const val GLYPH_0X090 = 0x923f
    const val GLYPH_0X091 = 0x9259
    const val GLYPH_0X092 = 0x9273
    const val GLYPH_0X093 = 0x928d
    const val GLYPH_0X094 = 0x92a7
    const val GLYPH_0X095 = 0x92c1
    const val GLYPH_0X096 = 0x92db
    const val GLYPH_0X097 = 0x92f5
    const val GLYPH_0X098 = 0x930f
    const val GLYPH_0X099 = 0x932c
    const val GLYPH_0X09A = 0x9349
    const val GLYPH_0X09B = 0x9363
    const val GLYPH_0X09C = 0x937d
    const val GLYPH_0X09D = 0x9397
    const val GLYPH_0X09E = 0x93b1
    const val GLYPH_0X09F = 0x93cb
    const val GLYPH_0X0A0 = 0x93e5
    const val GLYPH_0X0A1 = 0x93ff
    const val GLYPH_0X0A2 = 0x9412
    const val GLYPH_0X0A3 = 0x942a
    const val GLYPH_0X0A4 = 0x943d
    const val GLYPH_0X0A5 = 0x944d
    const val GLYPH_0X0A6 = 0x9460
    const val GLYPH_0X0A7 = 0x9478
    const val GLYPH_0X0A8 = 0x948d
    const val GLYPH_0X0A9 = 0x9491
    const val GLYPH_0X0AA = 0x94a9
    const val GLYPH_0X0AB = 0x94bc
    const val GLYPH_0X0AC = 0x94ca
    const val GLYPH_0X0AD = 0x94d3
    const val GLYPH_0X0AE = 0x94d7
    const val GLYPH_0X0AF = 0x94ef
    const val GLYPH_0X0B0 = 0x94f3
    const val GLYPH_0X0B1 = 0x94fe
    const val GLYPH_0X0B2 = 0x9511
    const val GLYPH_0X0B3 = 0x951f
    const val GLYPH_0X0B4 = 0x952d
    const val GLYPH_0X0B5 = 0x9533
    const val GLYPH_0X0B6 = 0x9543
    const val GLYPH_0X0B7 = 0x955d
    const val GLYPH_0X0B8 = 0x9561
    const val GLYPH_0X0B9 = 0x9567
    const val GLYPH_0X0BA = 0x9575
    const val GLYPH_0X0BB = 0x9588
    const val GLYPH_0X0BC = 0x9596
    const val GLYPH_0X0BD = 0x95b0
    const val GLYPH_0X0BE = 0x95ca
    const val GLYPH_0X0BF = 0x95e4
    const val GLYPH_0X0C0 = 0x95f7
    const val GLYPH_0X0C1 = 0x9611
    const val GLYPH_0X0C2 = 0x962b
    const val GLYPH_0X0C3 = 0x9645
    const val GLYPH_0X0C4 = 0x965f
    const val GLYPH_0X0C5 = 0x9677
    const val GLYPH_0X0C6 = 0x9691
    const val GLYPH_0X0C7 = 0x96a4
    const val GLYPH_0X0C8 = 0x96bc
    const val GLYPH_0X0C9 = 0x96d6
    const val GLYPH_0X0CA = 0x96f0
    const val GLYPH_0X0CB = 0x970a
    const val GLYPH_0X0CC = 0x9722
    const val GLYPH_0X0CD = 0x973c
    const val GLYPH_0X0CE = 0x9756
    const val GLYPH_0X0CF = 0x9770
    const val GLYPH_0X0D0 = 0x9788
    const val GLYPH_0X0D1 = 0x979b
    const val GLYPH_0X0D2 = 0x97b5
    const val GLYPH_0X0D3 = 0x97cf
    const val GLYPH_0X0D4 = 0x97e9
    const val GLYPH_0X0D5 = 0x9803
    const val GLYPH_0X0D6 = 0x981d
    const val GLYPH_0X0D7 = 0x9835
    const val GLYPH_0X0D8 = 0x9848
    const val GLYPH_0X0D9 = 0x9860
    const val GLYPH_0X0DA = 0x987a
    const val GLYPH_0X0DB = 0x9894
    const val GLYPH_0X0DC = 0x98ae
    const val GLYPH_0X0DD = 0x98c6
    const val GLYPH_0X0DE = 0x98e0
    const val GLYPH_0X0DF = 0x98f3
    const val GLYPH_0X0E0 = 0x9908
    const val GLYPH_0X0E1 = 0x991d
    const val GLYPH_0X0E2 = 0x9932
    const val GLYPH_0X0E3 = 0x9947
    const val GLYPH_0X0E4 = 0x995c
    const val GLYPH_0X0E5 = 0x996f
    const val GLYPH_0X0E6 = 0x9987
    const val GLYPH_0X0E7 = 0x9995
    const val GLYPH_0X0E8 = 0x99a8
    const val GLYPH_0X0E9 = 0x99bd
    const val GLYPH_0X0EA = 0x99d2
    const val GLYPH_0X0EB = 0x99e7
    const val GLYPH_0X0EC = 0x99fa
    const val GLYPH_0X0ED = 0x9a0f
    const val GLYPH_0X0EE = 0x9a24
    const val GLYPH_0X0EF = 0x9a39
    const val GLYPH_0X0F0 = 0x9a4c
    const val GLYPH_0X0F1 = 0x9a64
    const val GLYPH_0X0F2 = 0x9a79
    const val GLYPH_0X0F3 = 0x9a8e
    const val GLYPH_0X0F4 = 0x9aa3
    const val GLYPH_0X0F5 = 0x9ab8
    const val GLYPH_0X0F6 = 0x9acd
    const val GLYPH_0X0F7 = 0x9ae0
    const val GLYPH_0X0F8 = 0x9af3
    const val GLYPH_0X0F9 = 0x9b06
    const val GLYPH_0X0FA = 0x9b1b
    const val GLYPH_0X0FB = 0x9b30
    const val GLYPH_0X0FC = 0x9b45
    const val GLYPH_0X0FD = 0x9b58
    const val GLYPH_0X0FE = 0x9b72
    const val GLYPH_0X0FF = 0x9b8a

    const val GLYPH_0X100 = 0x1c28
    const val GLYPH_0X101 = 0x1c46
    const val GLYPH_0X102 = 0x1c5c
    const val GLYPH_0X103 = 0x1c7b
    const val GLYPH_0X104 = 0x1c9a
    const val GLYPH_0X105 = 0x1cb9
    const val GLYPH_0X106 = 0x1cd8
    const val GLYPH_0X107 = 0x1cf7
    const val GLYPH_0X108 = 0x1d04
    const val GLYPH_0X109 = 0x1d1a
    const val GLYPH_0X10A = 0x1d39
    const val GLYPH_0X10B = 0x1d58
    const val GLYPH_0X10C = 0x1d6b
    const val GLYPH_0X10D = 0x1d81
    const val GLYPH_0X10E = 0x1d97
    const val GLYPH_0X10F = 0x1daa
    const val GLYPH_0X110 = 0x1dcf
    const val GLYPH_0X111 = 0x1dd3
    const val GLYPH_0X112 = 0x1dd7
    const val GLYPH_0X113 = 0x1ddb
    const val GLYPH_0X114 = 0x1ddf
    const val GLYPH_0X115 = 0x1de3
    const val GLYPH_0X116 = 0x1e08
    const val GLYPH_0X117 = 0x1e2d
    const val GLYPH_0X118 = 0x1e40
    const val GLYPH_0X119 = 0x1e56
    const val GLYPH_0X11A = 0x1e7b
    const val GLYPH_0X11B = 0x1e91
    const val GLYPH_0X11C = 0x1ea7
    const val GLYPH_0X11D = 0x1eb7
    const val GLYPH_0X11E = 0x1ecd
    const val GLYPH_0X11F = 0x1ee3
    const val GLYPH_0X120 = 0x1ee7
    const val GLYPH_0X121 = 0x1efd
    const val GLYPH_0X122 = 0x1f19
    const val GLYPH_0X123 = 0x1f29
    const val GLYPH_0X124 = 0x1f4e
    const val GLYPH_0X125 = 0x1f73
    const val GLYPH_0X126 = 0x1f95
    const val GLYPHVT52_00 = 0x0000
    const val GLYPHVT52_01 = 0x0020
    const val GLYPHVT52_02 = 0x0040
    const val GLYPHVT52_03 = 0x0060
    const val GLYPHVT52_04 = 0x0080
    const val GLYPHVT52_05 = 0x00a0
    const val GLYPHVT52_06 = 0x00c0
    const val GLYPHVT52_07 = 0x00e0
    const val GLYPHVT52_08 = 0x0100
    const val GLYPHVT52_09 = 0x0120
    const val GLYPHVT52_0A = 0x0140
    const val GLYPHVT52_0B = 0x0160
    const val GLYPHVT52_0C = 0x0180
    const val GLYPHVT52_0D = 0x01a0
    const val GLYPHVT52_0E = 0x01c0
    const val GLYPHVT52_0F = 0x01e0
    const val GLYPHVT52_10 = 0x0200
    const val GLYPHVT52_11 = 0x0220
    const val GLYPHVT52_12 = 0x0240
    const val GLYPHVT52_13 = 0x0260
    const val GLYPHVT52_14 = 0x0280
    const val GLYPHVT52_15 = 0x02a0
    const val GLYPHVT52_16 = 0x02c0
    const val GLYPHVT52_17 = 0x02e0
    const val GLYPHVT52_18 = 0x0300
    const val GLYPHVT52_19 = 0x0320
    const val GLYPHVT52_1A = 0x0340
    const val GLYPHVT52_1B = 0x0360
    const val GLYPHVT52_1C = 0x0380
    const val GLYPHVT52_1D = 0x03a0
    const val GLYPHVT52_1E = 0x03c0
    const val GLYPHVT52_1F = 0x03e0
    const val GLYPHVT52_20 = 0x0400
    const val GLYPHVT52_21 = 0x0420
    const val GLYPHVT52_22 = 0x0440
    const val GLYPHVT52_23 = 0x0460
    const val GLYPHVT52_24 = 0x0480
    const val GLYPHVT52_25 = 0x04a0
    const val GLYPHVT52_26 = 0x04c0
    const val GLYPHVT52_27 = 0x04e0
    const val GLYPHVT52_28 = 0x0500
    const val GLYPHVT52_29 = 0x0520
    const val GLYPHVT52_2A = 0x0540
    const val GLYPHVT52_2B = 0x0560
    const val GLYPHVT52_2C = 0x0580
    const val GLYPHVT52_2D = 0x05a0
    const val GLYPHVT52_2E = 0x05c0
    const val GLYPHVT52_2F = 0x05e0
    const val GLYPHVT52_30 = 0x0600
    const val GLYPHVT52_31 = 0x0620
    const val GLYPHVT52_32 = 0x0640
    const val GLYPHVT52_33 = 0x0660
    const val GLYPHVT52_34 = 0x0680
    const val GLYPHVT52_35 = 0x06a0
    const val GLYPHVT52_36 = 0x06c0
    const val GLYPHVT52_37 = 0x06e0
    const val GLYPHVT52_38 = 0x0700
    const val GLYPHVT52_39 = 0x0720
    const val GLYPHVT52_3A = 0x0740
    const val GLYPHVT52_3B = 0x0760
    const val GLYPHVT52_3C = 0x0780
    const val GLYPHVT52_3D = 0x07a0
    const val GLYPHVT52_3E = 0x07c0
    const val GLYPHVT52_3F = 0x07e0
    const val GLYPHVT52_40 = 0x0800
    const val GLYPHVT52_41 = 0x0820
    const val GLYPHVT52_42 = 0x0840
    const val GLYPHVT52_43 = 0x0860
    const val GLYPHVT52_44 = 0x0880
    const val GLYPHVT52_45 = 0x08a0
    const val GLYPHVT52_46 = 0x08c0
    const val GLYPHVT52_47 = 0x08e0
    const val GLYPHVT52_48 = 0x0900
    const val GLYPHVT52_49 = 0x0920
    const val GLYPHVT52_4A = 0x0940
    const val GLYPHVT52_4B = 0x0960
    const val GLYPHVT52_4C = 0x0980
    const val GLYPHVT52_4D = 0x09a0
    const val GLYPHVT52_4E = 0x09c0
    const val GLYPHVT52_4F = 0x09e0
    const val GLYPHVT52_50 = 0x0a00
    const val GLYPHVT52_51 = 0x0a20
    const val GLYPHVT52_52 = 0x0a40
    const val GLYPHVT52_53 = 0x0a60
    const val GLYPHVT52_54 = 0x0a80
    const val GLYPHVT52_55 = 0x0aa0
    const val GLYPHVT52_56 = 0x0ac0
    const val GLYPHVT52_57 = 0x0ae0
    const val GLYPHVT52_58 = 0x0b00
    const val GLYPHVT52_59 = 0x0b20
    const val GLYPHVT52_5A = 0x0b40
    const val GLYPHVT52_5B = 0x0b60
    const val GLYPHVT52_5C = 0x0b80
    const val GLYPHVT52_5D = 0x0ba0
    const val GLYPHVT52_5E = 0x0bc0
    const val GLYPHVT52_5F = 0x0be0
    const val GLYPHVT52_60 = 0x0c00
    const val GLYPHVT52_61 = 0x0c20
    const val GLYPHVT52_62 = 0x0c40
    const val GLYPHVT52_63 = 0x0c60
    const val GLYPHVT52_64 = 0x0c80
    const val GLYPHVT52_65 = 0x0ca0
    const val GLYPHVT52_66 = 0x0cc0
    const val GLYPHVT52_67 = 0x0ce0
    const val GLYPHVT52_68 = 0x0d00
    const val GLYPHVT52_69 = 0x0d20
    const val GLYPHVT52_6A = 0x0d40
    const val GLYPHVT52_6B = 0x0d60
    const val GLYPHVT52_6C = 0x0d80
    const val GLYPHVT52_6D = 0x0da0
    const val GLYPHVT52_6E = 0x0dc0
    const val GLYPHVT52_6F = 0x0de0
    const val GLYPHVT52_70 = 0x0e00
    const val GLYPHVT52_71 = 0x0e20
    const val GLYPHVT52_72 = 0x0e40
    const val GLYPHVT52_73 = 0x0e60
    const val GLYPHVT52_74 = 0x0e80
    const val GLYPHVT52_75 = 0x0ea0
    const val GLYPHVT52_76 = 0x0ec0
    const val GLYPHVT52_77 = 0x0ee0
    const val GLYPHVT52_78 = 0x0f00
    const val GLYPHVT52_79 = 0x0f20
    const val GLYPHVT52_7A = 0x0f40
    const val GLYPHVT52_7B = 0x0f60
    const val GLYPHVT52_7C = 0x0f80
    const val GLYPHVT52_7D = 0x0fa0
    const val GLYPHVT52_7E = 0x0fc0
    const val GLYPHVT52_7F = 0x0fe0
}
