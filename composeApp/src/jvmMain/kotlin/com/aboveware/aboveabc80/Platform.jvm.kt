package com.aboveware.aboveabc80

import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.KeyEvent
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.prefs.Preferences
import javax.swing.JFileChooser

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    val disksDir =
        File(System.getProperty("user.home"), ".aboveabc80/disks").apply { if (!exists()) mkdirs() }
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getAppStorageDir(): File {
    val root = File(System.getProperty("user.home") ?: ".", ".aboveabc80")
    if (!root.exists()) root.mkdirs()
    return root
}

actual fun setPersistedString(key: String, value: String) {
    try {
        val prefs = Preferences.userRoot().node("com.aboveware.aboveabc80")
        prefs.put(key, value)
        prefs.flush()
    } catch (e: Exception) {
        // Log or handle error
    }
}

actual fun setPersistedMap(values: Map<String, String>) {
    try {
        val prefs = Preferences.userRoot().node("com.aboveware.aboveabc80")
        values.forEach { (key, value) ->
            prefs.put(key, value)
        }
        prefs.flush()
    } catch (e: Exception) {
        // Log or handle error
    }
}

actual fun getPersistedString(key: String, defaultValue: String): String {
    val prefs = Preferences.userRoot().node("com.aboveware.aboveabc80")
    return prefs.get(key, defaultValue)
}

actual fun clearPersistedSettings() {
    try {
        val prefs = Preferences.userRoot().node("com.aboveware.aboveabc80")
        prefs.clear()
        prefs.flush()
    } catch (e: Exception) {
        // Ignore persistence cleanup failures.
    }
}

actual fun saveLocalDisk(name: String, data: ByteArray): Boolean {
    return try {
        val dir = (getPlatform() as JVMPlatform).disksDir
        File(dir, name).writeBytes(data)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun loadLocalDisk(name: String): ByteArray? {
    return try {
        val dir = (getPlatform() as JVMPlatform).disksDir
        val file = File(dir, name)
        if (file.exists()) file.readBytes() else null
    } catch (e: Exception) {
        null
    }
}

actual fun listLocalDisks(): List<String> {
    val dir = (getPlatform() as JVMPlatform).disksDir
    return dir.listFiles { file -> file.isFile }?.map { it.name }?.sortedBy { it.lowercase() }
        ?: emptyList()
}

actual fun deleteLocalDisk(name: String): Boolean {
    val dir = (getPlatform() as JVMPlatform).disksDir
    return File(dir, name).delete()
}

actual fun saveLocalFile(folder: String, name: String, data: ByteArray): Boolean {
    return try {
        val root = (getPlatform() as JVMPlatform).disksDir.parentFile
        val dir = File(root, folder).apply { if (!exists()) mkdirs() }
        File(dir, name).writeBytes(data)
        true
    } catch (e: Exception) {
        false
    }

}

actual fun getLocalFileDirectory(folder: String): File =
    File((getPlatform() as JVMPlatform).disksDir.parentFile, folder)

actual fun loadLocalFile(folder: String, name: String): ByteArray? {
    return try {
        val root = (getPlatform() as JVMPlatform).disksDir.parentFile
        val file = File(File(root, folder), name)
        if (file.exists()) file.readBytes() else null
    } catch (e: Exception) {
        null
    }
}

actual fun listLocalFiles(folder: String): List<String> {
    val root = (getPlatform() as JVMPlatform).disksDir.parentFile
    val dir = File(root, folder)
    return dir.listFiles { file -> file.isFile }?.map { it.name } ?: emptyList()
}

actual fun deleteLocalFile(folder: String, name: String): Boolean {
    return try {
        val root = (getPlatform() as JVMPlatform).disksDir.parentFile
        val dir = File(root, folder)
        File(dir, name).delete()
    } catch (e: Exception) {
        false
    }
}

actual fun openPdfPreview(name: String, data: ByteArray): Boolean {
    return try {
        if (!Desktop.isDesktopSupported()) return false
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) return false

        val previewFile = File.createTempFile("aboveabc80-preview-", ".pdf")
        previewFile.writeBytes(data)
        previewFile.deleteOnExit()
        desktop.open(previewFile)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun pickFolder(): String? {
    val lastFolder = getPersistedString("last_export_folder", System.getProperty("user.home"))
    val chooser = JFileChooser(lastFolder).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select folder to export floppy files"
    }

    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val selected = chooser.selectedFile.absolutePath
        setPersistedString("last_export_folder", selected)
        selected
    } else {
        null
    }
}

actual fun createFolder(parentPath: String, name: String): String? {
    return try {
        val folder = File(parentPath, name)
        if (!folder.exists()) folder.mkdirs()
        folder.absolutePath
    } catch (_: Exception) {
        null
    }
}

actual fun writeFileToFolder(folderPath: String, name: String, data: ByteArray): Boolean {
    return try {
        File(folderPath, name).writeBytes(data)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun folderExists(parentPath: String, name: String): Boolean {
    return File(parentPath, name).let { it.exists() && it.isDirectory }
}

actual fun fileExists(name: String): Boolean {
    val dir = (getPlatform() as JVMPlatform).disksDir
    return File(dir, name).exists()
}

actual fun deleteFolder(path: String): Boolean {
    return try {
        File(path).deleteRecursively()
    } catch (e: Exception) {
        false
    }
}

actual fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return sdf.format(Date())
}

actual val KeyEvent.char: Char? get() = awtEventOrNull?.keyChar?.takeIf { it != '\uFFFF' }

private var lastBellTime = 0L

actual fun playBell() {
    Abc80Log.terminal("playBell")
    val now = System.currentTimeMillis()
    if (now - lastBellTime < 200) return // Cooldown 200ms
    lastBellTime = now

    try {
        java.awt.Toolkit.getDefaultToolkit().beep()
    } catch (e: Exception) {
        // Ignore
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
