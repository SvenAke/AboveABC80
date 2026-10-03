package com.aboveware.aboveabc80.core

import aboveabc80.composeapp.generated.resources.Res
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.aboveware.aboveabc80.Assembler
import com.aboveware.aboveabc80.NativeLib
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.currentTimeMillis
import com.aboveware.aboveabc80.loadLocalDisk
import com.aboveware.aboveabc80.saveLocalDisk
import com.aboveware.aboveabc80.toHex
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Disk Controller handling CP/M disk access via BIOS intercepts.
 */
class DiskController {
    enum class Source { RESOURCE, LOCAL }
    data class MountedDisk(val name: String, val source: Source)

    companion object {
        val instance = DiskController()
        const val MAX_DRIVES = 16
    }

    private val drives = arrayOfNulls<Floppy>(MAX_DRIVES)
    val mountedDisks = mutableStateMapOf<Int, MountedDisk>()

    var lastAccessTime by mutableLongStateOf(0L)
        private set

    var lastAccessDriveIndex by mutableIntStateOf(0)
        private set

    fun getFloppy(index: Int): Floppy? = if (index in 0 until MAX_DRIVES) drives[index] else null

    var currentDriveIndex = 0
        set(value) {
            val validIndex = value.coerceIn(0 until MAX_DRIVES)
            field = validIndex
        }
    var currentTrack = 0
    var currentSector = 0
    var dmaAddress = 0x0080

    fun loadDrive(index: Int, floppy: Floppy, mountedDisk: MountedDisk) {
        if (index in 0 until MAX_DRIVES) {
            drives[index] = floppy
            mountedDisks[index] = mountedDisk
            val firstEntry = floppy.listFiles().firstOrNull { it.isActive() }
            Abc80Log.diskett(
                "Disk drive ${'A' + index}: mounted ${mountedDisk.name} (${mountedDisk.source}), " +
                    "SPT=${floppy.dpb.spt}, OFF=${floppy.dpb.off}, " +
                    "firstFile=${firstEntry?.filename}.${firstEntry?.extension}"
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadFromResource(index: Int, resourcePath: String) {
        if (index !in 0 until MAX_DRIVES) return
        try {
            val data = Res.readBytes(resourcePath)
            val floppy = Floppy()
            floppy.loadRawData(data)
            val name = resourcePath.substringAfterLast("/")
            loadDrive(index, floppy, MountedDisk(name, Source.RESOURCE))
        } catch (e: Exception) {
            Abc80Log.wtf("Failed to load disk resource $resourcePath: ${e.message}")
        }
    }

    fun loadFromLocal(index: Int, name: String): String? {
        if (index !in 0 until MAX_DRIVES) return null
        val data = loadLocalDisk(name)
        if (data != null) {
            val floppy = Floppy.createFromData(data)

            // If it was an IMD file, save the converted version as .DSK
            var actualName = name
            if (name.lowercase().endsWith(".imd")) {
                val dskName = name.substringBeforeLast(".") + ".DSK"
                if (saveLocalDisk(dskName, floppy.getRawData())) {
                    actualName = dskName
                    Abc80Log.diskett("Converted $name to $dskName")
                }
            }

            loadDrive(index, floppy, MountedDisk(actualName, Source.LOCAL))
            return actualName
        } else {
            Abc80Log.wtf("Failed to load local disk $name")
            return null
        }
    }

    fun clearDrive(index: Int) {
        if (index in 0 until MAX_DRIVES) {
            drives[index] = null
            mountedDisks.remove(index)
            Abc80Log.diskett("Disk drive ${'A' + index}: cleared")
        }
    }

    fun biosRead(): Int {
        if (currentDriveIndex !in 0 until MAX_DRIVES) return 1
        val floppy = drives[currentDriveIndex]
        if (floppy != null) {
            try {
                // DMA boundary check
                if (dmaAddress > 0xFF80) {
                    Abc80Log.wtf("Disk READ: DMA address 0x${dmaAddress.toHex(4)} too high!")
                    return 1
                }

                // CP/M 26-sector media (IBM 3740) uses 1-based sector IDs in BIOS calls.
                // 40-sector media (Kaypro) uses 0-based logical sector numbers.
                val sector = if (floppy.dpb.spt == 26) {
                    currentSector - 1
                } else {
                    currentSector
                }.coerceIn(0, floppy.dpb.spt - 1)

                Abc80Log.diskett(
                    "Disk READ: Drive ${'A' + currentDriveIndex}, Track $currentTrack, Sector $currentSector, DMA=0x${
                        dmaAddress.toHex(
                            4
                        )
                    }"
                )

                lastAccessTime = currentTimeMillis()
                lastAccessDriveIndex = currentDriveIndex

                val data = floppy.readSector(currentTrack, sector)
                NativeLib.getObject().copyToMemory(dmaAddress, data)
                return 0
            } catch (e: Exception) {
                Abc80Log.wtf("Disk READ ERROR: ${e.message}")
                return 1
            }
        }
        return 1
    }

    fun biosWrite(): Int {
        val floppy = drives[currentDriveIndex]
        val mounted = mountedDisks[currentDriveIndex]
        if (floppy != null) {
            try {
                // DMA boundary check
                if (dmaAddress > 0xFF80) {
                    Abc80Log.wtf("Disk WRITE: DMA address 0x${dmaAddress.toHex(4)} too high!")
                    return 1
                }

                val sector = if (floppy.dpb.spt == 26) {
                    currentSector - 1
                } else {
                    currentSector
                }.coerceIn(0, floppy.dpb.spt - 1)

                Abc80Log.diskett(
                    "Disk WRITE: Drive ${'A' + currentDriveIndex}, Track $currentTrack, Sector $currentSector, DMA=0x${
                        dmaAddress.toHex(
                            4
                        )
                    }"
                )

                lastAccessTime = currentTimeMillis()
                lastAccessDriveIndex = currentDriveIndex

                val data =
                    NativeLib.getObject().getMemory().copyOfRange(dmaAddress, dmaAddress + 128)
                floppy.writeSector(currentTrack, sector, data)

                // Disks loaded via BIOS are almost always LOCAL now, but we check to be safe
                if (mounted?.source == Source.LOCAL) {
                    saveLocalDisk(mounted.name, floppy.getRawData())
                }

                return 0
            } catch (e: Exception) {
                Abc80Log.wtf("Disk WRITE ERROR: ${e.message}")
                return 1
            }
        }
        return 1
    }
}
