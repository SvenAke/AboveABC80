package com.aboveware.aboveabc80.ui

import androidx.compose.runtime.Composable

@Composable
expect fun LocalFilePicker(
    show: Boolean,
    onFileSelected: (String, ByteArray) -> Unit,
    onDismiss: () -> Unit
)
