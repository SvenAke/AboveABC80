package com.aboveware.abc80

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
        private val imageFormats = listOf("fd2", "abc830", "abc830-ufd", "fd4d", "abc832")

        internal fun detectFormatName(image: ByteArray): String? {
            val candidates = imageFormats.map { it to Abc80FloppyLayout(it, 0) }
                .filter { (_, layout) -> layout.format?.sectorFormat?.size == image.size }
            return candidates.maxByOrNull { (_, layout) -> layout.directoryEntryCount(image) }?.first
        }

        fun diskType(image: ByteArray): String {
            val formatName = detectFormatName(image)
            return when (formatName) {
                "fd2", "dd80" -> "SS/SD"
                "abc830", "abc830-ufd", "fd2d", "dd82", "mo" -> "SS/DD"
                "fd4d", "dd84" -> "DS/DD"
                "abc832", "abc832-ufd", "abc834", "mf" -> "DS/QD"
                else -> when (image.size) {
                    81920 -> "SS/SD"
                    163840 -> "SS/DD"
                    327680 -> "DS/DD"
                    655360 -> "DS/QD"
                    else -> "SS/DD"
                }
            }
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        fun readFile(image: ByteArray, name: String): ByteArray? {
            val layout = detectFormatName(image)?.let { Abc80FloppyLayout(it, 0) }
                ?: throw IllegalArgumentException("Diskformatet stöds inte")
            val format = requireNotNull(layout.format)
            val diskName = Abc80CloudDiskImport.encodeText(
                name.substringBeforeLast('.', name).uppercase().padEnd(8) +
                    name.substringAfterLast('.', "").uppercase().padEnd(3)
            )
            require(diskName.size == 11) { "Ogiltigt ABC80-filnamn: $name" }
            val usedDirectories = layout.usedDirectorySectors(image)
            for (dir in 0 until format.numberOfDirectorySectors) {
                if (!usedDirectories[dir]) continue
                val sector = layout.physicalSector(format.directorySectors.a + dir) * 256
                for (slot in 1..15) {
                    val entry = sector + slot * 16
                    if (image[entry] == 0.toByte() || image[entry] == 0xFF.toByte()) continue
                    if (!image.copyOfRange(entry + 4, entry + 15).contentEquals(diskName)) continue
                    val header = layout.offset(image[entry].toUByte().toInt(), image[entry + 1].toUByte().toInt())
                    require(header in 0..image.size - 256) { "Ogiltig ABC80-filhuvudpekare för $name" }
                    val usedSectors = (image[entry + 2].toUByte().toInt()) or
                        (image[entry + 3].toUByte().toInt() shl 8)
                    val sectorsPerCluster = 1 shl format.clusterShift
                    val sectors = mutableListOf<Int>()
                    for (offset in 4 until 240 step 2) {
                        if (image[header + offset] == 0xFF.toByte()) break
                        val file = image[header + offset].toUByte().toInt()
                        val address = image[header + offset + 1].toUByte().toInt()
                        val startCluster = (file shl 3) + (address shr 5)
                        val clusterCount = (address and 31) + 1
                        repeat(clusterCount * sectorsPerCluster) {
                            val logicalSector = (startCluster shl format.clusterShift) + it
                            val at = layout.physicalSector(logicalSector) * 256
                            require(at in 0..image.size - 256) { "Ogiltig ABC80-sektorpekare för $name" }
                            sectors.add(at)
                        }
                    }
                    val dataSectors = if (usedSectors > 0) sectors.drop(1).take(usedSectors - 1) else sectors.drop(1)
                    return dataSectors.flatMap { image.copyOfRange(it + 3, it + 256).asIterable() }.toByteArray()
                }
            }
            return null
        }

        fun freeSpace(image: ByteArray): Long {
            val layout = detectFormatName(image)?.let { Abc80FloppyLayout(it, 0) }
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
            val layout = detectFormatName(image)?.let { Abc80FloppyLayout(it, 0) }
                ?: throw IllegalArgumentException("Diskavbildens storlek stöds inte")
            val existingVolume = readVolumeName(image)
            val result = if (clear) layout.create(existingVolume) else image.copyOf()
            files.forEach { (name, records) -> layout.writeRecords(result, name, records) }
            return result
        }

        fun readVolumeName(image: ByteArray): String {
            val layout = detectFormatName(image)?.let { Abc80FloppyLayout(it, 0) } ?: return "SYSTEM"
            val diskFormat = layout.format ?: return "SYSTEM"
            val offsetsToTry = listOf(
                layout.physicalSector(diskFormat.systemSector) * 256 + 0x87,
                diskFormat.systemSector * 256 + 0x87,
                0x87
            )
            for (startOffset in offsetsToTry) {
                val firstByte = image.getOrNull(startOffset)?.toUByte()?.toInt() ?: 0
                if (firstByte in 32..126 && firstByte != 0x40 && firstByte != 0x00 && firstByte != 0xFF) {
                    val ascii = StringBuilder()
                    for (index in startOffset until (startOffset + 0x78)) {
                        val b = image.getOrNull(index)?.toUByte()?.toInt() ?: 0
                        if (b == 0x0D || b == 0x00 || b == 0xFF) break
                        val char = when (b) {
                            0x5D -> 'Å'
                            0x5B -> 'Ä'
                            0x5C -> 'Ö'
                            0x7D -> 'å'
                            0x7B -> 'ä'
                            0x7C -> 'ö'
                            0x5E -> 'Ü'
                            0x7E -> 'ü'
                            0x40 -> 'É'
                            0x60 -> 'é'
                            in 32..126 -> b.toChar()
                            else -> '.'
                        }
                        ascii.append(char)
                    }
                    val name = ascii.toString().trim()
                    if (name.isNotEmpty()) return name
                }
            }
            return "SYSTEM"
        }

        fun writeVolumeName(image: ByteArray, volumeName: String) {
            val layout = detectFormatName(image)?.let { Abc80FloppyLayout(it, 0) } ?: return
            val diskFormat = layout.format ?: return
            val offsetsToTry = listOf(
                layout.physicalSector(diskFormat.systemSector) * 256 + 0x87,
                diskFormat.systemSector * 256 + 0x87,
                0x87
            )
            val startOffset = offsetsToTry.firstOrNull { offset ->
                val b = image.getOrNull(offset)?.toUByte()?.toInt() ?: 0
                b in 32..126 && b != 0x40 && b != 0x00 && b != 0xFF
            } ?: (layout.physicalSector(diskFormat.systemSector) * 256 + 0x87)

            val name = volumeName.trim().ifEmpty { "SYSTEM" }
            var offset = startOffset
            val maxOffset = startOffset + 0x77
            for (char in name) {
                if (offset >= maxOffset) break
                val code = abc80CharCode(char) ?: char.code
                image[offset++] = (code and 0xFF).toByte()
            }
            if (offset <= startOffset + 0x78) {
                image[offset] = 0x0D.toByte()
            }
        }

        fun generateUniqueDiskFileName(volumeName: String, existingFiles: List<String>): String {
            val rawBase = volumeName.trim().ifEmpty { "SYSTEM" }
            var sanitized = rawBase.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
                .uppercase()
            if (sanitized.isEmpty()) sanitized = "DISK"
            if (sanitized.startsWith("SYSTEM_DISKETT") || sanitized.startsWith("SYSTEM_DISK")) {
                sanitized = "SYSTEM"
            } else {
                sanitized = sanitized.take(16)
            }

            var fileName = "$sanitized.DSK"
            var counter = 1
            val existingSet = existingFiles.map { it.uppercase() }.toSet()
            while (existingSet.contains(fileName.uppercase())) {
                fileName = "${sanitized}_$counter.DSK"
                counter++
            }
            return fileName
        }

        /** Lists "NAME.EXT" and size in bytes for each file on a raw disk image, or null if the size matches no known format. */
        fun list(image: ByteArray): List<Pair<String, Int>>? {
            val layout = detectFormatName(image)?.let { Abc80FloppyLayout(it, 0) } ?: return null
            return layout.dir(image).map { entry ->
                val (name, length) = entry.split("!")
                val base = name.take(8).trim()
                val ext = name.drop(8).trim()
                (if (ext.isEmpty()) base else "$base.$ext").toAndroid() to length.toInt()
            }
        }
    }

    private fun directoryEntryCount(image: ByteArray): Int {
        val usedDirectories = usedDirectorySectors(image)
        var count = 0
        for (directoryIndex in usedDirectories.indices) {
            if (!usedDirectories[directoryIndex]) continue
            val sectorStart = physicalSector(format!!.directorySectors.a + directoryIndex) * 256
            for (slot in 1..15) {
                val entry = sectorStart + slot * 16
                val firstSector = image[entry].toUByte().toInt()
                val sectorPart = image[entry + 1].toUByte().toInt()
                val name = (4..14).map { image[entry + it].toUByte().toInt() }
                if (firstSector == 0 || firstSector == 0xFF ||
                    name.any { it !in 0x20..0x7E } ||
                    name.all { it == ' '.code || it == '@'.code }
                ) continue
                val header = offset(firstSector, sectorPart)
                if (header in 0..image.size - 256) count++
            }
        }
        return count
    }

    private var offset = 0
    private var currentLogicalSector = 0
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
        val dir = getComputerModelStorageDir("dr")
        return File(dir, "$drive.dsk").absolutePath
    }

    fun inTransfer(): Boolean = pointer in 0 until buffer.size

    fun read(): Byte {
        if (pointer >= buffer.size) return 0x00
        return buffer[pointer++]
    }

    fun write(data: UByte) {
        if (pointer >= buffer.size) return
        buffer[pointer++] = data.toByte()
        if (pointer > 255) {
            randomAccessFile?.apply {
                seek(offset.toLong())
                write(buffer)
                val dirFormat = format
                if (dirFormat != null && dirFormat.directorySectors.b != 0) {
                    val dirAStart = dirFormat.directorySectors.a
                    val dirBStart = dirFormat.directorySectors.b
                    val dirCount = dirFormat.numberOfDirectorySectors
                    if (currentLogicalSector in dirBStart until (dirBStart + dirCount)) {
                        val mirrorSector = dirAStart + (currentLogicalSector - dirBStart)
                        seek((physicalSector(mirrorSector) shl 8).toLong())
                        write(buffer)
                    } else if (currentLogicalSector in dirAStart until (dirAStart + dirCount)) {
                        val mirrorSector = dirBStart + (currentLogicalSector - dirAStart)
                        seek((physicalSector(mirrorSector) shl 8).toLong())
                        write(buffer)
                    }
                }
            }
        }
    }

    fun open(physicalFileNumber: Int, physicalSectorNumber: Int, dos21SystemBuffer: Boolean = false) {
        offset = offset(physicalFileNumber, physicalSectorNumber, dos21SystemBuffer)
        block(offset)
        pointer = 0
    }

    internal fun physicalSectorForAddress(file: Int, sector: Int): Int =
        offset(file, sector) / 256

    private fun offset(
        physicalFileNumber: Int,
        physicalSectorNumber: Int,
        dos21SystemBuffer: Boolean = false
    ): Int {
        fileNumber = physicalFileNumber.and(0b00001111)
        libraryOffset = physicalFileNumber.and(0b11110000).shr(4)
        sector = physicalSectorNumber.shr(5) * 256L

        val diskFormat = format!!
        val cluster = physicalFileNumber.shl(3) + physicalSectorNumber.shr(5)
        val sectorInCluster = physicalSectorNumber.and(31)
        val logicalSector = if (dos21SystemBuffer && diskFormat.clusterShift > 0 && sectorInCluster == 0) {
            when (cluster) {
                6 -> diskFormat.systemSector
                7 -> diskFormat.systemSector + 1
                in 8..15 -> diskFormat.directorySectors.a + (cluster - 8)
                in 16..23 -> diskFormat.directorySectors.a + (cluster - 16)
                else -> cluster.shl(diskFormat.clusterShift)
            }
        } else {
            cluster.shl(diskFormat.clusterShift) + sectorInCluster
        }
        currentLogicalSector = logicalSector
        var offset = physicalSector(logicalSector)
        offset = offset.shl(8)
        return offset
    }

    internal fun physicalSector(logicalSector: Int): Int =
        (logicalSector and 15.inv()) or ((logicalSector * 7) and 15)

    private fun writeRecords(image: ByteArray, filename: String, records: ByteArray) {
        require(records.isNotEmpty() && records.size % 253 == 0) { "Ogiltiga ABC80-filposter" }
        val diskFormat = requireNotNull(format)
        val clusterShift = diskFormat.clusterShift
        val sectorsPerCluster = 1 shl clusterShift
        val totalClusters = diskFormat.sectorFormat.sectors shr clusterShift
        val name = filename.substringAfterLast('/').substringAfterLast('\\').toCloudDisplayName().uppercase()
        val base = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "")
        require(base.length in 1..8 && extension.length in 0..3 &&
            (base + extension).all { (it in '!'..'~' || it in "ÅÄÖ") && it !in ":/\\." }) {
            "Ogiltigt ABC80-filnamn: $filename (högst 8+3 tecken)"
        }
        val diskName = Abc80CloudDiskImport.encodeText(base.padEnd(8) + extension.padEnd(3))
        val bitmap = physicalSector(diskFormat.systemSector) * 256
        require(image[bitmap + 255].toInt() == clusterShift &&
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
        fun mark(cluster: Int, used: Boolean) {
            require(cluster in 0 until totalClusters) { "Ogiltig klusterpekare" }
            val at = bitmap + cluster / 8
            val mask = 0x80 shr (cluster % 8)
            image[at] = if (used) (image[at].toInt() or mask).toByte()
                else (image[at].toInt() and mask.inv()).toByte()
        }
        if (replacing) {
            val headerCluster = (image[selectedEntry].toInt() and 255) * 8 +
                ((image[selectedEntry + 1].toInt() and 255) shr 5)
            val header = physicalSector(headerCluster shl clusterShift) * 256
            for (at in 4 until 240 step 2) {
                if (image[header + at] == 0xFF.toByte()) break
                val start = (image[header + at].toInt() and 255) * 8 +
                    ((image[header + at + 1].toInt() and 255) shr 5)
                val count = (image[header + at + 1].toInt() and 31) + 1
                repeat(count) { mark(start + it, false) }
            }
        }
        val count = records.size / 253
        val neededClusters = (count + 1 + sectorsPerCluster - 1) shr clusterShift
        val clusters = (0 until totalClusters).filter {
            (image[bitmap + it / 8].toInt() and (0x80 shr (it % 8))) == 0
        }.take(neededClusters)
        require(clusters.size == neededClusters) { "Disketten är full" }
        val maxRun = if (clusterShift > 0) 16 else 32
        val runs = mutableListOf<Pair<Int, Int>>()
        clusters.forEach { cluster ->
            val last = runs.lastOrNull()
            if (last != null && last.first + last.second == cluster && last.second < maxRun) {
                runs[runs.lastIndex] = last.first to last.second + 1
            } else runs.add(cluster to 1)
        }
        require(runs.size <= 118) { "Disketten är för fragmenterad" }
        val identifier = ((selectedEntry % 256) or selectedDirectory).toByte()
        clusters.forEach { mark(it, true) }
        val sectors = clusters.flatMap { cluster ->
            (0 until sectorsPerCluster).map { (cluster shl clusterShift) + it }
        }
        sectors.take(count + 1).forEachIndexed { index, sector ->
            val at = physicalSector(sector) * 256
            image.fill(0, at, at + 256)
            image[at] = identifier
            image[at + 1] = index.toByte()
            image[at + 2] = (index shr 8).toByte()
            if (index > 0) records.copyInto(image, at + 3, (index - 1) * 253, index * 253)
        }
        val firstCluster = clusters.firstOrNull() ?: 0
        val header = physicalSector(firstCluster shl clusterShift) * 256
        image.fill(0xFF.toByte(), header + 3, header + 240)
        runs.forEachIndexed { index, (start, length) ->
            image[header + 4 + index * 2] = (start / 8).toByte()
            image[header + 5 + index * 2] = (((start % 8) shl 5) or (length - 1)).toByte()
        }
        image[selectedEntry] = (firstCluster / 8).toByte()
        image[selectedEntry + 1] = ((firstCluster % 8) shl 5).toByte()
        val usedSectors = if (clusterShift > 0) count + 1 else 0
        image[selectedEntry + 2] = usedSectors.toByte()
        image[selectedEntry + 3] = (usedSectors shr 8).toByte()
        diskName.copyInto(image, selectedEntry + 4)
        image[selectedEntry + 15] = 0xFF.toByte()
        if (!replacing) image[bitmap + 239 + selectedDirectory]++
        if (diskFormat.directorySectors.b != 0) {
            val main = physicalSector(diskFormat.directorySectors.a + selectedDirectory) * 256
            image.copyInto(image, physicalSector(diskFormat.directorySectors.b + selectedDirectory) * 256,
                main, main + 256)
        }
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
        val bytes = try { readImage() } catch (_: Exception) { null } ?: return "DR$drive:"
        return readVolumeName(bytes)
    }

    fun format(volumeName: String? = null) = load(
        ByteArrayInputStream(create(volumeName ?: (try { readVolumeName(readImage()) } catch (_: Exception) { "SYSTEM" }))),
        ""
    )

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
                if (bytes[offset].toInt() == -1) continue
                var hsect = bytes[offset].toUByte().toInt().shl(3) + bytes[offset + 1].toUByte().toInt().shr(5)
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
        if (bytes[oo + 3] != 0xFF.toByte()) return 0
        var sum = 0
        val clusterMultiplier = 1 shl (format?.clusterShift ?: 0)
        for (k in 4..240 step 2) {
            if (bytes[oo + k].toUByte().toInt() == 255) break
            sum += (bytes[oo + k + 1].and(31) + 1) * clusterMultiplier
        }
        return sum * 256
    }

    private fun usedDirectorySectors(bytes: ByteArray): MutableList<Boolean> {
        val userDirectorySectors = mutableListOf<Boolean>()
        val diskFormat = requireNotNull(format)
        val bitmapIndex = physicalSector(diskFormat.systemSector) * 256
        for (index in 0 until diskFormat.numberOfDirectorySectors) {
            userDirectorySectors.add(bytes[bitmapIndex + 239 + index].toUByte().toInt() >= 1)
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
    fun save(stream: OutputStream): Boolean = try {
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
    fun create(volumeName: String = "SYSTEM"): ByteArray {
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

            // Write volume name to sector 0
            val nameBytes = volumeName.trim().ifEmpty { "SYSTEM" }
            var offset = bitmapIndex + 0x87
            val maxOffset = bitmapIndex + 0xFE
            for (char in nameBytes) {
                if (offset >= maxOffset) break
                val code = abc80CharCode(char) ?: char.code
                image[offset++] = (code and 0xFF).toByte()
            }
            if (offset <= bitmapIndex + 0xFF) {
                image[offset] = 0x0D.toByte()
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