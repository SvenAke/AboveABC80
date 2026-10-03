package com.aboveware.aboveabc80.terminal

class Graphics {
    enum class CharacterSetIndex { G0, G1, G2, G3 }

    val characterSets = CharacterSets()
    val designated = DesignatedCharacterSets()

    var gL = CharacterSetIndex.G0
    var gR = CharacterSetIndex.G2
    private var ss = CharacterSetIndex.G0 // Single Shift

    class CharacterSets {
        private fun getROMSet(block: (CharacterSet.CharacterSets) -> CharacterSet.BaseCharacterSet): CharacterSet.BaseCharacterSet {
            return if (CharacterSet.isLoaded()) block(CharacterSet.characterSets) else CharacterSet.fallbackSet
        }

        val ascii get() = getROMSet { it.asciiCharacterSet }
        val decSupplemental get() = getROMSet { it.decSupplementalCharacterSet }
        val isoLatin1 get() = getROMSet { it.isoLatinAlphabetNr1SupplementCharacterSet }
        val specialGraphics get() = getROMSet { it.decSpecialGraphicsCharacterSet }
        val unitedKingdom get() = getROMSet { it.britishCharacterSet }
        val denmarkNorway get() = getROMSet { it.norwegianDanishCharacterSet }
        val dutch get() = getROMSet { it.dutchCharacterSet }
        val finnish get() = getROMSet { it.finnishCharacterSet }
        val french get() = getROMSet { it.frenchCharacterSet }
        val frenchCanadian get() = getROMSet { it.frenchCanadianCharacterSet }
        val german get() = getROMSet { it.germanCharacterSet }
        val italian get() = getROMSet { it.italianCharacterSet }
        val portuguese get() = getROMSet { it.portugueseCharacterSet }
        val spanish get() = getROMSet { it.spanishCharacterSet }
        val swedish get() = getROMSet { it.swedishCharacterSet }
        val swiss get() = getROMSet { it.swissCharacterSet }
        val vt52 get() = getROMSet { it.vt52CharacterSet }
        val vt52Graphics get() = getROMSet { it.vt52SpecialGraphics }

        val drcsFontBuffer = CharacterSet.DRCSFontBuffer(byteArrayOf())

        fun preferredSupplemental() =
            if (VT320Settings.userPreferredCharacterSet == 0) decSupplemental else isoLatin1

        fun all(): List<CharacterSet.BaseCharacterSet> = listOf(
            ascii, decSupplemental, isoLatin1, specialGraphics,
            unitedKingdom, denmarkNorway, dutch, finnish, french,
            frenchCanadian, german, italian, portuguese, spanish,
            swedish, swiss, vt52, vt52Graphics, drcsFontBuffer
        )
    }

    inner class DesignatedCharacterSets {
        // We store the designator string to allow resolving the correct set dynamically
        private val originalDesignators = arrayOf("B", "B", "<", "<")
        private val is96 =
            booleanArrayOf(false, false, true, true) // G2, G3 default to Supplemental (96)
        private val overrides = arrayOf<CharacterSet.BaseCharacterSet?>(null, null, null, null)

        operator fun get(index: CharacterSetIndex): CharacterSet.BaseCharacterSet {
            val idx = index.ordinal
            val override = overrides[idx]
            if (override != null) return override

            return resolveDesignator(originalDesignators[idx], is96[idx])
        }

        fun is96Set(index: CharacterSetIndex): Boolean {
            return is96[index.ordinal]
        }

        fun designate(index: CharacterSetIndex, designator: String, is96Set: Boolean) {
            originalDesignators[index.ordinal] = designator
            is96[index.ordinal] = is96Set
            overrides[index.ordinal] = null
        }

        fun designate(
            index: CharacterSetIndex,
            set: CharacterSet.BaseCharacterSet,
            is96Set: Boolean
        ) {
            overrides[index.ordinal] = set
            is96[index.ordinal] = is96Set
        }

        fun refreshAll() {
            // Nothing to do here as get() resolves dynamically
        }
    }

    fun designateGraphicSet(
        index: CharacterSetIndex,
        designator: String,
        is96Set: Boolean = false
    ) {
        if (designator == characterSets.drcsFontBuffer.designator() && designator.isNotEmpty()) {
            designated.designate(index, characterSets.drcsFontBuffer, is96Set)
            return
        }
        designated.designate(index, designator, is96Set)
    }

    private fun resolveDesignator(
        designator: String,
        is96Set: Boolean
    ): CharacterSet.BaseCharacterSet {
        val cs = characterSets
        return when (designator) {
            "B" -> cs.ascii
            "0" -> cs.specialGraphics
            "A" -> if (is96Set) cs.isoLatin1 else cs.unitedKingdom
            "4" -> cs.dutch
            "C", "5" -> cs.finnish
            "R" -> cs.french
            "9", "Q" -> cs.frenchCanadian
            "K" -> cs.german
            "Y" -> cs.italian
            "E", "6", "`" -> cs.denmarkNorway
            "Z" -> cs.spanish
            "H", "7" -> cs.swedish
            "=" -> cs.swiss
            "<" -> cs.preferredSupplemental()
            "%5" -> cs.decSupplemental
            "%6" -> cs.portuguese
            else -> CharacterSet.fallbackSet
        }
    }

    fun refreshDesignations() {
        designated.refreshAll()
    }

    fun getGlyph(c: Char, terminal: VT320): CharacterSet.Glyph? {
        if (TerminalManager.operatingMode == TerminalManager.OperatingMode.VT52) {
            val set =
                if (terminal.vt52GraphicsMode) characterSets.vt52Graphics else characterSets.vt52
            return set.getGlyph(c)
        }

        val code = c.code

        // Fallback for Unicode special graphics characters (e.g. used in boot screens)
        if (code > 255) {
            characterSets.specialGraphics.getGlyph(c)?.let { return it }
        }

        val activeIndex = if (ss != CharacterSetIndex.G0) {
            val idx = ss
            ss = CharacterSetIndex.G0
            idx
        } else if (code in 0x00..0x7F) {
            gL
        } else if (code in 0xA0..0xFF) {
            gR
        } else {
            return null
        }

        val set = designated[activeIndex]
        val is96 = designated.is96Set(activeIndex)

        // If NRC is active but disabled by user setting, fallback to ASCII
        var finalSet = set
        if (finalSet is CharacterSet.NationalCharacterSet && !VT320Settings.nationalReplacement) {
            finalSet = characterSets.ascii
        }

        val finalCode = if (is96) {
            if (code in 0x20..0x7F) (code + 0x80) and 0xFF else code
        } else {
            code % 128
        }

        return finalSet.getGlyph(finalCode.toChar())
    }

    fun ss2() {
        ss = CharacterSetIndex.G2
    }

    fun ss3() {
        ss = CharacterSetIndex.G3
    }

    fun clearSingleShift() {
        ss = CharacterSetIndex.G0
    }

    fun reset() {
        designated.designate(CharacterSetIndex.G0, "B", false)
        designated.designate(CharacterSetIndex.G1, "B", false)
        designated.designate(CharacterSetIndex.G2, "<", true)
        designated.designate(CharacterSetIndex.G3, "<", true)
        gL = CharacterSetIndex.G0
        gR = CharacterSetIndex.G2
        clearSingleShift()
    }

    fun softReset() {
        clearSingleShift()
        refreshDesignations()
    }

    fun updateMappings(mappings: Map<Char, Char>) {
        // DRCS or other dynamic mapping logic could go here
    }
}
