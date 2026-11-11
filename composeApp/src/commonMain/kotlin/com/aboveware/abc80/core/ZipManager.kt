package com.aboveware.abc80.core

import com.aboveware.abc80.ArchivedFile

/**
 * Extracts a .zip file and returns the same packed format as ARK extraction.
 */
expect fun unzipFile(zipData: ByteArray): MutableList<ArchivedFile>