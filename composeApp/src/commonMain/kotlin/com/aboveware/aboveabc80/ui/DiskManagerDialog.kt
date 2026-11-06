package com.aboveware.aboveabc80.ui

import aboveabc80.composeapp.generated.resources.*
import com.aboveware.aboveabc80.Abc80CloudDiskImport
import aboveabc80.composeapp.generated.resources.disk_list_contents
import aboveabc80.composeapp.generated.resources.disk_export_pc
import aboveabc80.composeapp.generated.resources.disk_import_cloud
import aboveabc80.composeapp.generated.resources.disk_delete
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.AnnotatedString
import com.aboveware.aboveabc80.ArchiveFileTextPreviewDialog
import com.aboveware.aboveabc80.formatSize
import com.aboveware.aboveabc80.isText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aboveware.aboveabc80.ArchiveFileDialog
import com.aboveware.aboveabc80.ArchivedFile
import com.aboveware.aboveabc80.NativeLib
import com.aboveware.aboveabc80.StorageFileDialog
import com.aboveware.aboveabc80.Abc80FloppyLayout
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.core.AbcDiskImage
import com.aboveware.aboveabc80.terminal.TerminalManager
import com.aboveware.aboveabc80.core.unzipFile
import com.aboveware.aboveabc80.createFolder
import com.aboveware.aboveabc80.deleteFolder
import com.aboveware.aboveabc80.deleteLocalDisk
import com.aboveware.aboveabc80.fileExists
import com.aboveware.aboveabc80.loadLocalDisk
import com.aboveware.aboveabc80.folderExists
import com.aboveware.aboveabc80.getPlatform
import com.aboveware.aboveabc80.listLocalDisks
import com.aboveware.aboveabc80.pickFolder
import com.aboveware.aboveabc80.saveLocalDisk
import com.aboveware.aboveabc80.setPersistedString
import com.aboveware.aboveabc80.writeFileToFolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import com.aboveware.aboveabc80.abc80ExecutableFormat
import com.aboveware.aboveabc80.Abc80ExecutableFormat


@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun DiskManagerDialog(
    onRunFile: (drive: Int, fileName: String) -> Unit = { _, _ -> },
    onLoadFile: (drive: Int, fileName: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val controller = DiskController.instance
    val scope = rememberCoroutineScope()
    val listContentsLabel = stringResource(Res.string.disk_list_contents)
    val exportPcLabel = stringResource(Res.string.disk_export_pc)
    val importCloudLabel = stringResource(Res.string.disk_import_cloud)
    val deleteLabel = stringResource(Res.string.disk_delete)
    val copyFailedMessage = stringResource(Res.string.disk_copy_failed)
    val modelName = TerminalManager.computerModel.displayName
    val mountFailedMessage = stringResource(Res.string.disk_mount_failed, modelName)
    val listingFailedMessage = stringResource(Res.string.disk_listing_failed, modelName)
    var selectedDriveIndex by remember { mutableStateOf<Int?>(null) }
    var showNewDiskDialog by remember { mutableStateOf(false) }
    var showCloudImportDialog by remember { mutableStateOf(false) }
    var targetImportDriveIndex by remember { mutableStateOf<Int?>(null) }
    var clearCloudImportTarget by remember { mutableStateOf(false) }
    var cloudImportTargetCleared by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var archiveName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var arkResult: List<ArchivedFile> by remember { mutableStateOf(emptyList()) }
    var localDisks by remember {
        mutableStateOf(listLocalDisks().filter { it.lowercase().endsWith(".dsk") })
    }

    var listing by remember { mutableStateOf<Pair<String, List<Pair<String, Int>>?>?>(null) }
    var listingDrive by remember { mutableStateOf(0) }
    var listingImage by remember { mutableStateOf<ByteArray?>(null) }
    var textPreview by remember { mutableStateOf<Pair<String, String>?>(null) }
    var runnableFiles by remember { mutableStateOf(emptyMap<String, Abc80ExecutableFormat>()) }

    data class ExportPending(val parentDir: String, val floppyName: String, val floppy: AbcDiskImage)
    var exportPending by remember { mutableStateOf<ExportPending?>(null) }

    fun performExport(parentDir: String, floppyName: String, floppy: AbcDiskImage, deleteFirst: Boolean) {
        try {
            val fullPath = if (deleteFirst) {
                val path = "$parentDir/$floppyName"
                deleteFolder(path)
                createFolder(parentDir, floppyName)
            } else {
                createFolder(parentDir, floppyName)
            }

            if (fullPath != null) {
                val files = floppy.listFiles()
                val uniqueFiles = mutableSetOf<String>()
                files.forEach { entry ->
                    val fullName = if (entry.extension.isBlank()) entry.filename else "${entry.filename}.${entry.extension}"
                    if (uniqueFiles.add(fullName)) {
                        val data = floppy.readFile(entry.filename, entry.extension)
                        checkNotNull(data) { "Kunde inte läsa $fullName från disketten" }
                        check(writeFileToFolder(fullPath, fullName, data)) { "Kunde inte exportera $fullName" }
                    }
                }
                Abc80Log.terminal("Exported ${uniqueFiles.size} files to $fullPath")
            } else {
                errorMessage = "Kunde inte skapa exportmappen $floppyName"
                Abc80Log.wtf(requireNotNull(errorMessage))
            }
        } catch (e: IllegalArgumentException) {
            errorMessage = e.message ?: "Disketten kunde inte exporteras"
            Abc80Log.wtf("ABC80-export: $errorMessage")
        } catch (e: IllegalStateException) {
            errorMessage = e.message ?: "Disketten kunde inte exporteras"
            Abc80Log.wtf("ABC80-export: $errorMessage")
        }
    }

    // Refresh list when dialog opens
    LaunchedEffect(Unit) {
        localDisks = listLocalDisks().filter { it.lowercase().endsWith(".dsk") }
    }

    listing?.let { (diskName, files) ->
        val diskFreeLabel = stringResource(Res.string.disk_free)
        val titleText = remember(diskName, listingImage, diskFreeLabel) {
            val baseName = diskName.substringBeforeLast(".")
            val image = listingImage
            if (image != null) {
                val type = Abc80FloppyLayout.diskType(image)
                val freeSpace = Abc80FloppyLayout.freeSpace(image)
                buildAnnotatedString {
                    append(baseName)
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    ) {
                        append(" ($type, ${formatSize(freeSpace)} $diskFreeLabel)")
                    }
                }
            } else {
                AnnotatedString(baseName)
            }
        }
        AlertDialog(
            onDismissRequest = { listing = null },
            title = { Text(titleText) },
            text = {
                when {
                    files == null -> Text(stringResource(Res.string.disk_unrecognized_image))
                    files.isEmpty() -> Text(stringResource(Res.string.disk_no_files))
                    else -> LazyColumn(modifier = Modifier.sizeIn(maxHeight = 400.dp)) {
                        items(files) { (name, size) ->
                            val runnable = name in runnableFiles
                            val isPreviewable = runnableFiles[name] == Abc80ExecutableFormat.BAS || name.isText()
                            ListItem(
                                headlineContent = { Text(name) },
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (runnable) {
                                            if (runnableFiles[name] != Abc80ExecutableFormat.ABS) CassetteIconButton(
                                                Icons.Default.Download,
                                                stringResource(Res.string.cassette_load),
                                                true
                                            ) {
                                                listing = null
                                                onLoadFile(listingDrive, name)
                                            }
                                            CassetteIconButton(
                                                Icons.Default.PlayArrow,
                                                stringResource(Res.string.cassette_run),
                                                true
                                            ) {
                                                listing = null
                                                onRunFile(listingDrive, name)
                                            }
                                        }
                                        if (isPreviewable) {
                                            CassetteIconButton(
                                                Icons.Default.Visibility,
                                                stringResource(Res.string.cassette_preview),
                                                true
                                            ) {
                                                val data = listingImage?.let { Abc80FloppyLayout.readFile(it, name) }
                                                if (data != null) {
                                                    val text = decodeAbc80FloppyText(data)
                                                    textPreview = name to text
                                                }
                                            }
                                        }
                                        Text(
                                            "${(size + 1023) / 1024} kB",
                                            modifier = Modifier.padding(start = 4.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { listing = null }) {
                    Text(stringResource(Res.string.ui_close))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.disk_manager_title)) },
        text = {
            Box(modifier = Modifier.sizeIn(minHeight = 300.dp, maxHeight = 500.dp).fillMaxWidth()) {
                if (selectedDriveIndex == null) {
                    LazyColumn {
                        items(DiskController.instance.maxDrives) { index ->
                            val driveLetter = "DR$index"
                            val mounted = controller.mountedDisks[index]

                            ListItem(
                                headlineContent = { Text("$driveLetter:") },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mounted?.name?.substringBeforeLast(".")
                                                ?: stringResource(Res.string.storage_no_disk),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mounted != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        if (mounted != null) {
                                            Row {
                                                TooltipIconButton(label = listContentsLabel, onClick = {
                                                    scope.launch {
                                                        val image = when (mounted.source) {
                                                            DiskController.Source.LOCAL -> loadLocalDisk(mounted.name)
                                                            DiskController.Source.RESOURCE -> try {
                                                                Res.readBytes("files/${mounted.name}")
                                                            } catch (e: Exception) {
                                                                null
                                                            }
                                                            DiskController.Source.DRIVE ->
                                                                controller.getFloppy(index)?.getRawData()
                                                        }
                                                        try {
                                                            listingDrive = index
                                                            listingImage = image
                                                            val files = image?.let { Abc80FloppyLayout.list(it) }
                                                            runnableFiles = files.orEmpty().mapNotNull { (name, _) ->
                                                                val data = checkNotNull(Abc80FloppyLayout.readFile(checkNotNull(image), name)) {
                                                                    "Could not read $name from ${mounted.name}"
                                                                }
                                                                abc80ExecutableFormat(name, data, 253)?.let { name to it }
                                                            }.toMap()
                                                            listing = mounted.name to files
                                                        } catch (e: IllegalArgumentException) {
                                                            Abc80Log.wtf("Invalid $modelName directory in ${mounted.name}: ${e.message}")
                                                            errorMessage = "${mounted.name}: $listingFailedMessage"
                                                        } catch (e: IllegalStateException) {
                                                            Abc80Log.wtf("Could not read $modelName files in ${mounted.name}: ${e.message}")
                                                            errorMessage = "${mounted.name}: $listingFailedMessage"
                                                        }
                                                    }
                                                }) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.List,
                                                        contentDescription = listContentsLabel
                                                    )
                                                }
                                                if (getPlatform().name.contains("Java")) {
                                                    TooltipIconButton(label = exportPcLabel, onClick = {
                                                        val floppy = controller.getFloppy(index)
                                                        if (floppy != null) {
                                                            val parentDir = pickFolder()
                                                            if (parentDir != null) {
                                                                val floppyName = mounted.name.substringBeforeLast(".")
                                                                if (folderExists(parentDir, floppyName)) {
                                                                    exportPending = ExportPending(parentDir, floppyName, floppy)
                                                                } else {
                                                                    performExport(parentDir, floppyName, floppy, false)
                                                                }
                                                            }
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Upload,
                                                            contentDescription = exportPcLabel
                                                        )
                                                    }
                                                }

                                                TooltipIconButton(label = importCloudLabel, onClick = {
                                                    targetImportDriveIndex = index
                                                    controller.currentDriveIndex = index
                                                    showCloudImportDialog = true
                                                }) {
                                                    Icon(
                                                        Icons.Default.CloudDownload,
                                                        contentDescription = importCloudLabel
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Storage, contentDescription = null)
                                },
                                modifier = Modifier.clickable { selectedDriveIndex = index }
                            )
                        }
                    }
                } else {
                    val driveIndex = selectedDriveIndex!!
                    val driveLetter = "DR$driveIndex"

                    Column {
                        Text(
                            text = stringResource(Res.string.disk_select_for_drive, driveLetter),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            stringResource(Res.string.disk_new),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier.clickable { showNewDiskDialog = true }
                                )
                            }

                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

                            items(localDisks) { diskName ->
                                val mountedOnIndex =
                                    controller.mountedDisks.entries.find { it.value.name == diskName }?.key
                                val isMountedElsewhere =
                                    mountedOnIndex != null && mountedOnIndex != driveIndex

                                ListItem(
                                    headlineContent = {
                                        val ext = diskName.substringAfterLast(".").uppercase()
                                        Text(
                                            text = diskName.substringBeforeLast(".") + if (ext != "DSK") " ($ext)" else "",
                                            color = if (isMountedElsewhere) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    supportingContent = if (isMountedElsewhere) {
                                        {
                                            Text(
                                                stringResource(
                                                    Res.string.disk_mounted_on,
                                                    ('A' + mountedOnIndex).toString()
                                                ),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else null,
                                    trailingContent = {
                                        TooltipIconButton(
                                            label = deleteLabel,
                                            onClick = {
                                                deleteLocalDisk(diskName)
                                                localDisks = listLocalDisks().filter { it.lowercase().endsWith(".dsk") }
                                                for (i in 0 until DiskController.instance.maxDrives) {
                                                    if (controller.mountedDisks[i]?.name == diskName) {
                                                        controller.clearDrive(i)
                                                        setPersistedString(
                                                            DiskController.drivePreferenceKey(TerminalManager.computerModel, i),
                                                            DiskController.UNMOUNTED_PREFERENCE
                                                        )
                                                    }
                                                }
                                            },
                                            enabled = !isMountedElsewhere
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = deleteLabel,
                                                tint = if (isMountedElsewhere) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    modifier = if (isMountedElsewhere) Modifier else Modifier.clickable {
                                        val actualName =
                                            controller.loadFromLocal(driveIndex, diskName)
                                        if (actualName != null) {
                                            setPersistedString(
                                                DiskController.drivePreferenceKey(
                                                    TerminalManager.computerModel,
                                                    driveIndex
                                                ),
                                                "LOCAL:$actualName"
                                            )
                                            if (actualName != diskName) {
                                                localDisks = listLocalDisks().filter { it.lowercase().endsWith(".dsk") }
                                            }
                                            selectedDriveIndex = null
                                        } else {
                                            errorMessage = "$diskName: $mountFailedMessage"
                                        }
                                    }
                                )
                            }

                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                            item {
                                ListItem(
                                    headlineContent = {
                                        Text(stringResource(Res.string.disk_unmount))
                                    },
                                    modifier = Modifier.clickable {
                                        controller.clearDrive(driveIndex)
                                        setPersistedString(
                                            DiskController.drivePreferenceKey(
                                                TerminalManager.computerModel,
                                                driveIndex
                                            ),
                                            DiskController.UNMOUNTED_PREFERENCE
                                        )
                                        selectedDriveIndex = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedDriveIndex != null) {
                    TextButton(onClick = { selectedDriveIndex = null }) {
                        Text(stringResource(Res.string.ui_back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.ui_close))
                }
            }
        }
    )

    if (showNewDiskDialog) {
        NewDiskDialog(
            onDismiss = { showNewDiskDialog = false },
            onCreated = { newName ->
                localDisks = listLocalDisks().filter { it.lowercase().endsWith(".dsk") }
                showNewDiskDialog = false
                // Auto-mount if a drive was selected
                selectedDriveIndex?.let { driveIndex ->
                    val actualName = controller.loadFromLocal(driveIndex, newName)
                    if (actualName != null) {
                        setPersistedString(
                            DiskController.drivePreferenceKey(
                                TerminalManager.computerModel,
                                driveIndex
                            ),
                            "LOCAL:$actualName"
                        )
                    }
                    selectedDriveIndex = null
                }
            }
        )
    }
    fun saveCloudFiles(driveIndex: Int, files: List<Pair<String, ByteArray>>) {
        if (files.isEmpty()) return
        val clear = clearCloudImportTarget && !cloudImportTargetCleared
        if (clear) cloudImportTargetCleared = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Abc80CloudDiskImport.save(driveIndex, files, clear)
                }
                cloudImportTargetCleared = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (clear) cloudImportTargetCleared = false
                Abc80Log.wtf("Cloud import to DR$driveIndex failed: ${e.message}")
                errorMessage = e.message ?: copyFailedMessage
            }
        }
    }

    if (showArchiveDialog) {
        ArchiveFileDialog(
            archiveName = archiveName,
            files = arkResult,
            targetDriveIndex = targetImportDriveIndex,
            clearBeforeDownload = clearCloudImportTarget,
            onClearBeforeDownloadChange = { clearCloudImportTarget = it },
            onResult = { files ->
                if (files != null) {
                    targetImportDriveIndex?.let { driveIndex ->
                        saveCloudFiles(driveIndex, files.map { it.name to it.content })
                    }
                    targetImportDriveIndex = null
                } else {
                    showCloudImportDialog = true
                }
                showArchiveDialog = false
            }
        )
    }

    if (showCloudImportDialog) {
        StorageFileDialog(
            targetDriveIndex = targetImportDriveIndex,
            onDismiss = {
                localDisks = listLocalDisks().filter { it.endsWith(".dsk", ignoreCase = true) }
                showCloudImportDialog = false
            },
            onFilesSelected = { selectedFiles, clearBeforeDownload ->
                clearCloudImportTarget = clearBeforeDownload
                cloudImportTargetCleared = false
                targetImportDriveIndex?.let { driveIndex ->
                    val floppy = controller.getFloppy(driveIndex)
                    if (floppy != null) {
                        // If multiple files are selected, we process them one by one.
                        // However, if one of them is an archive, it will trigger showArchiveDialog.
                        // For simplicity, we handle the first archive found or inject all normal files.
                        
                        var firstArchive: Pair<String, ByteArray>? = null
                        val normalFiles = mutableListOf<Pair<String, ByteArray>>()
                        
                        for (file in selectedFiles) {
                            val fileExt = file.first.substringAfterLast(".").lowercase()
                            if (fileExt == "ark" || fileExt == "arc" || fileExt == "zip" || fileExt == "lbr") {
                                if (firstArchive == null) firstArchive = file
                            } else {
                                normalFiles.add(file)
                            }
                        }
                        
                        if (firstArchive != null) {
                            val (fullName, data) = firstArchive
                            val fileExt = fullName.substringAfterLast(".").lowercase()
                            archiveName = fullName.substringAfterLast("/")
                            when (fileExt) {
                                "ark", "arc" -> {
                                    arkResult = NativeLib.getObject().extractArkArchive(data)
                                    showArchiveDialog = true
                                }
                                "zip" -> {
                                    arkResult = unzipFile(data)
                                    showArchiveDialog = true
                                }
                                "lbr" -> {
                                    arkResult = NativeLib.getObject().extractLbrArchive(data)
                                    showArchiveDialog = true
                                }
                            }
                            // Also inject normal files if any were selected along with the archive
                            if (normalFiles.isNotEmpty()) {
                                saveCloudFiles(driveIndex, normalFiles)
                            }
                        } else {
                            // Only normal files
                            saveCloudFiles(driveIndex, selectedFiles)
                            targetImportDriveIndex = null
                        }
                    }
                }
                showCloudImportDialog = false
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = {
                Text(
                    stringResource(Res.string.ui_error),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(Res.string.ui_error),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(message)
                }
            },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(Res.string.ui_ok))
                }
            }
        )
    }

    exportPending?.let { pending ->
        AlertDialog(
            onDismissRequest = { exportPending = null },
            title = { Text(stringResource(Res.string.disk_export_exists_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.disk_export_exists_message,
                        pending.floppyName
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        performExport(pending.parentDir, pending.floppyName, pending.floppy, deleteFirst = true)
                        exportPending = null
                    }
                ) {
                    Text(stringResource(Res.string.disk_export_delete_old))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        performExport(pending.parentDir, pending.floppyName, pending.floppy, deleteFirst = false)
                        exportPending = null
                    }
                ) {
                    Text(stringResource(Res.string.disk_export_merge))
                }
            }
        )
    }

    textPreview?.let { (fileName, content) ->
        ArchiveFileTextPreviewDialog(
            fileName = fileName,
            content = AnnotatedString(content),
            onDismissRequest = { textPreview = null }
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun NewDiskDialog(
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    val templates = listOf(
        "fd2" to stringResource(Res.string.disk_format_sssd),
        "abc830" to stringResource(Res.string.disk_format_ssdd),
        "fd4d" to stringResource(Res.string.disk_format_dsdd_320),
        "abc832" to stringResource(Res.string.disk_format_dsdd_640)
    )
    val storedSystemVolumeName by androidx.compose.runtime.produceState(initialValue = "SYSTEM-DISKETT ABC-80 Vers. 2.1.") {
        try {
            val raw = Res.readBytes("files/system.dsk")
            value = Abc80FloppyLayout.readVolumeName(raw)
        } catch (_: Exception) {
        }
    }

    var selectedTemplate by remember { mutableStateOf("abc830") }
    var systemDisk by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var showOverwriteConfirmation by remember { mutableStateOf<String?>(null) }

    var pendingCreateTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    androidx.compose.runtime.LaunchedEffect(systemDisk, storedSystemVolumeName) {
        if (systemDisk) {
            newName = storedSystemVolumeName
        } else if (newName == storedSystemVolumeName) {
            newName = ""
        }
    }

    fun createDisk(hostFileName: String, volumeName: String) {
        scope.launch {
            try {
                val data = if (systemDisk) {
                    val raw = Res.readBytes("files/system.dsk")
                    val targetVolName = volumeName.ifBlank { storedSystemVolumeName }
                    val modified = raw.copyOf()
                    Abc80FloppyLayout.writeVolumeName(modified, targetVolName)
                    modified
                } else {
                    val volName = volumeName.ifEmpty { "SYSTEM" }
                    Abc80FloppyLayout(selectedTemplate, 0).create(volName)
                }
                if (saveLocalDisk(hostFileName, data)) {
                    onCreated(hostFileName)
                }
            } catch (e: Exception) {
                Abc80Log.wtf("Creation failed: ${e.message}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.disk_new_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.storage_disk_type), style = MaterialTheme.typography.labelSmall)
                listOf(
                    false to stringResource(Res.string.storage_empty_disk),
                    true to stringResource(Res.string.storage_system_disk)
                ).forEach { (isSystem, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { systemDisk = isSystem }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = systemDisk == isSystem,
                            onClick = { systemDisk = isSystem }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DiskFormatDropdown(
                    formats = templates,
                    selected = if (systemDisk) "abc830" else selectedTemplate,
                    onSelected = {
                        selectedTemplate = it
                        systemDisk = false
                    },
                    enabled = !systemDisk,
                    label = stringResource(Res.string.storage_format),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = newName,
                    onValueChange = { input ->
                        newName = input.take(32)
                    },
                    label = { Text(stringResource(Res.string.disk_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val volName = newName.trim()
                    val defaultVolName = if (volName.isNotEmpty()) volName else if (systemDisk) "SYSTEM-DISKETT" else "SYSTEM"
                    val finalName = Abc80FloppyLayout.generateUniqueDiskFileName(defaultVolName, listLocalDisks())
                    if (fileExists(finalName)) {
                        pendingCreateTarget = finalName to volName
                        showOverwriteConfirmation = finalName
                    } else {
                        createDisk(finalName, volName)
                    }
                }
            ) {
                Text(stringResource(Res.string.ui_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.ui_cancel))
            }
        }
    )

    showOverwriteConfirmation?.let { name ->
        AlertDialog(
            onDismissRequest = { showOverwriteConfirmation = null },
            title = { Text(stringResource(Res.string.disk_exists_title)) },
            text = { Text(stringResource(Res.string.disk_exists_message, name)) },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pendingCreateTarget
                        if (target != null) {
                            createDisk(target.first, target.second)
                        } else {
                            createDisk(name, newName.trim())
                        }
                        showOverwriteConfirmation = null
                    }
                ) {
                    // Overwrite is basically "Replace" for a file
                    Text(stringResource(Res.string.ui_overwrite))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirmation = null }) {
                    Text(stringResource(Res.string.ui_cancel))
                }
            }
        )
    }
}

private fun decodeAbc80FloppyText(data: ByteArray, recordSize: Int = 253): String {
    val text = StringBuilder()
    for (start in data.indices step recordSize) {
        for (index in start until minOf(start + recordSize, data.size)) {
            val code = data[index].toInt() and 255
            if (code == 0 || code == 3) break
            text.append(when (code) {
                0x40 -> 'É'
                0x5b -> 'Ä'
                0x5c -> 'Ö'
                0x5d -> 'Å'
                0x5e -> 'Ü'
                0x60 -> 'é'
                0x7b -> 'ä'
                0x7c -> 'ö'
                0x7d -> 'å'
                0x7e -> 'ü'
                0x24 -> '¤'
                else -> code.toChar()
            })
        }
    }
    return text.toString().replace("\r\n", "\n").replace('\r', '\n')
}
