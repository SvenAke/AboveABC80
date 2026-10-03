package com.aboveware.aboveabc80.ui

import aboveabc80.composeapp.generated.resources.Res
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.ArchiveFileDialog
import com.aboveware.aboveabc80.ArchivedFile
import com.aboveware.aboveabc80.NativeLib
import com.aboveware.aboveabc80.StorageFileDialog
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.core.Floppy
import com.aboveware.aboveabc80.core.unzipFile
import com.aboveware.aboveabc80.createFolder
import com.aboveware.aboveabc80.deleteFolder
import com.aboveware.aboveabc80.deleteLocalDisk
import com.aboveware.aboveabc80.fileExists
import com.aboveware.aboveabc80.folderExists
import com.aboveware.aboveabc80.getPlatform
import com.aboveware.aboveabc80.listLocalDisks
import com.aboveware.aboveabc80.pickFolder
import com.aboveware.aboveabc80.saveLocalDisk
import com.aboveware.aboveabc80.setPersistedString
import com.aboveware.aboveabc80.splitFilename
import com.aboveware.aboveabc80.writeFileToFolder
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi


@Composable
fun DiskManagerDialog(
    onDismiss: () -> Unit
) {
    val controller = DiskController.instance
    val scope = rememberCoroutineScope()
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
        mutableStateOf(listLocalDisks().filter {
            it.lowercase().endsWith(".dsk") || it.lowercase().endsWith(".imd")
        })
    }

    data class ExportPending(val parentDir: String, val floppyName: String, val floppy: Floppy)
    var exportPending by remember { mutableStateOf<ExportPending?>(null) }

    fun performExport(parentDir: String, floppyName: String, floppy: Floppy, deleteFirst: Boolean) {
        val fullPath = if (deleteFirst) {
            val path = "$parentDir/$floppyName"
            deleteFolder(path)
            createFolder(parentDir, floppyName)
        } else {
            createFolder(parentDir, floppyName)
        }

        if (fullPath != null) {
            val files = floppy.listFiles().filter { it.isActive() }
            val uniqueFiles = mutableSetOf<String>()
            files.forEach { entry ->
                val fullName = if (entry.extension.isBlank()) entry.filename else "${entry.filename}.${entry.extension}"
                if (uniqueFiles.add(fullName)) {
                    val data = floppy.readFile(entry.filename, entry.extension)
                    if (data != null) {
                        writeFileToFolder(fullPath, fullName, data)
                    }
                }
            }
            Abc80Log.terminal("Exported ${uniqueFiles.size} files to $fullPath")
        }
    }

    // Refresh list when dialog opens
    LaunchedEffect(Unit) {
        localDisks = listLocalDisks().filter {
            it.lowercase().endsWith(".dsk") || it.lowercase().endsWith(".imd")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disk Manager") },
        text = {
            Box(modifier = Modifier.sizeIn(minHeight = 300.dp, maxHeight = 500.dp).fillMaxWidth()) {
                if (selectedDriveIndex == null) {
                    LazyColumn {
                        items(DiskController.MAX_DRIVES) { index ->
                            val driveLetter = ('A' + index).toString()
                            val mounted = controller.mountedDisks[index]

                            ListItem(
                                headlineContent = { Text("$driveLetter:") },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mounted?.name?.substringBeforeLast(".")
                                                ?: "None",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mounted != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        if (mounted != null) {
                                            Row {
                                                if (getPlatform().name.contains("Java")) {
                                                    IconButton(onClick = {
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
                                                            Icons.Default.Download,
                                                            contentDescription = "Export to PC"
                                                        )
                                                    }
                                                }

                                                IconButton(onClick = {
                                                    targetImportDriveIndex = index
                                                    showCloudImportDialog = true
                                                }) {
                                                    Icon(
                                                        Icons.Default.CloudDownload,
                                                        contentDescription = "Import from Cloud"
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
                    val driveLetter = ('A' + driveIndex).toString()

                    Column {
                        Text(
                            text = "Select Disk for Drive $driveLetter",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            "New disk...",
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
                                                "Mounted on ${('A' + mountedOnIndex)}",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else null,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                deleteLocalDisk(diskName)
                                                localDisks = listLocalDisks().filter {
                                                    it.lowercase()
                                                        .endsWith(".dsk") || it.lowercase()
                                                        .endsWith(".imd")
                                                }
                                                for (i in 0 until DiskController.MAX_DRIVES) {
                                                    if (controller.mountedDisks[i]?.name == diskName) {
                                                        controller.clearDrive(i)
                                                        setPersistedString("drive_$i", "")
                                                    }
                                                }
                                            },
                                            enabled = !isMountedElsewhere
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = if (isMountedElsewhere) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    modifier = if (isMountedElsewhere) Modifier else Modifier.clickable {
                                        val actualName =
                                            controller.loadFromLocal(driveIndex, diskName)
                                        if (actualName != null) {
                                            setPersistedString(
                                                "drive_$driveIndex",
                                                "LOCAL:$actualName"
                                            )
                                            if (actualName != diskName) {
                                                localDisks = listLocalDisks().filter {
                                                    it.lowercase()
                                                        .endsWith(".dsk") || it.lowercase()
                                                        .endsWith(".imd")
                                                }
                                            }
                                            selectedDriveIndex = null
                                        }
                                    }
                                )
                            }

                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                            item {
                                ListItem(
                                    headlineContent = { Text("Unmount") },
                                    modifier = Modifier.clickable {
                                        controller.clearDrive(driveIndex)
                                        setPersistedString("drive_$driveIndex", "")
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
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )

    if (showNewDiskDialog) {
        NewDiskDialog(
            onDismiss = { showNewDiskDialog = false },
            onCreated = { newName ->
                localDisks = listLocalDisks().filter {
                    it.lowercase().endsWith(".dsk") || it.lowercase().endsWith(".imd")
                }
                showNewDiskDialog = false
                // Auto-mount if a drive was selected
                selectedDriveIndex?.let { driveIndex ->
                    val actualName = controller.loadFromLocal(driveIndex, newName)
                    if (actualName != null) {
                        setPersistedString("drive_$driveIndex", "LOCAL:$actualName")
                    }
                    selectedDriveIndex = null
                }
            }
        )
    }
    fun save2Diskette(
        floppy: Floppy,
        fileName: String,
        fileExt: String,
        data: ByteArray,
        driveIndex: Int,
        fullName: String
    ) {
        if (floppy.injectFile(fileName, fileExt, data)) {
            // Re-save to local storage if it was a local disk
            controller.mountedDisks[driveIndex]?.let { mounted ->
                if (mounted.source == DiskController.Source.LOCAL) {
                    saveLocalDisk(mounted.name, floppy.getRawData())
                }
            }
            Abc80Log.terminal("Successfully injected $fullName into drive ${'A' + driveIndex}")
        } else {
            errorMessage = "Failed to copy $fullName. The disk may be full."
        }
    }

    fun clearCloudImportTargetIfRequested(floppy: Floppy, driveIndex: Int) {
        if (!clearCloudImportTarget || cloudImportTargetCleared) return
        floppy.clearFiles()
        cloudImportTargetCleared = true
        controller.mountedDisks[driveIndex]?.let { mounted ->
            if (mounted.source == DiskController.Source.LOCAL) {
                saveLocalDisk(mounted.name, floppy.getRawData())
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
                        val floppy = controller.getFloppy(driveIndex)
                        floppy?.apply {
                            clearCloudImportTargetIfRequested(this, driveIndex)
                            files.forEach { file ->
                                file.name.splitFilename().let {
                                    save2Diskette(
                                        floppy,
                                        it.first,
                                        it.second,
                                        file.content,
                                        driveIndex,
                                        file.name
                                    )
                                }
                            }
                        }
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
            onDismiss = { showCloudImportDialog = false },
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
                                clearCloudImportTargetIfRequested(floppy, driveIndex)
                            }
                            normalFiles.forEach { (fullName, data) ->
                                val fileName = fullName.substringAfterLast("/").substringBeforeLast(".")
                                val ext = fullName.substringAfterLast(".").take(3)
                                save2Diskette(floppy, fileName, ext, data, driveIndex, fullName)
                            }
                        } else {
                            // Only normal files
                            clearCloudImportTargetIfRequested(floppy, driveIndex)
                            selectedFiles.forEach { (fullName, data) ->
                                val fileName = fullName.substringAfterLast("/").substringBeforeLast(".")
                                val ext = fullName.substringAfterLast(".").take(3)
                                save2Diskette(floppy, fileName, ext, data, driveIndex, fullName)
                            }
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
                    "Error",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(message)
                }
            },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    exportPending?.let { pending ->
        AlertDialog(
            onDismissRequest = { exportPending = null },
            title = { Text("Export Folder Already Exists") },
            text = { Text("The folder '${pending.floppyName}' already exists in the target directory. What would you like to do?") },
            confirmButton = {
                Button(
                    onClick = {
                        performExport(pending.parentDir, pending.floppyName, pending.floppy, deleteFirst = true)
                        exportPending = null
                    }
                ) {
                    Text("Delete Old & Export")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        performExport(pending.parentDir, pending.floppyName, pending.floppy, deleteFirst = false)
                        exportPending = null
                    }
                ) {
                    Text("Overwrite/Merge")
                }
            }
        )
    }
}

@Composable
fun NewDiskDialog(
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    val templates = listOf("CPM22.DSK", "EMPTY.DSK")
    var selectedTemplate by remember { mutableStateOf(templates[0]) }
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var showOverwriteConfirmation by remember { mutableStateOf<String?>(null) }

    fun createDisk(name: String) {
        scope.launch {
            try {
                @OptIn(ExperimentalResourceApi::class)
                val data = Res.readBytes("files/$selectedTemplate")

                // Preserve the prepared system tracks in the selected template.
                val floppy = Floppy()
                floppy.loadRawData(data)

                if (saveLocalDisk(name, floppy.getRawData())) {
                    onCreated(name)
                }
            } catch (e: Exception) {
                Abc80Log.wtf("Creation failed: ${e.message}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Disk") },
        text = {
            Column {
                Text("Select template:", style = MaterialTheme.typography.labelSmall)
                templates.forEach { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTemplate = template }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTemplate == template,
                            onClick = { selectedTemplate = template })
                        Text(
                            template.substringBeforeLast("."),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = newName,
                    onValueChange = { input ->
                        // Filter for A-Z only, max 8 chars, and force uppercase
                        val filtered = input.uppercase().filter { it in 'A'..'Z' }.take(8)
                        newName = filtered
                    },
                    label = { Text("Name (max 8 chars A-Z)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        val finalName = "$newName.DSK"
                        if (fileExists(finalName)) {
                            showOverwriteConfirmation = finalName
                        } else {
                            createDisk(finalName)
                        }
                    }
                },
                enabled = newName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    showOverwriteConfirmation?.let { name ->
        AlertDialog(
            onDismissRequest = { showOverwriteConfirmation = null },
            title = { Text("File Already Exists") },
            text = { Text("The disk image '$name' already exists. What would you like to do?") },
            confirmButton = {
                Button(
                    onClick = {
                        createDisk(name)
                        showOverwriteConfirmation = null
                    }
                ) {
                    // Overwrite is basically "Replace" for a file
                    Text("Overwrite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
