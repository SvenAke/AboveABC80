package com.aboveware.aboveabc80

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.lang.Integer.max
import kotlin.experimental.and
import kotlin.experimental.or

@ExperimentalUnsignedTypes
@OptIn(ExperimentalUnsignedTypes::class)
class Abc80FloppyLayout(formatName: String, val drive: Int) {
    private val formats = mapOf(
        "fd2" to Abc80Floppy.DiskFormat(SSSD(), 0, 6, directorySectors(), 8),
        "dd80" to Abc80Floppy.DiskFormat(SSSD(), 0, 6, directorySectors(), 8, 0x6ef, 0x300, 0x600),
        "fd2d" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "dd82" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "abc830" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "mo" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "abc830-ufd" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, Abc80Floppy.DirectorySectors(16, 0), 16),
        "fd4d" to Abc80Floppy.DiskFormat(DSDD(), 0, 6, directorySectors(), 8),
        "dd84" to Abc80Floppy.DiskFormat(DSDD(), 0, 6, directorySectors(), 8),
        "abc832" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, Abc80Floppy.DirectorySectors(16, 0), 16),
        "abc832-ufd" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, directorySectors(), 8),
        "abc834" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, Abc80Floppy.DirectorySectors(16, 0), 16),
        "mf" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, Abc80Floppy.DirectorySectors(16, 0), 16)
    )

    private fun directorySectors() = Abc80Floppy.DirectorySectors(8, 16)

    // Format	Encoding	Sector size	Sectors/track	Tracks
    // SSSD		FM(?)		256 bytes	 8		        40          80kB
    // SSDD		MFM		    256 bytes	16		        40          160kB
    // DSDD		MFM		    256 bytes	16		        40 x2       320kB
    // DSQD		MFM		    256 bytes	16		        80 x2       640kB
    private fun DSQD() = Abc80Floppy.SectorFormat(80, 2, 16)
    private fun DSDD() = Abc80Floppy.SectorFormat(40, 2, 16)
    private fun SSDD() = Abc80Floppy.SectorFormat(40, 1, 16)
    private fun SSSD() = Abc80Floppy.SectorFormat(40, 1, 8)

    val format = formats[formatName]

    companion object {
        private val imageFormats = listOf("fd2", "abc830", "fd4d", "abc832")

        fun freeSpace(image: ByteArray): Long {
            val layout = imageFormats.map { Abc80FloppyLayout(it, 0) }
                .firstOrNull { it.format?.sectorFormat?.size == image.size }
                ?: throw IllegalArgumentException("Diskavbildens storlek stöds inte")
            val diskFormat = requireNotNull(layout.format)
            val bitmap = layout.physicalSector(diskFormat.systemSector) * 256
            val clusters = diskFormat.sectorFormat.sectors shr diskFormat.clusterShift
            val free = (0 until clusters).count {
                (image[bitmap + it / 8].toInt() and (0x80 shr (it % 8))) == 0
            }
            return free.toLong() * (1 shl diskFormat.clusterShift) * 253
        }

        fun importFiles(
            image: ByteArray,
            files: List<Pair<String, ByteArray>>,
            clear: Boolean = false
        ): ByteArray {
            val layout = imageFormats.map { Abc80FloppyLayout(it, 0) }
                .firstOrNull { it.format?.sectorFormat?.size == image.size }
                ?: throw IllegalArgumentException("Diskavbildens storlek stöds inte")
            require(layout.format!!.clusterShift == 0) {
                "Molnimport stöder disketter på 80, 160 och 320 kB"
            }
            val result = if (clear) layout.create() else image.copyOf()
            files.forEach { (name, records) -> layout.writeRecords(result, name, records) }
            return result
        }

        /** Lists "NAME.EXT" and size in bytes for each file on a raw disk image, or null if the size matches no known format. */
        fun list(image: ByteArray): List<Pair<String, Int>>? {
            val layout = imageFormats.map { Abc80FloppyLayout(it, 0) }
                .firstOrNull { it.format?.sectorFormat?.size == image.size } ?: return null
            return layout.dir(image).map { entry ->
                val (name, length) = entry.split("!")
                val base = name.take(8).trim()
                val ext = name.drop(8).trim()
                (if (ext.isEmpty()) base else "$base.$ext") to length.toInt()
            }
        }
    }

    private var offset = 0
    private var fileNumber: Int = 0
    private var libraryOffset: Int = 0
    private var sector = 0L
    var status: UByte = 0b11001001u
    private var randomAccessFile: RandomAccessFile? = null
    private fun size() = format!!.sectorFormat.size
    private var buffer = ByteArray(256) { 0x00 }
    private var pointer = -1
    fun open(create: Boolean = false): Abc80FloppyLayout {
        close()
        Abc80Log.floppy("OPEN ${diskPath()}")
        val file = File(diskPath())
        if (!file.exists() || create) {
            Abc80Log.floppy("CREATE")
            file.delete()
            file.createNewFile()
        }
        if (file.exists()) {
            randomAccessFile = RandomAccessFile(file, "rwd")
            Abc80Log.floppy("OPEN ${diskPath()} exists ${volumeName()}")
        }
        return this
    }

    private fun diskPath(): String {
        val dir = File(getAppStorageDir(), "dr").apply { if (!exists()) mkdirs() }
        return File(dir, "$drive.dsk").absolutePath
    }

    fun read(): Byte {
        if (pointer >= buffer.size) return 0x00
        return buffer[pointer++]
    }

    fun write(data: UByte) {
        buffer[pointer++] = data.toByte()
        if (pointer > 255) {
            randomAccessFile?.apply {
                seek(offset.toLong())
                write(buffer)
            }
        }
    }

    fun open(physicalFileNumber: Int, physicalSectorNumber: Int) {
        offset = offset(physicalFileNumber, physicalSectorNumber)
        block(offset)
        pointer = 0
    }

    private fun offset(physicalFileNumber: Int, physicalSectorNumber: Int): Int {
        fileNumber = physicalFileNumber.and(0b00001111)
        libraryOffset = physicalFileNumber.and(0b11110000).shr(4)
        sector = physicalSectorNumber.shr(5) * 256L
        val secPerCluster = 1.shl(format!!.clusterShift)

        var offset =
            ((physicalFileNumber.shl(3) + physicalSectorNumber.shr(5)) * secPerCluster +
                    physicalSectorNumber.and(31))
        offset = physicalSector(offset)
        offset = offset.shl(8)
        return offset
    }

    private fun physicalSector(logicalSector: Int): Int =
        (logicalSector and 15.inv()) or ((logicalSector * 7) and 15)

    private fun writeRecords(image: ByteArray, filename: String, records: ByteArray) {
        require(records.isNotEmpty() && records.size % 253 == 0) { "Ogiltiga ABC80-filposter" }
        val diskFormat = requireNotNull(format)
        val name = filename.substringAfterLast('/').substringAfterLast('\\').toCloudDisplayName().uppercase()
        val base = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "")
        require(base.length in 1..8 && extension.length in 0..3 &&
            (base + extension).all { (it in '!'..'~' || it in "ÅÄÖ") && it !in ":/\\." }) {
            "Ogiltigt ABC80-filnamn: $filename (högst 8+3 tecken)"
        }
        val diskName = Abc80CloudDiskImport.encodeText(base.padEnd(8) + extension.padEnd(3))
        val bitmap = physicalSector(diskFormat.systemSector) * 256
        require(image[bitmap + 255].toInt() == 0 &&
            (0 until diskFormat.numberOfDirectorySectors).all {
                (image[bitmap + 239 + it].toInt() and 255) in 1..16
            }) { "Disketten är inte korrekt formaterad för ABC80" }
        var selectedEntry = -1
        var selectedDirectory = -1
        var freeEntry = -1
        var freeDirectory = -1
        for (directory in 0 until diskFormat.numberOfDirectorySectors) {
            val sector = physicalSector(diskFormat.directorySectors.a + directory) * 256
            for (slot in 1..15) {
                val entry = sector + slot * 16
                val used = (image[entry].toInt() and 255) !in listOf(0, 255)
                if (!used && freeEntry < 0) {
                    freeEntry = entry
                    freeDirectory = directory
                }
                if (used && image.copyOfRange(entry + 4, entry + 15).contentEquals(diskName)) {
                    selectedEntry = entry
                    selectedDirectory = directory
                }
            }
        }
        val replacing = selectedEntry >= 0
        if (!replacing) {
            selectedEntry = freeEntry
            selectedDirectory = freeDirectory
        }
        require(selectedEntry >= 0) { "Diskettens katalog är full" }
        fun mark(sector: Int, used: Boolean) {
            require(sector in 0 until diskFormat.sectorFormat.sectors) { "Ogiltig klusterpekare" }
            val at = bitmap + sector / 8
            val mask = 0x80 shr (sector % 8)
            image[at] = if (used) (image[at].toInt() or mask).toByte()
                else (image[at].toInt() and mask.inv()).toByte()
        }
        if (replacing) {
            val headerSector = (image[selectedEntry].toInt() and 255) * 8 +
                ((image[selectedEntry + 1].toInt() and 255) shr 5)
            val header = physicalSector(headerSector) * 256
            for (at in 4 until 240 step 2) {
                if (image[header + at] == 0xFF.toByte()) break
                val start = (image[header + at].toInt() and 255) * 8 +
                    ((image[header + at + 1].toInt() and 255) shr 5)
                val count = (image[header + at + 1].toInt() and 31) + 1
                repeat(count) { mark(start + it, false) }
            }
        }
        val count = records.size / 253
        val sectors = (0 until diskFormat.sectorFormat.sectors).filter {
            (image[bitmap + it / 8].toInt() and (0x80 shr (it % 8))) == 0
        }.take(count + 1)
        require(sectors.size == count + 1) { "Disketten är full" }
        val runs = mutableListOf<Pair<Int, Int>>()
        sectors.forEach { sector ->
            val last = runs.lastOrNull()
            if (last != null && last.first + last.second == sector && last.second < 32) {
                runs[runs.lastIndex] = last.first to last.second + 1
            } else runs.add(sector to 1)
        }
        require(runs.size <= 118) { "Disketten är för fragmenterad" }
        val identifier = ((selectedEntry % 256) or selectedDirectory).toByte()
        sectors.forEachIndexed { index, sector ->
            mark(sector, true)
            val at = physicalSector(sector) * 256
            image.fill(0, at, at + 256)
            image[at] = identifier
            image[at + 1] = index.toByte()
            image[at + 2] = (index shr 8).toByte()
            if (index > 0) records.copyInto(image, at + 3, (index - 1) * 253, index * 253)
        }
        val header = physicalSector(sectors.first()) * 256
        image.fill(0xFF.toByte(), header + 3, header + 240)
        runs.forEachIndexed { index, (start, length) ->
            image[header + 4 + index * 2] = (start / 8).toByte()
            image[header + 5 + index * 2] = (((start % 8) shl 5) or (length - 1)).toByte()
        }
        image[selectedEntry] = (sectors.first() / 8).toByte()
        image[selectedEntry + 1] = ((sectors.first() % 8) shl 5).toByte()
        image[selectedEntry + 2] = 0
        image[selectedEntry + 3] = 0
        diskName.copyInto(image, selectedEntry + 4)
        image[selectedEntry + 15] = 0xFF.toByte()
        if (!replacing) image[bitmap + 239 + selectedDirectory]++
        if (diskFormat.directorySectors.b != 0) {
            val main = physicalSector(diskFormat.directorySectors.a + selectedDirectory) * 256
            image.copyInto(image, physicalSector(diskFormat.directorySectors.b + selectedDirectory) * 256,
                main, main + 256)
        }
        image.copyInto(image, physicalSector(diskFormat.systemSector + 1) * 256, bitmap, bitmap + 256)
    }

    private fun block(address: Int) {
        randomAccessFile?.apply {
            seek(address.toLong())
            read(buffer)
        }
    }

    fun close() {
        Abc80Log.floppy("CLOSE")
        randomAccessFile?.close()
        randomAccessFile = null
    }

    fun readImage(): ByteArray {
        val file = checkNotNull(randomAccessFile) { "Disketten är inte öppen" }
        val position = file.filePointer
        return try {
            ByteArray(size()).also {
                file.seek(0)
                file.readFully(it)
            }
        } finally {
            file.seek(position)
        }
    }

    private fun volumeName(): String {
        if (randomAccessFile?.length()?.toInt() != size())
            return "NO DISKETTE"
        block(0)
        return if (buffer[0x87].toInt().toChar() !in 'A'..'~') "DR$drive:"
        else {
            val ascii = StringBuilder()
            for (index in 0x87..0xff) {
                if (buffer[index] == 0x0d.toByte()) return ascii.toString()
                val asChar = buffer[index].toInt().toChar()
                ascii.append(if (asChar in ' '..'~') asChar else ".")
            }
            ascii.toString()
        }
    }

    fun format() = load(ByteArrayInputStream(create()), "")

    fun load(stream: InputStream, name: String): Boolean = try {
        open(create = true)
        Abc80Log.floppy("FLOPPY $name ${stream.available().toLong()} ${size()} $randomAccessFile")
        when {
            stream.available() != size() -> false
            randomAccessFile != null -> {
                randomAccessFile?.apply {
                    seek(0)
                    while (stream.read(buffer) == buffer.size) {
                        write(buffer)
                    }
                    seek(0)
                    close()
                    open()
                }
                true
            }
            else -> false
        }
    } catch (e: IOException) {
        Abc80Log.floppy("LOAD $e")
        false
    }
    /**
     * Read 'n' parse directory sector
     */
    private fun dir(bytes: ByteArray): MutableList<String> {
        val usedDirectorySectors = usedDirectorySectors(bytes)
        val filenames = mutableListOf<String>()
        for (directoryIndex in 0 until format!!.numberOfDirectorySectors) {
            if (!usedDirectorySectors[directoryIndex]) continue
            val sectorStartOffset = physicalSector(format.directorySectors.a + directoryIndex) * 256
            for (index in 16 until 256 step 16) {
                val offset = sectorStartOffset + index
                if (bytes[offset].toInt() == 0x00 || bytes[offset].toInt() == -1) continue
                var hsect = bytes[offset].toInt().shl(3) + bytes[offset + 1].toInt().shr(5)
                hsect = hsect.shl(format.clusterShift)
                if (hsect == 0 ||
                    (bytes[offset + 1].toInt() and bytes[offset + 1].toInt()) == -1 ||
                    bytes[offset + 4].toInt() == 0x00 ||
                    bytes[offset + 4].toInt() == -1
                ) continue   // Empty slot

                val length = getFileSize(bytes, offset(bytes[offset].toUByte().toInt(), bytes[offset + 1].toUByte().toInt()))
                val name = StringBuilder()
                for (n in 4..14)
                    name.append(bytes[offset + n].toInt().toChar())
                name.append("!")
                name.append(length)
                filenames.add(name.toString())
            }
        }
        return filenames
    }

    private fun getFileSize(bytes: ByteArray, oo: Int): Int {
        require(oo >= 0 && oo <= bytes.size - 256) {
            "Invalid ABC80 file header sector: $oo (image size ${bytes.size})"
        }
        var sum = 0
        for (k in 4..240 step 2) {
            if (bytes[oo + k].toUByte().toInt() == 255) break
            sum += bytes[oo + k + 1].and(31) + 1
        }
        return sum * 256
    }

    private fun usedDirectorySectors(bytes: ByteArray): MutableList<Boolean> {
        val userDirectorySectors = mutableListOf<Boolean>()
        val diskFormat = requireNotNull(format)
        val bitmapIndex = physicalSector(diskFormat.systemSector) * 256
        for (index in 0 until diskFormat.numberOfDirectorySectors) {
            userDirectorySectors.add(bytes[bitmapIndex + 239 + index].toUByte().toInt() > 1)
        }
        return userDirectorySectors
    }

    fun eject() = if (File(diskPath()).delete()) {
        close()
        Abc80Log.floppy("DELETE ${diskPath()}")
        true
    } else
        false


    /**
     * @return true if the disk has a floppy inserted.
     */
    fun isValid(): Boolean {
        return (randomAccessFile != null &&
                randomAccessFile?.length()?.toInt() == size()).apply {
            Abc80Log.floppy("IsValid $this ${randomAccessFile?.length()} == ${size()}")
        }
    }

    /**
     * Save this disk to the stream given
     *
     * @param stream the stream to write this floppy to
     * @return true if we succeeded in our endeavour
     */
    fun save(stream: OutputStream) = try {
        Abc80Log.floppy("SAVE")
        randomAccessFile?.apply {
            while (read(buffer) == buffer.size) {
                stream.write(buffer)
            }
            Abc80Log.floppy("SAVE $filePointer")
            eject()
            return true
        }
        false
    } catch (e: IOException) {
        Abc80Log.floppy("SAVE $e")
        false
    }

    /**
     * @return the floppy size, in bytes
     */
    private fun unAllocatedSize(): Int {
        var size = 0
        randomAccessFile?.apply {
            format?.apply {
                for (index in format.unAllocatedIndex..format.unAllocatedIndex + 0xa0) {
                    seek(index.toLong())
                    if (read() == 0x00)
                        size += 8
                }
            }
        }
        return size
    }

    /**
     * @return the floppy size, in bytes
     */
    private fun allocatedSize(): Int {
        var size = 0
        randomAccessFile?.apply {
            format?.apply {
                for (index in format.allocatedIndex..format.allocatedIndex + 0xa0) {
                    seek(index.toLong())
                    size += 8 - read().countOneBits()
                }
            }
        }
        return size
    }

    /**
     * Create an empty disk image.
     */
    fun create(): ByteArray {
        var image = ByteArray(0) { 0x40 }
        format?.apply {
            image = ByteArray(sectorFormat.size) { 0x40 }

            image.fill(0xff.toByte(), format.unAllocatedIndex, 0x103)
            image.fill(0x00, 0x103, format.unAllocatedIndex + (sectorFormat.c * sectorFormat.s).shr(3))

            val bitmapIndex = systemSector.shl(8)
            // Create empty bitmap
            image.fill(0x00, bitmapIndex, bitmapIndex + 256)

            /* Mark nonexistent clusters busy */
            for (index in sectorFormat.sectors.shr(clusterShift) until 239 * 8) {
                val i = bitmapIndex + index.shr(3)
                image[i] = image[i].or(0x80.shr(index.and(0x07)).toByte())
            }

            // 239..254 is the MFD (master file table) bitmap
            image.fill(0x01, bitmapIndex + 239, bitmapIndex + 239 + numberOfDirectorySectors)

            /* This is claimed in a message found on ABC-klubben MSG */
            image[bitmapIndex + 255] = clusterShift.toByte()

            // Mark all the system sectors used
            var startIndex = numberOfDirectorySectors + max(directorySectors.a, directorySectors.b)
            val sectorsPerCluster = 1.shl(clusterShift)

            startIndex = (startIndex + sectorsPerCluster - 1).shr(clusterShift)
            for (index in 0 until startIndex) {
                val i = bitmapIndex + index.shr(3)
                image[i] = image[i].or(0x80.shr(index.and(0x07)).toByte())
            }

            // Copy bitmap to backup empty bitmap (= bad block map)
            image.copyInto(image, bitmapIndex + 256, bitmapIndex, bitmapIndex + 256)

            // Create MFD directory sectors
            val directorySectorIndex = directorySectors.a.shl(8)
            for (index in 0 until numberOfDirectorySectors) {
                val fromIndex = directorySectorIndex + index * 256
                image.fill(0x00, fromIndex, fromIndex + 16)
                image.fill(0xff.toByte(), fromIndex + 16, fromIndex + 256)
            }
            // Create backup MFD if it exists
            if (directorySectors.b != 0) {
                val toIndex = directorySectors.b.shl(8)
                val fromIndex = directorySectors.a.shl(8)
                val size = numberOfDirectorySectors.shl(8)
                image.copyInto(image, toIndex, fromIndex, fromIndex + size)
            }
        }
        // The controller applies 7:1 interleave to every logical sector address.
        val physicalImage = ByteArray(image.size)
        for (logicalSector in 0 until image.size / 256) {
            image.copyInto(
                physicalImage,
                physicalSector(logicalSector) * 256,
                logicalSector * 256,
                (logicalSector + 1) * 256
            )
        }
        return physicalImage
    }

}