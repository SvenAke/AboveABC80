package com.aboveware.aboveabc80

/**
 * Basic IMD (ImageDisk) parser to extract raw sector data.
 * IMD files contain a text header followed by binary track data.
 */
class ImdParser(val data: ByteArray) {
    private var pos = 0

    init {
        // Skip text header ending with 0x1A (CTRL-Z)
        while (pos < data.size && data[pos].toInt() != 0x1A) {
            pos++
        }
        if (pos < data.size) pos++ // Skip the 0x1A
    }

    data class TrackInfo(
        val mode: Int,
        val cylinder: Int,
        val head: Int,
        val sectorCount: Int,
        val sectorSizeCode: Int,
        val sectorSize: Int,
        val sectorMap: IntArray,
        val sectorData: Array<ByteArray>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TrackInfo) return false
            if (mode != other.mode) return false
            if (cylinder != other.cylinder) return false
            if (head != other.head) return false
            if (sectorCount != other.sectorCount) return false
            if (sectorSizeCode != other.sectorSizeCode) return false
            if (sectorSize != other.sectorSize) return false
            if (!sectorMap.contentEquals(other.sectorMap)) return false
            if (!sectorData.contentDeepEquals(other.sectorData)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = mode
            result = 31 * result + cylinder
            result = 31 * result + head
            result = 31 * result + sectorCount
            result = 31 * result + sectorSizeCode
            result = 31 * result + sectorSize
            result = 31 * result + sectorMap.contentHashCode()
            result = 31 * result + sectorData.contentDeepHashCode()
            return result
        }
    }

    fun parseAllTracks(): List<TrackInfo> {
        val originalPos = pos
        val tracks = mutableListOf<TrackInfo>()
        while (pos < data.size) {
            val track = parseTrack() ?: break
            tracks.add(track)
        }
        pos = originalPos // Reset for potential second pass
        return tracks
    }

    private fun parseTrack(): TrackInfo? {
        if (pos >= data.size) return null

        val mode = data[pos++].toInt() and 0xFF
        val cylinder = data[pos++].toInt() and 0xFF
        val headRaw = data[pos++].toInt() and 0xFF
        val head = headRaw and 0x0F
        val sectorCount = data[pos++].toInt() and 0xFF
        val sectorSizeCode = data[pos++].toInt() and 0xFF

        val sectorSize = 128 shl sectorSizeCode

        // Sector numbering map
        val sectorMap = IntArray(sectorCount)
        for (i in 0 until sectorCount) {
            sectorMap[i] = data[pos++].toInt() and 0xFF
        }

        // Optional sector cylinder/head maps
        if (headRaw and 0x80 != 0) { // Cylinder map
            pos += sectorCount
        }
        if (headRaw and 0x40 != 0) { // Head map
            pos += sectorCount
        }

        val sectors = Array(sectorCount) { ByteArray(sectorSize) }

        for (i in 0 until sectorCount) {
            val recordType = data[pos++].toInt() and 0xFF
            when (recordType) {
                0 -> { // Data not available
                    sectors[i].fill(0xE5.toByte())
                }

                1, 3, 5, 7 -> { // Normal data (with optional Deleted Mark or Error)
                    System.arraycopy(data, pos, sectors[i], 0, sectorSize)
                    pos += sectorSize
                }

                2, 4, 6, 8 -> { // Compressed data (with optional Deleted Mark or Error)
                    val value = data[pos++]
                    sectors[i].fill(value)
                }

                else -> { // Unknown - try to fill with empty
                    sectors[i].fill(0xE5.toByte())
                }
            }
        }

        return TrackInfo(
            mode and 0x0F,
            cylinder,
            head,
            sectorCount,
            sectorSizeCode,
            sectorSize,
            sectorMap,
            sectors
        )
    }

    /**
     * Converts the IMD data to a raw sector stream (DSK).
     * Reorders sectors into logical (ID) order and respects track/head geometry.
     */
    fun toRawDsk(): ByteArray {
        val allTracks = parseAllTracks()
        if (allTracks.isEmpty()) return byteArrayOf()

        // Determine geometry
        val sectorSize = allTracks.maxOf { it.sectorSize }
        val maxCyl = allTracks.maxOf { it.cylinder }
        val maxHead = allTracks.maxOf { it.head }
        val headsCount = maxHead + 1

        // CP/M usually expects consistent SPT across all tracks in a raw image
        val maxPhysicalSectors = allTracks.maxOf { it.sectorCount }
        val logicalSectorsPerPhysicalSector = sectorSize / 128
        val logicalSectorsPerTrack = maxPhysicalSectors * logicalSectorsPerPhysicalSector

        val totalSize = (maxCyl + 1) * headsCount * logicalSectorsPerTrack * 128
        val raw = ByteArray(totalSize)
        raw.fill(0xE5.toByte()) // Default CP/M empty value

        ZXLog.diskett(
            "Converting IMD to raw DSK: Cyls=${maxCyl + 1}, Heads=$headsCount, " +
                "physicalSPT=$maxPhysicalSectors, logicalSPT=$logicalSectorsPerTrack, Size=$totalSize"
        )

        for (track in allTracks) {
            // Calculate absolute track offset in the raw image
            val trackOffset =
                (track.cylinder * headsCount + track.head) * logicalSectorsPerTrack * 128

            // Find min sector number on this track to handle 0-based or 1-based numbering
            val minSector = track.sectorMap.minOrNull() ?: 1

            for (i in 0 until track.sectorCount) {
                val sectorID = track.sectorMap[i]
                val physicalIndex = sectorID - minSector

                if (physicalIndex in 0 until maxPhysicalSectors) {
                    val source = track.sectorData[i]
                    for (logicalPart in 0 until logicalSectorsPerPhysicalSector) {
                        val destOffset =
                            trackOffset + (physicalIndex * logicalSectorsPerPhysicalSector + logicalPart) * 128
                        val sourceOffset = logicalPart * 128
                        if (destOffset + 128 <= raw.size && sourceOffset + 128 <= source.size) {
                            System.arraycopy(source, sourceOffset, raw, destOffset, 128)
                        }
                    }
                }
            }
        }
        return raw
    }
}
