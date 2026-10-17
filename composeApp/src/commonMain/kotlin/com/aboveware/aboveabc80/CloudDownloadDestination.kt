package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.terminal.TerminalManager

internal fun cloudDownloadFolder(path: String, configuredFolder: String?, fileName: String? = null): String? =
    if (fileName?.endsWith(".dsk", ignoreCase = true) == true) "disks"
    else configuredFolder ?: if (path.startsWith("abc80/cassettes/")) "tapes" else null

internal fun mountDownloadedCloudDisk(name: String, drive: Int) {
    val model = TerminalManager.computerModel
    val mountedName = checkNotNull(DiskController.instance.loadFromLocal(drive, name)) {
        "Kunde inte montera $name i ${model.displayName} på DR$drive:"
    }
    setPersistedString(DiskController.drivePreferenceKey(model, drive), "LOCAL:$mountedName")
}
