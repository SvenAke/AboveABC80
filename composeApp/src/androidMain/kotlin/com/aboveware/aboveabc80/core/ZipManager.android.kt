package com.aboveware.aboveabc80.core
import com.aboveware.aboveabc80.ArchivedFile
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

actual fun unzipFile(zipData: ByteArray): MutableList<ArchivedFile> {
    val files = mutableListOf<ArchivedFile>()

    ZipInputStream(ByteArrayInputStream(zipData).buffered()).use { zipInputStream ->
        while (true) {
            val entry = zipInputStream.nextEntry ?: break
            if (!entry.isDirectory) {
                files += ArchivedFile(
                    entry.name.substringAfterLast('/'),
                    zipInputStream.readBytes()
                )
            }
        }
    }
    return files
}
