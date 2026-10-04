package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.core.Floppy
import com.aboveware.aboveabc80.core.unzipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StorageFileDialog(
    filter: String = "",
    targetDriveIndex: Int? = null,
    onDismiss: () -> Unit,
    onFilesSelected: ((List<Pair<String, ByteArray>>, Boolean) -> Unit)? = null
) {
    val provider = remember { FirebaseRestStorageProvider() }
    val scope = rememberCoroutineScope()
    val controller = remember { DiskController.instance }
    var nodes by remember { mutableStateOf<List<StorageNode>>(emptyList()) }
    var pathStack by remember { mutableStateOf(listOf("abc80/")) }
    var isLoading by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadingFileName by remember { mutableStateOf("") }
    var clearDestinationBeforeDownload by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedCloudFiles by remember { mutableStateOf<List<Pair<String, ByteArray>>>(emptyList()) }
    var selectedNodes by remember(nodes) { mutableStateOf<Set<StorageNode>>(emptySet()) }
    var textPreview by remember { mutableStateOf<Pair<String, AnnotatedString>?>(null) }
    var activeImageIndex by remember { mutableStateOf<Int?>(null) }
    var archivePreview by remember { mutableStateOf<Pair<String, List<ArchivedFile>>?>(null) }
    var localDisks by remember { mutableStateOf(listLocalDisks()) }
    var showNewDiskDialog by remember { mutableStateOf(false) }

    // Refresh list when dialog opens
    LaunchedEffect(Unit) {
        localDisks = listLocalDisks()
    }

    var thumbnails by remember { mutableStateOf(mapOf<String, ImageBitmap>()) }
    var fileCache by remember { mutableStateOf(mapOf<String, ByteArray>()) }

    fun injectFileToDisk(
        diskName: String,
        fileName: String,
        data: ByteArray,
        shouldClearFirst: Boolean = false
    ) {
        val bytes = loadLocalDisk(diskName) ?: return
        val floppy = Floppy.createFromData(bytes)
        if (shouldClearFirst) {
            floppy.format()
        }

        val name = fileName.substringAfterLast("/").substringBeforeLast(".")
        val ext = if (fileName.contains(".")) fileName.substringAfterLast(".").take(3) else "BIN"

        if (floppy.injectFile(name, ext, data)) {
            var finalDiskName = diskName
            if (diskName.lowercase().endsWith(".imd")) {
                finalDiskName = diskName.substringBeforeLast(".") + ".DSK"
            }

            saveLocalDisk(finalDiskName, floppy.getRawData())

            // If this disk is currently mounted, reload it in the controller
            val controller = DiskController.instance
            controller.mountedDisks.forEach { (index, mounted) ->
                if (mounted.source == DiskController.Source.LOCAL && (mounted.name == diskName || mounted.name == finalDiskName)) {
                    controller.loadFromLocal(index, finalDiskName)
                }
            }
            Abc80Log.terminal("Successfully injected $fileName into $finalDiskName")
        } else {
            Abc80Log.wtf("Failed to inject $fileName into $diskName (disk full?)")
        }
    }

    fun handleFilesSelection(files: List<Pair<String, ByteArray>>) {
        if (clearDestinationBeforeDownload) {
            val driveIdx = targetDriveIndex ?: controller.currentDriveIndex
            val mounted = controller.mountedDisks[driveIdx]
            if (mounted != null && mounted.source == DiskController.Source.LOCAL) {
                val bytes = loadLocalDisk(mounted.name)
                if (bytes != null) {
                    val floppy = Floppy.createFromData(bytes)
                    floppy.format()
                    saveLocalDisk(mounted.name, floppy.getRawData())
                    controller.loadFromLocal(driveIdx, mounted.name)
                }
            }
        }
        if (onFilesSelected != null) {
            onFilesSelected.invoke(files, clearDestinationBeforeDownload)
            onDismiss()
        } else {
            selectedCloudFiles = files
        }
    }

    fun isArchiveFile(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in setOf("ark", "arc", "zip", "lbr")

    fun isImageFile(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "bmp", "gif", "webp")

    fun openArchive(node: StorageNode) {
        scope.launch {
            downloadingFileName = node.name
            isDownloading = true
            try {
                val data = fileCache[node.fullName] ?: provider.downloadFile(node.fullName)
                if (data == null) {
                    errorMessage = "Could not load ${node.name}."
                } else {
                    if (!fileCache.containsKey(node.fullName)) {
                        fileCache = fileCache + (node.fullName to data)
                    }
                    val files = when (node.name.substringAfterLast('.', "").lowercase()) {
                        "ark", "arc" -> NativeLib.getObject().extractArkArchive(data)
                        "zip" -> unzipFile(data)
                        "lbr" -> NativeLib.getObject().extractLbrArchive(data)
                        else -> emptyList()
                    }
                    archivePreview = node.name to files
                }
            } catch (e: Exception) {
                errorMessage = "Could not open ${node.name}: ${e.message}"
            } finally {
                isDownloading = false
            }
        }
    }

    val currentPrefix = pathStack.last()

    LaunchedEffect(currentPrefix) {
        isLoading = true
        try {
            nodes = provider.listFiles(currentPrefix)
            isLoading = false
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load files"
            isLoading = false
        }
    }

    LaunchedEffect(nodes) {
        nodes.filter { !it.isDirectory }.forEach { node ->
            val lowerName = node.name.lowercase()
            val isImage = lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".bmp")

            if (isImage && !thumbnails.containsKey(node.fullName)) {
                launch {
                    val data = fileCache[node.fullName] ?: provider.downloadFile(node.fullName)
                    if (data != null) {
                        if (!fileCache.containsKey(node.fullName)) {
                            fileCache = fileCache + (node.fullName to data)
                        }
                        val imageBitmap = decodeImagePlatform(data)
                        if (imageBitmap != null) {
                            thumbnails = thumbnails + (node.fullName to imageBitmap)
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) {
                if (selectedCloudFiles.isNotEmpty()) {
                    selectedCloudFiles = emptyList()
                } else {
                    onDismiss()
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (selectedCloudFiles.isNotEmpty()) "Save to Diskette" else "Cloud Files")
                    if (pathStack.size > 1 && selectedCloudFiles.isEmpty()) {
                        Text(
                            text = "/" + pathStack.last().removePrefix("cpm/"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Box(modifier = Modifier.sizeIn(minHeight = 300.dp, maxHeight = 500.dp).fillMaxWidth()) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (selectedCloudFiles.isNotEmpty()) {
                    val controller = DiskController.instance
                    val mountedDrives = (0 until DiskController.MAX_DRIVES).mapNotNull { i ->
                        val mounted = controller.mountedDisks[i]
                        if (mounted != null && mounted.source == DiskController.Source.LOCAL) {
                            i to mounted
                        } else null
                    }

                    Column {
                        val totalBytes = selectedCloudFiles.sumOf { it.second.size.toLong() }
                        val displayNames = if (selectedCloudFiles.size > 1) {
                            "${selectedCloudFiles.size} files"
                        } else {
                            "\"${selectedCloudFiles[0].first}\""
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Save $displayNames to:",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${formatSize(totalBytes)})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary // Default color at top
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            if (mountedDrives.isNotEmpty()) {
                                item {
                                    Text(
                                        "Mounted Drives",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                items(mountedDrives) { (index, mounted) ->
                                    val floppy = controller.getFloppy(index)
                                    val activeIdx = targetDriveIndex ?: controller.currentDriveIndex
                                    val isTargetDrive = index == activeIdx
                                    val freeSpace =
                                        if (clearDestinationBeforeDownload && isTargetDrive && floppy != null) {
                                            val copy =
                                                Floppy.createFromData(floppy.getRawData().copyOf())
                                            copy.format()
                                            copy.getFreeSpace()
                                        } else {
                                            floppy?.getFreeSpace() ?: 0L
                                        }
                                    val fits = freeSpace >= totalBytes

                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                "Drive ${('A' + index)}: ${
                                                    mounted.name.substringBeforeLast(
                                                        "."
                                                    )
                                                }"
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = "Free space: ${formatSize(freeSpace)}",
                                                color = if (fits) MaterialTheme.colorScheme.onSurfaceVariant else Color.Red
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = if (fits) MaterialTheme.colorScheme.primary else Color.Red
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            selectedCloudFiles.forEachIndexed { i, (fileName, fileData) ->
                                                val shouldClear =
                                                    clearDestinationBeforeDownload && (i == 0) && isTargetDrive
                                                injectFileToDisk(
                                                    mounted.name,
                                                    fileName,
                                                    fileData,
                                                    shouldClearFirst = shouldClear
                                                )
                                            }
                                            selectedCloudFiles = emptyList()
                                            onDismiss()
                                        }
                                    )
                                }
                                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                            }

                            item {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            "Create new diskette...",
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

                            val otherDisks = localDisks.filter { diskName ->
                                mountedDrives.none { it.second.name == diskName }
                            }

                            if (otherDisks.isNotEmpty()) {
                                item {
                                    Text(
                                        "Other Disks",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                items(otherDisks) { diskName ->
                                    val ext = diskName.substringAfterLast(".").uppercase()
                                    val bytes = loadLocalDisk(diskName)
                                    val freeSpace = if (bytes != null) Floppy.createFromData(bytes)
                                        .getFreeSpace() else 0L
                                    val fits = freeSpace >= totalBytes

                                    ListItem(
                                        headlineContent = { Text(diskName.substringBeforeLast(".") + if (ext != "DSK") " ($ext)" else "") },
                                        supportingContent = {
                                            Text(
                                                text = "Free space: ${formatSize(freeSpace)}",
                                                color = if (fits) MaterialTheme.colorScheme.onSurfaceVariant else Color.Red
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = if (fits) Color.Unspecified else Color.Red
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            selectedCloudFiles.forEach { (fileName, fileData) ->
                                                injectFileToDisk(diskName, fileName, fileData)
                                            }
                                            selectedCloudFiles = emptyList()
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                } else {
                    val filteredNodes = nodes.filter { node ->
                        if (filter == "snapshot") {
                            node.isDirectory // Snapshots removed
                        } else true
                    }
                    LazyColumn {
                        if (pathStack.size > 1) {
                            item {
                                ListItem(
                                    headlineContent = { Text("..") },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107)
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        pathStack = pathStack.dropLast(1)
                                    }
                                )
                            }
                        }

                        items(filteredNodes) { node ->
                            val thumbnail = thumbnails[node.fullName]
                            val displayName = node.name
                            val isTextFile = !node.isDirectory && node.name.isText()
                            val isArchiveFile = !node.isDirectory && isArchiveFile(node.name)
                            val isImageFile = !node.isDirectory && isImageFile(node.name)
                            val isSelected = node in selectedNodes

                            val currentFolderImageNodes = remember(nodes) {
                                nodes.filter { !it.isDirectory && isImageFile(it.name) }
                            }

                            ListItem(
                                headlineContent = { Text(displayName) },
                                supportingContent = if (!node.isDirectory) {
                                    { Text(formatSize(node.size)) }
                                } else null,
                                leadingContent = {
                                    if (!node.isDirectory) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedNodes = if (checked) {
                                                    selectedNodes + node
                                                } else {
                                                    selectedNodes - node
                                                }
                                            }
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (thumbnail != null) {
                                            Image(
                                                bitmap = thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clickable {
                                                        val idx = currentFolderImageNodes.indexOf(node)
                                                        if (idx >= 0) activeImageIndex = idx
                                                    }
                                            )
                                        }

                                        if (isImageFile) {
                                            IconButton(
                                                onClick = {
                                                    val idx = currentFolderImageNodes.indexOf(node)
                                                    if (idx >= 0) activeImageIndex = idx
                                                },
                                                enabled = !isDownloading
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = "Preview image"
                                                )
                                            }
                                        }

                                        if (isTextFile) {
                                            IconButton(
                                                onClick = {
                                                    val isPdf =
                                                        node.name.endsWith(
                                                            ".pdf",
                                                            ignoreCase = true
                                                        )
                                                    val cachedData = fileCache[node.fullName]
                                                    if (cachedData != null && !isPdf) {
                                                        val content = if (cachedData.size >= 2 &&
                                                            cachedData[0] == 0x76.toByte() &&
                                                            cachedData[1] == 0xFF.toByte()
                                                        ) {
                                                            "Compressed (Squeezed) file detected. \nManual decompression required in emulator."
                                                        } else {
                                                            cachedData.decodeToString()
                                                        }
                                                        textPreview =
                                                            node.name to AnnotatedString(content)
                                                    } else {
                                                        scope.launch {
                                                            downloadingFileName = node.name
                                                            isDownloading = true
                                                            try {
                                                                val downloadedData = cachedData
                                                                    ?: provider.downloadFile(node.fullName)
                                                                    ?: error("Download returned no data")
                                                                fileCache =
                                                                    fileCache + (node.fullName to downloadedData)

                                                                if (isPdf) {
                                                                    val opened =
                                                                        withContext(Dispatchers.IO) {
                                                                            openPdfPreview(
                                                                                node.name,
                                                                                downloadedData
                                                                            )
                                                                        }
                                                                    if (!opened) {
                                                                        errorMessage =
                                                                            "Could not open ${node.name}. Make sure a PDF viewer is installed."
                                                                    }
                                                                } else {
                                                                    val content =
                                                                        if (downloadedData.size >= 2 &&
                                                                            downloadedData[0] == 0x76.toByte() &&
                                                                            downloadedData[1] == 0xFF.toByte()
                                                                        ) {
                                                                            "Compressed (Squeezed) file detected. \nManual decompression required in emulator."
                                                                        } else {
                                                                            downloadedData.decodeToString()
                                                                        }
                                                                    textPreview =
                                                                        node.name to AnnotatedString(
                                                                            content
                                                                        )
                                                                }
                                                            } catch (e: Exception) {
                                                                errorMessage =
                                                                    "Could not open ${node.name}: ${e.message}"
                                                            } finally {
                                                                isDownloading = false
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = "Open document"
                                                )
                                            }
                                        }

                                        if (isArchiveFile) {
                                            IconButton(
                                                onClick = { openArchive(node) },
                                                enabled = !isDownloading
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderOpen,
                                                    contentDescription = "Open archive"
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    if (node.isDirectory) {
                                        pathStack = pathStack + node.fullName
                                    } else {
                                        selectedNodes = if (isSelected) {
                                            selectedNodes - node
                                        } else {
                                            selectedNodes + node
                                        }
                                    }
                                }
                            )
                        }

                        if (filteredNodes.isEmpty() && pathStack.size == 1) {
                            item {
                                Text(
                                    "No matching files found.",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                if (isDownloading) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (downloadingFileName.isNotEmpty()) "Downloading $downloadingFileName…" else "Downloading…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedCloudFiles.isNotEmpty()) {
                TextButton(onClick = {
                    selectedCloudFiles = emptyList()
                    clearDestinationBeforeDownload = false
                }) {
                    Text("Back")
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val activeFloppy =
                        controller.getFloppy(targetDriveIndex ?: controller.currentDriveIndex)
                    if (activeFloppy != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clearDestinationBeforeDownload = !clearDestinationBeforeDownload
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = clearDestinationBeforeDownload,
                                onCheckedChange = { clearDestinationBeforeDownload = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear active floppy before download",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isDownloading) {
                            Text("Close")
                        }

                        Row {
                            val fileNodes = nodes.filter { !it.isDirectory }
                            TextButton(
                                enabled = fileNodes.isNotEmpty() && !isDownloading,
                                onClick = {
                                    selectedNodes = if (selectedNodes.size == fileNodes.size) {
                                        emptySet()
                                    } else {
                                        fileNodes.toSet()
                                    }
                                }
                            ) {
                                Text(if (selectedNodes.size == fileNodes.size && fileNodes.isNotEmpty()) "Deselect all" else "Select all")
                            }

                            TextButton(
                                enabled = selectedNodes.isNotEmpty() && !isDownloading,
                                onClick = {
                                    scope.launch {
                                        isDownloading = true
                                        val results = mutableListOf<Pair<String, ByteArray>>()
                                        var allOk = true
                                        for (node in selectedNodes) {
                                            downloadingFileName = node.name
                                            val data =
                                                fileCache[node.fullName] ?: provider.downloadFile(
                                                    node.fullName
                                                )
                                            if (data != null) {
                                                if (!fileCache.containsKey(node.fullName)) {
                                                    fileCache = fileCache + (node.fullName to data)
                                                }
                                                results.add(node.name to data)
                                            } else {
                                                allOk = false
                                                errorMessage = "Could not download ${node.name}"
                                                break
                                            }
                                        }
                                        isDownloading = false
                                        if (allOk && results.isNotEmpty()) {
                                            handleFilesSelection(results)
                                        }
                                    }
                                }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (selectedNodes.size > 1) "Download (${selectedNodes.size})" else "Download")
                                    if (selectedNodes.isNotEmpty()) {
                                        val totalSize = selectedNodes.sumOf { it.size }
                                        val currentFloppy = controller.getFloppy(
                                            targetDriveIndex ?: controller.currentDriveIndex
                                        )
                                        val freeSpace =
                                            if (clearDestinationBeforeDownload && currentFloppy != null) {
                                                val copy = Floppy.createFromData(
                                                    currentFloppy.getRawData().copyOf()
                                                )
                                                copy.format()
                                                copy.getFreeSpace()
                                            } else {
                                                currentFloppy?.getFreeSpace() ?: 0L
                                            }
                                        val fits = currentFloppy == null || totalSize <= freeSpace

                                        Text(
                                            text = formatSize(totalSize),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (fits) MaterialTheme.colorScheme.primary else Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    if (showNewDiskDialog) {
        NewDisketteDialog(
            onDismiss = { showNewDiskDialog = false },
            onCreated = { newDiskName ->
                localDisks = listLocalDisks()
                showNewDiskDialog = false
                // Auto-inject the selected files if we have any
                if (selectedCloudFiles.isNotEmpty()) {
                    selectedCloudFiles.forEach { (fileName, fileData) ->
                        injectFileToDisk(newDiskName, fileName, fileData)
                    }
                    selectedCloudFiles = emptyList()
                    onDismiss()
                }
            }
        )
    }

    textPreview?.let { (fileName, content) ->
        ArchiveFileTextPreviewDialog(
            fileName = fileName,
            content = content,
            onDismissRequest = { textPreview = null }
        )
    }

    val currentFolderImageNodes = remember(nodes) {
        nodes.filter { !it.isDirectory && isImageFile(it.name) }
    }

    if (activeImageIndex != null) {
        val currentIndex = activeImageIndex!!
        val currentNode = currentFolderImageNodes.getOrNull(currentIndex)
        if (currentNode != null) {
            val currentBitmap = thumbnails[currentNode.fullName]
            val totalImages = currentFolderImageNodes.size

            LaunchedEffect(currentNode.fullName) {
                if (!thumbnails.containsKey(currentNode.fullName)) {
                    val downloadedData = fileCache[currentNode.fullName]
                        ?: provider.downloadFile(currentNode.fullName)
                    if (downloadedData != null) {
                        if (!fileCache.containsKey(currentNode.fullName)) {
                            fileCache = fileCache + (currentNode.fullName to downloadedData)
                        }
                        val bitmap = decodeImagePlatform(downloadedData)
                        if (bitmap != null) {
                            thumbnails = thumbnails + (currentNode.fullName to bitmap)
                        }
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { activeImageIndex = null },
                title = {
                    Text(
                        if (totalImages > 1) {
                            "${currentNode.name} (${currentIndex + 1}/$totalImages)"
                        } else {
                            currentNode.name
                        }
                    )
                },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { activeImageIndex = currentIndex - 1 },
                            enabled = currentIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous image"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 400.dp)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentBitmap != null) {
                                Image(
                                    bitmap = currentBitmap,
                                    contentDescription = currentNode.name,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }

                        IconButton(
                            onClick = { activeImageIndex = currentIndex + 1 },
                            enabled = currentIndex < totalImages - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next image"
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeImageIndex = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    archivePreview?.let { (fileName, files) ->
        ArchiveFileDialog(
            archiveName = fileName,
            files = files,
            targetDriveIndex = targetDriveIndex ?: controller.currentDriveIndex,
            clearBeforeDownload = clearDestinationBeforeDownload,
            onClearBeforeDownloadChange = {
                clearDestinationBeforeDownload = it
            },
            onResult = { selectedFiles ->
                archivePreview = null
                selectedFiles?.let { archivedFiles ->
                    handleFilesSelection(archivedFiles.map { it.name to it.content })
                }
            }
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun NewDisketteDialog(
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    val templates = listOf("fd2" to "ABC80 80 kB (SSSD)", "abc830" to "ABC80 160 kB (SSDD)", "fd4d" to "ABC80 320 kB (DSDD)", "abc832" to "ABC80 640 kB (DSQD)")
    var selectedTemplate by remember { mutableStateOf(templates[0].first) }
    var systemDisk by remember { mutableStateOf(true) }
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var showOverwriteConfirmation by remember { mutableStateOf<String?>(null) }

    fun createDisk(name: String) {
        scope.launch {
            try {
                val data = if (systemDisk) {
                    Res.readBytes("files/system.dsk")
                } else {
                    Abc80FloppyLayout(selectedTemplate, 0).create()
                }
                if (saveLocalDisk(name, data)) {
                    onCreated(name)
                }
            } catch (e: Exception) {
                Abc80Log.wtf("Creation failed: ${e.message}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Diskette") },
        text = {
            Column {
                Text("Disk type:", style = MaterialTheme.typography.labelSmall)
                listOf(true to "System disk (160 kB)", false to "Empty disk").forEach { (isSystem, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { systemDisk = isSystem }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = systemDisk == isSystem, onClick = { systemDisk = isSystem })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (!systemDisk) Text("Format:", style = MaterialTheme.typography.labelSmall)
                if (!systemDisk) templates.forEach { (template, label) ->
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
                            label,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = newName,
                    onValueChange = { input ->
                        val filtered =
                            input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(8)
                        newName = filtered
                    },
                    label = { Text("Name (max 8 chars A-Z, 0-9)") },
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

fun decodeImagePlatform(data: ByteArray): ImageBitmap? = decodeImage(data)

expect fun decodeImage(data: ByteArray): ImageBitmap?
