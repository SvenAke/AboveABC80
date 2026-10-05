package com.aboveware.aboveabc80.ui

import aboveabc80.composeapp.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.Abc80Cassette
import com.aboveware.aboveabc80.Abc80CassetteStatus
import com.aboveware.aboveabc80.LocalFileDialog
import com.aboveware.aboveabc80.StorageFileDialog
import com.aboveware.aboveabc80.getCurrentTimestamp
import com.aboveware.aboveabc80.saveLocalFile
import org.jetbrains.compose.resources.stringResource

private const val SAVED_TAPES = "tapes"

@Composable
fun CassetteDialog(onDismiss: () -> Unit) {
    var files by remember { mutableStateOf(Abc80Cassette.list()) }
    var showImport by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }
    var showCloud by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val importFailed = stringResource(Res.string.cassette_import_failed)
    val exportedFormat = stringResource(Res.string.cassette_exported)
    val saveFailedFormat = stringResource(Res.string.cassette_save_failed)

    fun refresh(list: List<Abc80Cassette.FileInfo> = Abc80Cassette.list()) {
        files = list
    }

    fun importTape(name: String, data: ByteArray) {
        message = if (Abc80Cassette.import(data.inputStream(), name)) null else importFailed
        refresh()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cassette_title)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { Abc80Cassette.rewind(); refresh() }) {
                        Icon(Icons.Default.FastRewind, stringResource(Res.string.cassette_rewind))
                    }
                    IconButton(onClick = { refresh(Abc80Cassette.back()) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.cassette_back))
                    }
                    IconButton(onClick = { refresh(Abc80Cassette.forward()) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(Res.string.cassette_forward))
                    }
                    IconButton(onClick = { Abc80Cassette.format(); refresh() }) {
                        Icon(Icons.Default.Add, stringResource(Res.string.cassette_new))
                    }
                    IconButton(onClick = { Abc80Cassette.eject(); refresh() }) {
                        Icon(Icons.Default.Eject, stringResource(Res.string.cassette_eject))
                    }
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.Default.Upload, stringResource(Res.string.cassette_import))
                    }
                    IconButton(onClick = { showCloud = true }) {
                        Icon(Icons.Default.CloudDownload, stringResource(Res.string.cassette_cloud))
                    }
                    IconButton(onClick = { showSaved = true }) {
                        Icon(Icons.Default.FolderOpen, stringResource(Res.string.cassette_open_saved))
                    }
                    IconButton(
                        enabled = Abc80Cassette.anyCassette(),
                        onClick = {
                            val name = "tape-${getCurrentTimestamp()}.tape"
                            if (saveLocalFile(SAVED_TAPES, name, Abc80Cassette.exportBytes()))
                                message = exportedFormat.replace("%1\$s", name)
                        }
                    ) {
                        Icon(Icons.Default.Save, stringResource(Res.string.cassette_export))
                    }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Box(modifier = Modifier.sizeIn(minHeight = 200.dp, maxHeight = 400.dp).fillMaxWidth()) {
                    if (files.none { it.name.isNotBlank() }) {
                        Text(
                            stringResource(Res.string.cassette_empty),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn {
                            items(files) { info ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            info.displayName,
                                            fontWeight = if (info.current) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingContent = { Text(if (info.current) "▶" else "") },
                                    trailingContent = {
                                        if (info.length > 0) Text("${(info.length + 1023) / 1024} kB")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cassette_close)) } }
    )

    LocalFilePicker(
        show = showImport,
        onFileSelected = { name, data ->
            showImport = false
            importTape(name, data)
        },
        onDismiss = { showImport = false }
    )

    if (showCloud) {
        StorageFileDialog(
            rootPath = "abc80/cassettes/",
            onDismiss = { showCloud = false },
            onFilesSelected = { selected, _ ->
                showCloud = false
                message = selected.joinToString("\n") { (name, data) ->
                    val resultFormat = if (saveLocalFile(SAVED_TAPES, name, data)) {
                        exportedFormat
                    } else {
                        saveFailedFormat
                    }
                    resultFormat.replace("%1\$s", name)
                }
            }
        )
    }

    if (showSaved) {
        LocalFileDialog(
            folder = SAVED_TAPES,
            title = stringResource(Res.string.cassette_open_saved),
            onDismiss = { showSaved = false },
            onFileSelected = { name, data ->
                showSaved = false
                importTape(name, data)
            }
        )
    }
}
