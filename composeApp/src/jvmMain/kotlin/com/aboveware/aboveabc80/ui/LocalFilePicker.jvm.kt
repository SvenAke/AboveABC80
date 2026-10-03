package com.aboveware.aboveabc80.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.aboveware.aboveabc80.Abc80Log
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun LocalFilePicker(
    show: Boolean,
    onFileSelected: (String, ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        LaunchedEffect(Unit) {
            val fileDialog = FileDialog(null as Frame?, "Select File to Upload", FileDialog.LOAD)
            fileDialog.isVisible = true

            val directory = fileDialog.directory
            val fileName = fileDialog.file

            if (directory != null && fileName != null) {
                try {
                    val file = File(directory, fileName)
                    val bytes = file.readBytes()
                    onFileSelected(fileName, bytes)
                } catch (e: Exception) {
                    Abc80Log.wtf("Upload failed: ${e.message}")
                }
            }
            onDismiss()
        }
    }
}
