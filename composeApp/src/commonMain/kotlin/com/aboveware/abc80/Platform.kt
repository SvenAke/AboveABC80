package com.aboveware.abc80

import androidx.compose.ui.input.key.KeyEvent
import java.io.File
import java.io.InputStream

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getAppStorageDir(): File
expect fun getComputerModelStorageDir(folder: String): File

internal fun migrateLegacyDirectory(source: File, destination: File) {
    if (!source.exists()) return
    check(source.isDirectory) { "Legacy storage path is not a directory: ${source.absolutePath}" }
    check(destination.parentFile?.let { it.isDirectory || it.mkdirs() } != false) {
        "Could not create model storage directory ${destination.parentFile?.absolutePath}"
    }
    if (!destination.exists() && source.renameTo(destination)) return
    check(destination.isDirectory || destination.mkdirs()) {
        "Could not create model storage directory ${destination.absolutePath}"
    }

    val conflicts = mutableListOf<String>()
    fun moveContents(from: File, to: File) {
        val entries = checkNotNull(from.listFiles()) {
            "Could not read legacy storage directory ${from.absolutePath}"
        }
        entries.forEach { entry ->
            val target = File(to, entry.name)
            if (entry.isDirectory) {
                if (!target.exists() && entry.renameTo(target)) return@forEach
                check(target.isDirectory || target.mkdirs()) {
                    "Could not create migrated storage directory ${target.absolutePath}"
                }
                moveContents(entry, target)
                if (entry.list()?.isEmpty() == true) {
                    check(entry.delete()) { "Could not remove legacy directory ${entry.absolutePath}" }
                }
            } else if (target.exists()) {
                conflicts.add(entry.absolutePath)
            } else if (!entry.renameTo(target)) {
                entry.copyTo(target)
                check(entry.delete()) { "Could not remove legacy file ${entry.absolutePath}" }
            }
        }
    }

    moveContents(source, destination)
    check(conflicts.isEmpty()) {
        "Legacy storage files conflict with existing files: ${conflicts.joinToString()}"
    }
    check(source.delete()) { "Could not remove legacy directory ${source.absolutePath}" }
}

// Persisted settings
expect fun setPersistedString(key: String, value: String)
expect fun setPersistedMap(values: Map<String, String>)
expect fun getPersistedString(key: String, defaultValue: String = ""): String
expect fun clearPersistedSettings()

// Disk persistence
expect fun saveLocalDisk(name: String, data: ByteArray): Boolean
expect fun loadLocalDisk(name: String): ByteArray?
expect fun listLocalDisks(): List<String>
expect fun deleteLocalDisk(name: String): Boolean

// Generic file saving
expect fun getLocalFileDirectory(folder: String): File
expect fun saveLocalFile(folder: String, name: String, data: ByteArray): Boolean
expect fun loadLocalFile(folder: String, name: String): ByteArray?
expect fun listLocalFiles(folder: String): List<String>
expect fun deleteLocalFile(folder: String, name: String): Boolean
expect fun getCurrentTimestamp(): String
expect fun openPdfPreview(name: String, data: ByteArray): Boolean
expect fun openHtmlPreview(name: String, data: ByteArray): Boolean
expect fun pickFolder(): String?
expect fun createFolder(parentPath: String, name: String): String?
expect fun writeFileToFolder(folderPath: String, name: String, data: ByteArray): Boolean
expect fun folderExists(parentPath: String, name: String): Boolean
expect fun fileExists(name: String): Boolean
expect fun deleteFolder(path: String): Boolean

expect val KeyEvent.char: Char?

expect fun playBell()

expect fun currentTimeMillis(): Long
