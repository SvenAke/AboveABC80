package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.DiskFormats
import com.aboveware.aboveabc80.core.Floppy
import org.junit.Test
import java.io.File

class ImdTest {
    @Test
    fun testAnalyzeInstallDiskImage() {
        // Try to find the file in common locations
        val possiblePaths = listOf(
            "composeApp/src/commonMain/composeResources/files/INSTALL.IMD",
            "src/commonMain/composeResources/files/INSTALL.IMD"
        )

        var imdFile: File? = null
        for (path in possiblePaths) {
            val f = File(path)
            if (f.exists()) {
                imdFile = f
                break
            }
        }

        if (imdFile == null) {
            println("IMD file not found in any expected location.")
            return
        }

        println("Found IMD at: ${imdFile.absolutePath}")
        val imdData = imdFile.readBytes()

        // Log first 16 bytes after CTRL-Z
        var p = 0
        while (p < imdData.size && imdData[p].toInt() != 0x1A) p++
        if (p < imdData.size) {
            p++
            val firstBytes = imdData.sliceArray(p until p + 16).joinToString(" ") {
                it.toInt().and(0xFF).toString(16).padStart(2, '0').uppercase()
            }
            println("IMD bytes after header: $firstBytes")
        }

        val parser = ImdParser(imdData)
        val tracks = parser.parseAllTracks()

        println("IMD Analysis of install.imd:")
        println("Total Tracks: ${tracks.size}")

        for (i in 0 until minOf(5, tracks.size)) {
            val t = tracks[i]
            println(
                "Track $i: Head=${t.head}, Sectors=${t.sectorCount}, Size=${t.sectorSize}, Map=${
                    t.sectorMap.joinToString(
                        ","
                    )
                }"
            )
        }

        val heads = tracks.map { it.head }.distinct()
        println("Heads found: $heads")

        var firstNonE5Track = -1
        for (i in tracks.indices) {
            val hasData = tracks[i].sectorData.any { sector -> sector.any { it != 0xE5.toByte() } }
            if (hasData) {
                firstNonE5Track = i
                break
            }
        }
        println("First track with non-E5 data: $firstNonE5Track")

        val rawImd = parser.toRawDsk()

        // Use a DPB that matches what we found (Directory at Track 3)
        val installDpb = DiskFormats.KAYPRO_II.copy(off = 3)
        val floppyImd = Floppy(installDpb)
        floppyImd.loadRawData(rawImd)

        println("\nFiles found in install.imd:")
        val imdFiles = floppyImd.listFiles().filter { it.isActive() }
        if (imdFiles.isEmpty()) {
            println("No files found (maybe not a standard CP/M layout or wrong DPB detected)")
        } else {
            imdFiles.forEach { println(it) }
        }

        // Compare with CPM22.DSK
        val dskPath = "composeApp/src/commonMain/composeResources/files/CPM22.DSK"
        val dskFile = File(dskPath)
        if (dskFile.exists()) {
            println("\nComparing with CPM22.DSK:")
            val dskData = dskFile.readBytes()
            val floppyDsk = Floppy.createFromData(dskData)
            val dskFiles = floppyDsk.listFiles().filter { it.isActive() }

            println("CPM22.DSK Files: ${dskFiles.distinctBy { "${it.filename}.${it.extension}" }.size}")

            val imdFileNames = imdFiles.map { "${it.filename}.${it.extension}" }.toSet()
            val dskFileNames = dskFiles.map { "${it.filename}.${it.extension}" }.toSet()

            val common = imdFileNames.intersect(dskFileNames)
            if (common.isNotEmpty()) {
                println("Common files: $common")
            } else {
                println("No common files found between install.imd and CPM22.DSK")
            }
        }
    }
}
