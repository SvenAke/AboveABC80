package com.aboveware.aboveabc80

internal fun cloudDownloadFolder(path: String, configuredFolder: String?): String? =
    configuredFolder ?: if (path.startsWith("abc80/cassettes/")) "tapes" else null
