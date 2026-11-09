package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Resource path for the bundled system disk matching the given computer model. */
fun systemDiskResourcePath(model: ComputerModel): String =
    if (model.isAbc800) "files/system800.dsk" else "files/system.dsk"

/**
 * Returns the raw disk image used as template when creating a new system diskette:
 * the bundled system disk resource matching the active computer model.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadSystemDiskTemplate(): ByteArray =
    Res.readBytes(systemDiskResourcePath(TerminalManager.computerModel))
