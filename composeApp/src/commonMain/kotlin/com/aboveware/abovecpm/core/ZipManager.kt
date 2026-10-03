package com.aboveware.abovecpm.core

import com.aboveware.abovecpm.ArchivedFile

/**
 * Extracts a .zip file and returns the same packed format as ARK extraction.
 */
expect fun unzipFile(zipData: ByteArray): MutableList<ArchivedFile>