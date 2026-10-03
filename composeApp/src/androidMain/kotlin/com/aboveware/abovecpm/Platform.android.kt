package com.aboveware.abovecpm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.input.key.KeyEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit
import androidx.core.content.FileProvider

class AndroidPlatform(context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    val disksDir = File(context.filesDir, "disks").apply { if (!exists()) mkdirs() }
}

val androidContext: Context? get() = _androidContext
private var _androidContext: Context? = null

fun setAndroidContext(context: Context) {
    _androidContext = context.applicationContext
}

actual fun getPlatform(): Platform = AndroidPlatform(_androidContext!!)

actual fun setPersistedString(key: String, value: String) {
    val prefs = androidContext?.getSharedPreferences("abovecpm_prefs", Context.MODE_PRIVATE)
    prefs?.edit()?.putString(key, value)?.apply()
}

actual fun setPersistedMap(values: Map<String, String>) {
    val prefs =
        androidContext?.getSharedPreferences("abovecpm_prefs", Context.MODE_PRIVATE) ?: return
    prefs.edit {
        values.forEach { (key, value) ->
            putString(key, value)
        }
    }
}

actual fun getPersistedString(key: String, defaultValue: String): String {
    val prefs = androidContext?.getSharedPreferences("abovecpm_prefs", Context.MODE_PRIVATE)
    return prefs?.getString(key, defaultValue) ?: defaultValue
}

actual fun clearPersistedSettings() {
    androidContext?.getSharedPreferences("abovecpm_prefs", Context.MODE_PRIVATE)
        ?.edit()
        ?.clear()
        ?.apply()
}

actual fun saveLocalDisk(name: String, data: ByteArray): Boolean {
    return try {
        val dir = (getPlatform() as AndroidPlatform).disksDir
        File(dir, name).writeBytes(data)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun loadLocalDisk(name: String): ByteArray? {
    return try {
        val dir = (getPlatform() as AndroidPlatform).disksDir
        val file = File(dir, name)
        if (file.exists()) file.readBytes() else null
    } catch (e: Exception) {
        null
    }
}

actual fun listLocalDisks(): List<String> {
    val dir = (getPlatform() as AndroidPlatform).disksDir
    return dir.listFiles { file -> file.isFile }?.map { it.name }?.sortedBy { it.lowercase() }
        ?: emptyList()
}

actual fun deleteLocalDisk(name: String): Boolean {
    val dir = (getPlatform() as AndroidPlatform).disksDir
    return File(dir, name).delete()
}

actual fun saveLocalFile(folder: String, name: String, data: ByteArray): Boolean {
    return try {
        val root = androidContext?.filesDir ?: return false
        val dir = File(root, folder).apply { if (!exists()) mkdirs() }
        File(dir, name).writeBytes(data)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun loadLocalFile(folder: String, name: String): ByteArray? {
    return try {
        val root = androidContext?.filesDir ?: return null
        val file = File(File(root, folder), name)
        if (file.exists()) file.readBytes() else null
    } catch (e: Exception) {
        null
    }
}

actual fun listLocalFiles(folder: String): List<String> {
    val root = androidContext?.filesDir ?: return emptyList()
    val dir = File(root, folder)
    return dir.listFiles { file -> file.isFile }?.map { it.name } ?: emptyList()
}

actual fun deleteLocalFile(folder: String, name: String): Boolean {
    return try {
        val root = androidContext?.filesDir ?: return false
        val dir = File(root, folder)
        File(dir, name).delete()
    } catch (e: Exception) {
        false
    }
}

actual fun openPdfPreview(name: String, data: ByteArray): Boolean {
    return try {
        val context = androidContext ?: return false
        val previewDir = File(context.cacheDir, "pdf-previews").apply { mkdirs() }
        val previewFile = File.createTempFile("abovecpm-preview-", ".pdf", previewDir)
        previewFile.writeBytes(data)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            previewFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun pickFolder(): String? = null
actual fun createFolder(parentPath: String, name: String): String? = null
actual fun writeFileToFolder(folderPath: String, name: String, data: ByteArray): Boolean = false
actual fun folderExists(parentPath: String, name: String): Boolean = false
actual fun fileExists(name: String): Boolean = false
actual fun deleteFolder(path: String): Boolean = false

actual fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return sdf.format(Date())
}

actual val KeyEvent.char: Char? get() = nativeKeyEvent.unicodeChar.toChar()

private val toneGenerator by lazy {
    try {
        android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        null
    }
}

private var lastBellTime = 0L

actual fun playBell() {
    ZXLog.terminal("playBell")
    val now = System.currentTimeMillis()
    if (now - lastBellTime < 200) return // Cooldown 200ms to prevent spam
    lastBellTime = now

    try {
        toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)
    } catch (e: Exception) {
        // Ignore
    }
}

actual fun playKeyClick() {
    ZXLog.terminal("playKeyClick")
    val audioManager =
        androidContext?.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
    audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
