package com.aboveware.abc80

import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.TerminalManager

internal fun cloudDownloadFolder(path: String, configuredFolder: String?, fileName: String? = null): String? =
    if (fileName != null && isDiskImageName(fileName)) "disks"
    else configuredFolder ?: if (path.startsWith("abc/abc80/cassettes/") || path.startsWith("abc/abc800/cassettes/") || path.startsWith("abc80/cassettes/") || path.startsWith("abc800/cassettes/")) "tapes" else null

internal fun mountDownloadedCloudDisk(name: String, drive: Int) {
    val model = TerminalManager.computerModel
    val mountedName = checkNotNull(DiskController.instance.loadFromLocal(drive, name)) {
        "Kunde inte montera $name i ${model.displayName} på DR$drive:"
    }
    setPersistedString(DiskController.drivePreferenceKey(model, drive), "LOCAL:$mountedName")
}
