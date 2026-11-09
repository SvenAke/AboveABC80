package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Resource path for the bundled system disk matching the given computer model. */
fun systemDiskResourcePath(model: ComputerModel): String =
    if (model.isAbc800) "files/system800.dsk" else "files/system.dsk"

/** Name of the file providing verbose BASIC error messages for the given model. */
fun verboseErrorFileName(model: ComputerModel): String =
    if (model.isAbc800) "BASICINI.SYS" else "BASICERR.SYS"

/**
 * Returns the raw disk image used as template when creating a new system diskette:
 * the bundled system disk resource matching the active computer model.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadSystemDiskTemplate(): ByteArray =
    Res.readBytes(systemDiskResourcePath(TerminalManager.computerModel))

/**
 * Creates a blank diskette image and always adds the active model's verbose
 * error message file (BASICERR.SYS on ABC80, BASICINI.SYS on ABC800), copied
 * from the bundled system disk.
 */
@OptIn(ExperimentalUnsignedTypes::class)
suspend fun createBlankDisk(formatName: String, volumeName: String): ByteArray {
    val blank = Abc80FloppyLayout(formatName, 0).create(volumeName)
    val errorFile = verboseErrorFileName(TerminalManager.computerModel)
    return try {
        val records = Abc80FloppyLayout.readFile(loadSystemDiskTemplate(), errorFile)
        if (records != null && records.isNotEmpty()) {
            Abc80FloppyLayout.importFiles(blank, listOf(errorFile to records))
        } else {
            Abc80Log.wtf("$errorFile not found on the bundled system disk")
            blank
        }
    } catch (e: Exception) {
        Abc80Log.wtf("Could not add $errorFile to new disk: ${e.message}")
        blank
    }
}
