package com.aboveware.abovecpm

import androidx.compose.ui.input.key.KeyEvent
import java.io.InputStream

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

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
expect fun saveLocalFile(folder: String, name: String, data: ByteArray): Boolean
expect fun loadLocalFile(folder: String, name: String): ByteArray?
expect fun listLocalFiles(folder: String): List<String>
expect fun deleteLocalFile(folder: String, name: String): Boolean
expect fun getCurrentTimestamp(): String
expect fun openPdfPreview(name: String, data: ByteArray): Boolean
expect fun pickFolder(): String?
expect fun createFolder(parentPath: String, name: String): String?
expect fun writeFileToFolder(folderPath: String, name: String, data: ByteArray): Boolean
expect fun folderExists(parentPath: String, name: String): Boolean
expect fun fileExists(name: String): Boolean
expect fun deleteFolder(path: String): Boolean

expect val KeyEvent.char: Char?

expect fun playBell()
expect fun playKeyClick()

expect fun currentTimeMillis(): Long
