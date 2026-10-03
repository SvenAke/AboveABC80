package com.aboveware.aboveabc80.core

import com.aboveware.aboveabc80.ArchivedFile

/**
 * Extracts a .zip file and returns the same packed format as ARK extraction.
 */
expect fun unzipFile(zipData: ByteArray): MutableList<ArchivedFile>