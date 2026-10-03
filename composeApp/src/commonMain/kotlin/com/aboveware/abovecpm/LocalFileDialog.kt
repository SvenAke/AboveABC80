package com.aboveware.abovecpm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocalFileDialog(
    folder: String,
    title: String = "Local Files",
    onDismiss: () -> Unit,
    onFileSelected: (String, ByteArray) -> Unit
) {
    var files by remember {
        mutableStateOf(listLocalFiles(folder).sortedWith(compareByDescending { extractTimestamp(it) }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.sizeIn(minHeight = 200.dp, maxHeight = 400.dp).fillMaxWidth()) {
                if (files.isEmpty()) {
                    Text(
                        "No files found in '$folder'.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn {
                        items(files, key = { it }) { filename ->
                            val readableInfo = formatFilename(filename)
                            ListItem(
                                headlineContent = { Text(readableInfo.first) },
                                supportingContent = readableInfo.second?.let { timestamp ->
                                    {
                                        Text(
                                            timestamp
                                        )
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        if (deleteLocalFile(folder, filename)) {
                                            files = listLocalFiles(folder).sortedWith(
                                                compareByDescending { extractTimestamp(it) })
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                },
                                modifier = Modifier.clickable {
                                    val data = loadLocalFile(folder, filename)
                                    if (data != null) {
                                        onFileSelected(filename, data)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Parses filenames like PREFIX_YYYYMMDD_HHMMSS.TAP into a readable pair.
 * Returns Pair(Prefix/MainName, ReadableTimestamp?)
 */
private fun formatFilename(filename: String): Pair<String, String?> {
    val nameWithoutExt = filename.removeSuffix(".TAP").removeSuffix(".tap")
    val parts = nameWithoutExt.split("_")

    if (parts.size >= 3) {
        val datePart = parts[parts.size - 2]
        val timePart = parts[parts.size - 1]

        if (datePart.length == 8 && timePart.length == 6) {
            val prefix = parts.dropLast(2).joinToString("_")
            val year = datePart.substring(0, 4)
            val month = datePart.substring(4, 6)
            val day = datePart.substring(6, 8)
            val hour = timePart.substring(0, 2)
            val min = timePart.substring(2, 4)
            val sec = timePart.substring(4, 6)

            return Pair(prefix, "$year-$month-$day $hour:$min:$sec")
        }
    }

    return Pair(filename, null)
}

/**
 * Extracts the timestamp part of filenames like PREFIX_YYYYMMDD_HHMMSS.TAP for sorting.
 * Returns the timestamp string or the whole filename if pattern not found.
 */
private fun extractTimestamp(filename: String): String {
    val nameWithoutExt = filename.removeSuffix(".TAP").removeSuffix(".tap")
    val parts = nameWithoutExt.split("_")
    if (parts.size >= 2) {
        val datePart = parts[parts.size - 2]
        val timePart = parts[parts.size - 1]
        if (datePart.length == 8 && timePart.length == 6) {
            return "${datePart}_$timePart"
        }
    }
    return filename
}
