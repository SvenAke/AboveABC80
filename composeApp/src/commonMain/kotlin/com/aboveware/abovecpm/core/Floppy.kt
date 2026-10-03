package com.aboveware.abovecpm.core

import com.aboveware.abovecpm.ImdParser
import com.aboveware.abovecpm.NativeLib
import com.aboveware.abovecpm.ZXLog
import com.aboveware.abovecpm.splitFilename
import kotlin.math.min

/**
 * Disk Parameter Block (DPB) definition for CP/M 2.2
 */
data class DPB(
    val spt: Int,        // Sectors per track
    val bsh: Int,        // Block shift factor
    val blm: Int,        // Block mask
    val exm: Int,        // Extent mask
    val dsm: Int,        // Max block number (capacity in blocks - 1)
    val drm: Int,        // Max directory entries - 1
    val al0: Int,        // Directory allocation 0
    val al1: Int,        // Directory allocation 1
    val cks: Int,        // Directory check size
    val off: Int         // Number of reserved tracks
) {
    val blockSize: Int get() = 128 shl bsh
    val dirEntries: Int get() = drm + 1
    val dirSizeInSectors: Int get() = (dirEntries * 32 + 127) / 128
}

/**
 * Common CP/M 2.2 Disk Formats
 */
object DiskFormats {
    // Standard 8" Single Sided, Single Density (IBM 3740)
    // 77 tracks, 26 sectors/track, 128 bytes/sector
    val IBM_3740 = DPB(
        spt = 26,
        bsh = 3, blm = 7, exm = 0,
        dsm = 242,
        drm = 63,
        al0 = 0xC0, al1 = 0x00,
        cks = 16,
        off = 2
    )

    // Kaypro II (5.25" SS/DD)
    // 40 tracks, 10 sectors/track, 512 bytes/sector (mapped to 40 sectors/track of 128 bytes)
    val KAYPRO_II = DPB(
        spt = 40,
        bsh = 3, blm = 7, exm = 0,
        dsm = 194,
        drm = 63,
        al0 = 0xC0, al1 = 0x00,
        cks = 16,
        off = 1
    )
}

/**
 * Floppy class representing a CP/M 2.2 diskette image.
 */
class Floppy(
    val dpb: DPB = DiskFormats.IBM_3740,
    val tracks: Int = 77,
    sectorSize: Int = 128
) {
    companion object {
        fun createFromData(newData: ByteArray): Floppy {
            var workingData = newData
            var guessedOff: Int? = null
            var sectorsPerTrack: Int? = null

            // Check for IMD header
            if (workingData.size > 3 && workingData[0] == 'I'.code.toByte() && workingData[1] == 'M'.code.toByte() && workingData[2] == 'D'.code.toByte()) {
                val parser = ImdParser(workingData)
                val tracks = parser.parseAllTracks()

                // Heuristic: find the directory track
                // Look for CP/M directory pattern: User 0-15, Name 8 chars, Ext 3 chars (ASCII)
                for (i in tracks.indices) {
                    val sectors = tracks[i].sectorData
                    val hasFiles = sectors.any { s ->
                        (s.indices step 32).any { entryOff ->
                            val user = s[entryOff].toInt() and 0xFF
                            if (user <= 15) {
                                // Filename and Extension should be printable ASCII
                                (1..11).all { s[entryOff + it].toInt() in 32..126 }
                            } else false
                        }
                    }
                    if (hasFiles) {
                        guessedOff = tracks[i].cylinder
                        ZXLog.diskett("Detected CP/M directory at Cylinder $guessedOff")
                        break
                    }
                }

                if (tracks.isNotEmpty()) {
                    // Convert physical sectors to 128-byte logical sectors
                    sectorsPerTrack = (tracks[0].sectorCount * tracks[0].sectorSize) / 128
                    ZXLog.diskett("IMD: Physical SPT=${tracks[0].sectorCount}, Size=${tracks[0].sectorSize} -> logical SPT=$sectorsPerTrack")
                }

                workingData = parser.toRawDsk()
            }

            var format = when (workingData.size) {
                256256 -> DiskFormats.IBM_3740
                204800 -> DiskFormats.KAYPRO_II
                else -> {
                    // If we have sectorsPerTrack info, try to find a matching format or create one
                    if (sectorsPerTrack == 40) DiskFormats.KAYPRO_II
                    else if (sectorsPerTrack == 26) DiskFormats.IBM_3740
                    else DiskFormats.IBM_3740 // Default
                }
            }

            // Standard 256256-byte CP/M images use OFF=2. Scanning their
            // program tracks produces false directory matches and can move
            // the catalog away from the real directory. The 204800-byte
            // installation images are the exception with their nonstandard
            // catalog track.
            if (guessedOff == null && workingData.size != 256256) {
                guessedOff = detectDirectoryTrack(workingData, format.spt)
            }

            if (guessedOff != null) {
                ZXLog.diskett("Setting reserved tracks (OFF) to $guessedOff")
                format = format.copy(off = guessedOff)
            }

            if (sectorsPerTrack != null && sectorsPerTrack != format.spt) {
                ZXLog.diskett("Updating SPT to $sectorsPerTrack")
                format = format.copy(spt = sectorsPerTrack)
            }

            val tracksCount = workingData.size / (format.spt * 128)
            ZXLog.diskett("Floppy created: SPT=${format.spt}, OFF=${format.off}, Tracks=$tracksCount, Size=${workingData.size}")

            val floppy = Floppy(format, tracks = tracksCount)
            floppy.loadRawData(workingData)
            return floppy
        }

        private fun detectDirectoryTrack(data: ByteArray, sectorsPerTrack: Int): Int? {
            if (sectorsPerTrack <= 0 || data.size < sectorsPerTrack * 128) return null

            val trackCount = data.size / (sectorsPerTrack * 128)
            var bestTrack: Int? = null
            var bestCount = 0

            for (track in 0 until trackCount) {
                var validEntries = 0
                val trackOffset = track * sectorsPerTrack * 128
                for (entry in 0 until (sectorsPerTrack * 4)) {
                    val offset = trackOffset + entry * 32
                    val user = data[offset].toInt() and 0xFF
                    if (user > 15) continue

                    val name = data.copyOfRange(offset + 1, offset + 9)
                        .map { (it.toInt() and 0x7F).toChar() }
                        .joinToString("")
                    val extension = data.copyOfRange(offset + 9, offset + 12)
                        .map { (it.toInt() and 0x7F).toChar() }
                        .joinToString("")

                    if (name.trim().isNotEmpty() &&
                        name.all { it in ' '..'~' } &&
                        extension.all { it in ' '..'~' }
                    ) {
                        validEntries++
                    }
                }

                if (validEntries > bestCount) {
                    bestCount = validEntries
                    bestTrack = track
                }
            }

            return bestTrack
        }
    }

    private val data: ByteArray = ByteArray(tracks * dpb.spt * sectorSize)

    init {
        format()
    }

    /**
     * Fills the entire disk with 0xE5 (CP/M standard for empty sectors/directory).
     */
    fun format() {
        data.fill(0xE5.toByte())
    }

    /**
     * Reads a 128-byte sector from the disk.
     */
    fun readSector(track: Int, sector: Int): ByteArray {
        val offset = getOffset(track, sector)
        return data.copyOfRange(offset, offset + 128)
    }

    /**
     * Writes a 128-byte sector to the disk.
     */
    fun writeSector(track: Int, sector: Int, sectorData: ByteArray) {
        val offset = getOffset(track, sector)
        val bytesToCopy = min(128, sectorData.size)
        System.arraycopy(sectorData, 0, data, offset, bytesToCopy)
    }

    /**
     * Returns the raw byte array of the disk image.
     */
    fun getRawData(): ByteArray = data

    /**
     * Initializes the system tracks (SYSGEN) using the provided image.
     * Writes Track 0 and Track 1 (the reserved system tracks for CP/M).
     */
    fun applySysgen(systemImage: ByteArray) {
        var offset = 0
        for (trk in 0 until dpb.off) {
            for (sec in 1..dpb.spt) {
                if (offset >= systemImage.size) return

                val chunkSize = min(128, systemImage.size - offset)
                val chunk = ByteArray(128) { 0x00.toByte() }
                System.arraycopy(systemImage, offset, chunk, 0, chunkSize)

                writeSector(trk, sec - 1, chunk)
                offset += 128
            }
        }
    }

    /**
     * Loads raw data into the disk image.
     */
    fun loadRawData(newData: ByteArray) {
        System.arraycopy(newData, 0, data, 0, min(data.size, newData.size))
    }

    /**
     * Calculates the byte offset for a given track and sector.
     * Note: CP/M sectors are usually 1-indexed in many formats, but we assume 0-indexed internally.
     */
    private fun getOffset(track: Int, sector: Int): Int {
        return (track * dpb.spt + sector) * 128
    }

    private fun logicalToPhysicalSector(sector: Int): Int {
        val logical = sector.coerceIn(0, dpb.spt - 1)
        if (dpb.spt != 26) return logical

        val skew = intArrayOf(
            1, 7, 13, 19, 25, 5, 11, 17, 23, 3, 9, 15, 21,
            2, 8, 14, 20, 26, 6, 12, 18, 24, 4, 10, 16, 22
        )
        return skew[logical] - 1
    }

    private fun readLogicalSector(track: Int, sector: Int): ByteArray =
        readSector(track, logicalToPhysicalSector(sector))

    private fun writeLogicalSector(track: Int, sector: Int, sectorData: ByteArray) =
        writeSector(track, logicalToPhysicalSector(sector), sectorData)

    /**
     * Directory Entry (32 bytes)
     */
    class DirEntry(val bytes: ByteArray, val dsm: Int) {
        val user: Int get() = bytes[0].toInt() and 0xFF
        val filename: String get() = bytes.sliceArray(1..8).decodeToString().trim()
        val extension: String get() = bytes.sliceArray(9..11).decodeToString().trim()
        val extent: Int get() = bytes[12].toInt() and 0xFF
        val s1: Int get() = bytes[13].toInt() and 0xFF
        val s2: Int get() = bytes[14].toInt() and 0xFF
        val recordCount: Int get() = bytes[15].toInt() and 0xFF

        /**
         * Returns the list of blocks allocated to this entry.
         */
        fun getBlocks(): List<Int> {
            val blocks = mutableListOf<Int>()
            if (dsm < 256) {
                // 8-bit block pointers (16 per entry)
                for (i in 0 until 16) {
                    val b = bytes[16 + i].toInt() and 0xFF
                    if (b != 0) blocks.add(b)
                }
            } else {
                // 16-bit block pointers (8 per entry, little endian)
                for (i in 0 until 8) {
                    val low = bytes[16 + i * 2].toInt() and 0xFF
                    val high = bytes[16 + i * 2 + 1].toInt() and 0xFF
                    val b = low or (high shl 8)
                    if (b != 0) blocks.add(b)
                }
            }
            return blocks
        }

        fun isDeleted(): Boolean = user == 0xE5
        fun isActive(): Boolean = user in 0..15

        override fun toString(): String {
            if (isDeleted()) return "[DELETED]"
            val blocks = getBlocks()
            return "User $user: $filename.$extension (Ext: $extent, Recs: $recordCount, Blocks: ${blocks.size})"
        }
    }

    /**
     * Lists all files in the directory.
     */
    fun listFiles(): List<DirEntry> {
        val dirSectors = dpb.dirSizeInSectors
        val entries = mutableListOf<DirEntry>()

        var currentTrack = dpb.off
        var currentSector = 0

        (0 until dirSectors).forEach { i ->
            val sectorData = readLogicalSector(currentTrack, currentSector)
            for (j in 0 until 4) { // 4 entries per 128-byte sector
                val entryBytes = sectorData.copyOfRange(j * 32, (j + 1) * 32)
                entries.add(DirEntry(entryBytes, dpb.dsm))
            }

            currentSector++
            if (currentSector >= dpb.spt) {
                currentSector = 0
                currentTrack++
            }
        }
        return entries
    }

    fun readFile(name: String, extension: String): ByteArray? {
        val formattedName = name.uppercase().take(8)
        val formattedExt = extension.uppercase().take(3)
        val extents = listFiles()
            .filter { entry ->
                entry.isActive() &&
                        entry.filename.map { (it.code and 0x7F).toChar() }.joinToString("")
                            .trimEnd() == formattedName &&
                        entry.extension.map { (it.code and 0x7F).toChar() }.joinToString("")
                            .trimEnd() == formattedExt
            }
            .sortedBy { it.extent }

        if (extents.isEmpty()) return null

        val result = ArrayList<Byte>()
        for (extent in extents) {
            val extentBytes = extent.getBlocks().flatMap { block ->
                readBlock(block).toList()
            }
            val extentSize = extent.recordCount * 128
            result.addAll(extentBytes.take(extentSize))
        }
        return result.toByteArray()
    }

    /**
     * Higher level block access.
     */
    fun readBlock(blockIndex: Int): ByteArray {
        val blockSize = dpb.blockSize
        val result = ByteArray(blockSize)

        val sectorsPerBlock = blockSize / 128
        val startSectorIndex = blockIndex * sectorsPerBlock + (dpb.off * dpb.spt)

        for (i in 0 until sectorsPerBlock) {
            val totalSector = startSectorIndex + i
            val track = totalSector / dpb.spt
            val sector = totalSector % dpb.spt
            val sectorData = readLogicalSector(track, sector)
            System.arraycopy(sectorData, 0, result, i * 128, 128)
        }
        return result
    }

    fun writeBlock(blockIndex: Int, blockData: ByteArray) {
        val blockSize = dpb.blockSize
        val sectorsPerBlock = blockSize / 128
        val startSectorIndex = blockIndex * sectorsPerBlock + (dpb.off * dpb.spt)

        for (i in 0 until sectorsPerBlock) {
            val totalSector = startSectorIndex + i
            val track = totalSector / dpb.spt
            val sector = totalSector % dpb.spt
            val sectorData = ByteArray(128)
            System.arraycopy(blockData, i * 128, sectorData, 0, 128)
            writeLogicalSector(track, sector, sectorData)
        }
    }

    /**
     * Deletes a file from the disk.
     */
    fun deleteFile(name: String, extension: String): Boolean {
        val formattedName = name.uppercase().padEnd(8, ' ').take(8)
        val formattedExt = extension.uppercase().padEnd(3, ' ').take(3)
        var deletedCount = 0

        val dirSectors = dpb.dirSizeInSectors
        var currentTrack = dpb.off
        var currentSector = 0

        (0 until dirSectors).forEach { i ->
            val sectorData = readLogicalSector(currentTrack, currentSector)
            var sectorChanged = false
            for (j in 0 until 4) {
                val offset = j * 32
                val user = sectorData[offset].toInt() and 0xFF
                if (user != 0xE5) {
                    val entryName = sectorData
                        .sliceArray(offset + 1 until offset + 9)
                        .map { (it.toInt() and 0x7F).toByte() }
                        .toByteArray()
                        .decodeToString()
                    val entryExt = sectorData
                        .sliceArray(offset + 9 until offset + 12)
                        .map { (it.toInt() and 0x7F).toByte() }
                        .toByteArray()
                        .decodeToString()

                    if (entryName == formattedName && entryExt == formattedExt) {
                        sectorData[offset] = 0xE5.toByte()
                        // Optional: clear rest of entry for cleanliness
                        for (k in 1 until 32) sectorData[offset + k] = 0.toByte()
                        sectorChanged = true
                        deletedCount++
                    }
                }
            }

            if (sectorChanged) {
                writeLogicalSector(currentTrack, currentSector, sectorData)
            }

            currentSector++
            if (currentSector >= dpb.spt) {
                currentSector = 0
                currentTrack++
            }
        }
        return deletedCount > 0
    }

    /**
     * Deletes every CP/M directory entry while preserving reserved system tracks.
     */
    fun clearFiles() {
        val dirSectors = dpb.dirSizeInSectors
        var currentTrack = dpb.off
        var currentSector = 0

        repeat(dirSectors) {
            val sectorData = readLogicalSector(currentTrack, currentSector)
            var sectorChanged = false
            for (entry in 0 until 4) {
                val offset = entry * 32
                val user = sectorData[offset].toInt() and 0xFF
                if (user in 0..15) {
                    sectorData[offset] = 0xE5.toByte()
                    sectorChanged = true
                }
            }
            if (sectorChanged) writeLogicalSector(currentTrack, currentSector, sectorData)

            currentSector++
            if (currentSector >= dpb.spt) {
                currentSector = 0
                currentTrack++
            }
        }
    }

    fun getFreeSpace(): Long {
        val usedBlocks = BooleanArray(dpb.dsm + 1)
        val al = (dpb.al0 shl 8) or dpb.al1
        for (i in 0 until 16) {
            if ((al and (0x8000 ushr i)) != 0) usedBlocks[i] = true
        }

        val entries = listFiles()
        entries.filter { it.isActive() }.forEach { entry ->
            entry.getBlocks().forEach { block ->
                if (block in usedBlocks.indices) usedBlocks[block] = true
            }
        }

        val freeBlocks = usedBlocks.count { !it }
        return freeBlocks.toLong() * dpb.blockSize
    }

    /**
     * Injects a file into the disk image.
     */
    fun injectFile(name: String, extension: String, fileData: ByteArray): Boolean {
        val originalData = data.copyOf()

        fun fail(): Boolean {
            System.arraycopy(originalData, 0, data, 0, data.size)
            return false
        }

        // Remove an existing copy so its directory entries and blocks can be reused.
        deleteFile(name, extension)
        val blockSize = dpb.blockSize
        val sectorsPerBlock = blockSize / 128
        val maxBlocksPerEntry = if (dpb.dsm < 256) 16 else 8
        val sectorsPerEntry = maxBlocksPerEntry * sectorsPerBlock
        val sectorsRemainingTotal = (fileData.size + 127) / 128

        // 1. Calculate required directory entries
        val entriesNeeded =
            if (sectorsRemainingTotal == 0) 1
            else (sectorsRemainingTotal + sectorsPerEntry - 1) / sectorsPerEntry

        // Find N contiguous directory entries if possible for sequential order
        val entryIndices = findContiguousFreeDirectoryEntries(entriesNeeded)
        if (entryIndices == null) {
            ZXLog.wtf(
                "Inject $name.$extension failed: directory entries needed=$entriesNeeded " +
                        "free=${countFreeDirectoryEntries()}"
            )
            return fail()
        }

        val blocksNeeded = (fileData.size + blockSize - 1) / blockSize

        // 2. Find used blocks
        val usedBlocks = BooleanArray(dpb.dsm + 1)
        val al = (dpb.al0 shl 8) or dpb.al1
        for (i in 0 until 16) {
            if ((al and (0x8000 ushr i)) != 0) usedBlocks[i] = true
        }

        val entries = listFiles()
        entries.filter { it.isActive() }.forEach { entry ->
            entry.getBlocks().forEach { block ->
                if (block in usedBlocks.indices) usedBlocks[block] = true
            }
        }

        // 3. Find free blocks
        val freeBlocks = mutableListOf<Int>()
        for (i in 0..dpb.dsm) {
            if (!usedBlocks[i]) freeBlocks.add(i)
        }

        if (freeBlocks.size < blocksNeeded) {
            ZXLog.wtf(
                "Inject $name.$extension failed: blocks needed=$blocksNeeded " +
                        "free=${freeBlocks.size}, fileBytes=${fileData.size}"
            )
            return fail()
        }

        // 4. Write data to blocks
        val allocatedBlocks = freeBlocks.take(blocksNeeded)
        allocatedBlocks.forEachIndexed { i, block ->
            val start = i * blockSize
            val end = min(start + blockSize, fileData.size)
            val chunk = ByteArray(blockSize) { index ->
                if (start + index < end) fileData[start + index] else 0x1A.toByte() // padding with CTRL-Z
            }
            writeBlock(block, chunk)
        }

        // 5. Update directory entries
        var currentSectorOffset = 0
        var blockPointer = 0

        val formattedName = name.uppercase().padEnd(8, ' ').take(8)
        val formattedExt = extension.uppercase().padEnd(3, ' ').take(3)

        for ((extentIndex, entryIndex) in entryIndices.withIndex()) {
            val entryData = ByteArray(32)
            entryData[0] = 0 // User 0
            formattedName.forEachIndexed { i, c -> entryData[1 + i] = c.code.toByte() }
            formattedExt.forEachIndexed { i, c -> entryData[9 + i] = c.code.toByte() }

            // Each extent covers the number of blocks supported by this DPB.
            entryData[12] = (extentIndex and 0x1F).toByte()
            entryData[13] = 0 // S1
            entryData[14] = (extentIndex shr 5).toByte() // S2 (high bits of extent)

            val sectorsInThisEntry =
                min(sectorsPerEntry, sectorsRemainingTotal - currentSectorOffset)
            entryData[15] = sectorsInThisEntry.toByte()

            // Block allocation for this entry
            val blocksInThisEntry =
                if (sectorsInThisEntry == 0) 0 else (sectorsInThisEntry + sectorsPerBlock - 1) / sectorsPerBlock

            val actualBlocks = min(maxBlocksPerEntry, blocksInThisEntry)

            for (i in 0 until actualBlocks) {
                if (blockPointer < allocatedBlocks.size) {
                    val b = allocatedBlocks[blockPointer++]
                    if (dpb.dsm < 256) {
                        entryData[16 + i] = b.toByte()
                    } else {
                        entryData[16 + i * 2] = (b and 0xFF).toByte()
                        entryData[16 + i * 2 + 1] = (b shr 8).toByte()
                    }
                }
            }

            // Write entry back to disk
            val dirSector = entryIndex / 4
            val dirEntryInSector = entryIndex % 4
            val track = dpb.off + (dirSector / dpb.spt)
            val sector = dirSector % dpb.spt
            val sectorData = readLogicalSector(track, sector)
            System.arraycopy(entryData, 0, sectorData, dirEntryInSector * 32, 32)
            writeLogicalSector(track, sector, sectorData)

            currentSectorOffset += sectorsInThisEntry
        }

        val storedData = readFile(name, extension)
        if (storedData == null) {
            ZXLog.wtf("Inject $name.$extension failed: verification read returned null")
            return fail()
        }
        if (storedData.size < fileData.size ||
            !storedData.copyOf(fileData.size).contentEquals(fileData)
        ) {
            ZXLog.wtf(
                "Inject $name.$extension failed: verification mismatch " +
                        "storedBytes=${storedData.size}, fileBytes=${fileData.size}"
            )
            return fail()
        }

        return true
    }

    private fun findContiguousFreeDirectoryEntries(needed: Int): List<Int>? {
        val allFree = mutableListOf<Int>()
        val dirSectors = dpb.dirSizeInSectors
        var currentTrack = dpb.off
        var currentSector = 0

        for (i in 0 until dirSectors) {
            val sectorData = readLogicalSector(currentTrack, currentSector)
            for (j in 0 until 4) {
                if (isFreeDirectoryEntry(sectorData, j * 32)) {
                    allFree.add(i * 4 + j)
                }
            }
            currentSector++
            if (currentSector >= dpb.spt) {
                currentSector = 0
                currentTrack++
            }
        }

        if (allFree.isEmpty()) return null

        // Try to find N consecutive indices
        for (i in 0..allFree.size - needed) {
            val sub = allFree.subList(i, i + needed)
            var consecutive = true
            for (k in 0 until needed - 1) {
                if (sub[k + 1] != sub[k] + 1) {
                    consecutive = false
                    break
                }
            }
            if (consecutive) return sub
        }

        // Fallback: first N available slots
        return if (allFree.size >= needed) allFree.take(needed) else null
    }

    private fun countFreeDirectoryEntries(): Int {
        var count = 0
        val dirSectors = dpb.dirSizeInSectors
        var currentTrack = dpb.off
        var currentSector = 0
        (0 until dirSectors).forEach { i ->
            val sectorData = readLogicalSector(currentTrack, currentSector)
            for (j in 0 until 4) {
                if (isFreeDirectoryEntry(sectorData, j * 32)) count++
            }
            currentSector++
            if (currentSector >= dpb.spt) {
                currentSector = 0
                currentTrack++
            }
        }
        return count
    }

    private fun isFreeDirectoryEntry(sectorData: ByteArray, offset: Int): Boolean {
        if ((sectorData[offset].toInt() and 0xFF) == 0xE5) return true
        if (sectorData[offset].toInt() != 0) return false

        // Some CP/M images use a zero-filled, never-used entry instead of E5.
        return sectorData.copyOfRange(offset + 1, offset + 32).all { it.toInt() == 0 }
    }

    private fun findFreeDirectoryEntryIndex(): Int? {
        val dirSectors = dpb.dirSizeInSectors
        var currentTrack = dpb.off
        var currentSector = 0

        for (i in 0 until dirSectors) {
            val sectorData = readLogicalSector(currentTrack, currentSector)
            for (j in 0 until 4) {
                if (isFreeDirectoryEntry(sectorData, j * 32)) {
                    return i * 4 + j
                }
            }
            currentSector++
            if (currentSector >= dpb.spt) {
                currentSector = 0
                currentTrack++
            }
        }
        return null
    }
}
