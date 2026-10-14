package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.*
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
import java.io.File
import org.jetbrains.compose.resources.stringResource

/**
 * An alert dialog composable that lists saved local files within a specified app storage folder.
 *
 * Displays local files in reverse-chronological order (by embedded timestamp), formatting
 * filenames into readable labels and creation dates. Users can select/open a file or delete unwanted
 * files from storage.
 *
 * @param folder The relative folder name in local app storage (e.g. "cassettes").
 * @param title Optional title text for the dialog. If `null`, defaults to the localized `local_files_title` string.
 * @param onDismiss Callback invoked when the user dismisses or closes the dialog.
 * @param onFileOpened Optional callback invoked when a file item is selected, receiving the filename and [File] handle.
 * @param onFileSelected Callback invoked when a file is selected if [onFileOpened] is `null`, receiving the filename and binary [ByteArray] contents.
 */
@Composable
fun LocalFileDialog(
    folder: String,
    title: String? = null,
    onDismiss: () -> Unit,
    onFileOpened: ((String, File) -> Unit)? = null,
    onFileSelected: (String, ByteArray) -> Unit
) {
    var files by remember {
        mutableStateOf(listLocalFiles(folder).sortedWith(compareByDescending { extractTimestamp(it) }))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title ?: stringResource(Res.string.local_files_title)) },
        text = {
            Box(modifier = Modifier.sizeIn(minHeight = 200.dp, maxHeight = 400.dp).fillMaxWidth()) {
                if (files.isEmpty()) {
                    Text(
                        stringResource(Res.string.local_files_empty, folder),
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
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(Res.string.local_files_delete)
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    if (onFileOpened != null) {
                                        onFileOpened(filename, getLocalFileDirectory(folder).resolve(filename))
                                    } else {
                                        val data = loadLocalFile(folder, filename)
                                        if (data != null) {
                                            onFileSelected(filename, data)
                                        }
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
                Text(stringResource(Res.string.ui_close))
            }
        }
    )
}

/**
 * Formats a filename with a timestamp suffix (e.g., `PREFIX_YYYYMMDD_HHMMSS.TAP`) into a human-readable pair.
 *
 * @param filename The raw filename string to format.
 * @return A [Pair] containing the prefix/main name as [Pair.first], and the formatted date string (`YYYY-MM-DD HH:MM:SS`) as [Pair.second] (or `null` if the timestamp pattern is not matched).
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
 * Extracts the timestamp portion (`YYYYMMDD_HHMMSS`) from a filename for sorting purposes.
 *
 * @param filename The raw filename string.
 * @return The extracted `YYYYMMDD_HHMMSS` timestamp string, or the full original [filename] if no pattern matches.
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
