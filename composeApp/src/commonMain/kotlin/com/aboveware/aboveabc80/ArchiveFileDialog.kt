package com.aboveware.aboveabc80

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.core.Floppy

private fun ByteArray.toCpmPreviewText(): AnnotatedString {
    data class InlinePart(
        val text: String,
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean
    )

    fun parseInlineControls(value: String): List<InlinePart> {
        val parts = mutableListOf<InlinePart>()
        val text = StringBuilder()
        var bold = false
        var italic = false
        var underline = false

        fun flush() {
            if (text.isNotEmpty()) {
                parts += InlinePart(text.toString(), bold, italic, underline)
                text.clear()
            }
        }

        var index = 0
        while (index < value.length) {
            when {
                value.startsWith("~=", index) -> {
                    val end = value.indexOf('~', index + 2)
                    if (end > index + 2 && value.substring(index + 2, end).all(Char::isDigit)) {
                        index = end + 1
                    } else {
                        text.append(value[index++])
                    }
                }

                value[index] == '[' && index + 1 < value.length -> {
                    when (value[index + 1].uppercaseChar()) {
                        'S', 'B' -> {
                            flush()
                            bold = !bold
                            index += 2
                        }
                        'I' -> {
                            flush()
                            italic = !italic
                            index += 2
                        }
                        'U' -> {
                            flush()
                            underline = !underline
                            index += 2
                        }
                        'P' -> {
                            flush()
                            index += 2
                        }
                        else -> text.append(value[index++])
                    }
                }

                else -> text.append(value[index++])
            }
        }
        flush()
        return parts
    }

    val lines = mutableListOf<StringBuilder>()
    var line = StringBuilder()
    var column = 0
    var carriageReturnPending = false
    fun commitLine() {
        lines += line
        line = StringBuilder()
        column = 0
    }

    fun add(value: Int) {
        while (line.length <= column) line.append(' ')
        line[column] = value.toChar()
        column++
    }

    for (byte in this) {
        when (val value = byte.toInt() and 0xFF) {
            0x0D,
            0x0E -> {
                commitLine()
                carriageReturnPending = true
            }

            0x0A -> {
            }

            0x1A -> Unit
            0x09 -> {
                val nextTab = ((column / 8) + 1) * 8
                while (column < nextTab) {
                    line.append(' ')
                    column++
                }
            }

            in 0x20..0x7E -> {
                add(value)
            }

            else -> Unit
        }
    }
    if (line.isNotEmpty()) lines += line

    data class RenderedLine(
        val parts: List<InlinePart>,
        val bold: Boolean,
        val italic: Boolean,
        val centered: Boolean,
        val rightAligned: Boolean
    )
    val renderedLines = mutableListOf<RenderedLine>()
    var centered = false
    var rightAligned = false
    var bold = false
    var italic = false
    var linesPerPage = 66
    var charactersPerLine = 80
    var pendingRightText = false

    val logicalLines = lines.joinToString("\n").lines().toMutableList()
    var lineIndex = 1
    while (lineIndex < logicalLines.size - 1) {
        if (logicalLines[lineIndex].trim().matches(Regex("--b1\\s+r")) &&
            logicalLines[lineIndex + 1].trim().matches(Regex("\\d+"))
        ) {
            val previousIndex = (lineIndex - 1 downTo 0).firstOrNull { index ->
                logicalLines[index].trim().isNotEmpty()
            }
            if (previousIndex != null) {
                logicalLines[previousIndex] =
                    "${logicalLines[previousIndex].trimEnd()}..b1 r${logicalLines[lineIndex + 1].trim()}"
                logicalLines.subList(previousIndex + 1, lineIndex + 2).clear()
                lineIndex = (previousIndex + 1).coerceAtLeast(1)
                continue
            }
        }
        lineIndex++
    }

    for (line in logicalLines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("..")) {
            val rightNumberCommand = Regex("^\\.\\.b1\\s+r(\\d+)\\s*$")
                .matchEntire(trimmed)
            if (rightNumberCommand != null && renderedLines.isNotEmpty()) {
                val number = rightNumberCommand.groupValues[1]
                val previousIndex = renderedLines.indexOfLast { renderedLine ->
                    renderedLine.parts.any { part -> part.text.trim().isNotEmpty() }
                }
                if (previousIndex < 0) continue
                while (renderedLines.lastIndex > previousIndex) {
                    renderedLines.removeAt(renderedLines.lastIndex)
                }
                val previous = renderedLines[previousIndex]
                val visibleLength = previous.parts.sumOf { it.text.length }
                val padding = (charactersPerLine - visibleLength - number.length).coerceAtLeast(1)
                renderedLines[previousIndex] = previous.copy(
                    parts = previous.parts +
                        InlinePart(" ".repeat(padding), false, false, false) +
                        InlinePart(number, true, false, false)
                )
                continue
            }
            trimmed.substring(2).split(Regex("\\s+")).forEach { command ->
                when {
                    command == "c" -> centered = true
                    command == "c+" -> centered = true
                    command == "c-" -> centered = false
                    command == "b1" -> bold = true
                    command == "b0" -> bold = false
                    command == "e+" -> italic = true
                    command == "e-" -> italic = false
                    command == "r" -> rightAligned = true
                    command.startsWith("l") ->
                        command.substring(1).toIntOrNull()?.let { linesPerPage = it }
                    command.startsWith("m") ->
                        command.substring(1).toIntOrNull()?.let { charactersPerLine = it }
                    command.startsWith("j") || command.startsWith("g") ||
                        command.startsWith("v") || command.startsWith("n") -> Unit
                }
            }
            continue
        }
        if (trimmed.matches(Regex("--b1\\s+r"))) {
            pendingRightText = true
            continue
        }
        if (trimmed.startsWith(". ") ||
            trimmed.startsWith(".r ") ||
            trimmed.startsWith("~=R") ||
            trimmed.matches(Regex("~=\\d{3}~")) ||
            trimmed == ")"
        ) continue

        if (pendingRightText && trimmed.isNotEmpty()) {
            val previousIndex = renderedLines.indexOfLast { renderedLine ->
                renderedLine.parts.any { part -> part.text.trim().isNotEmpty() }
            }
            if (previousIndex >= 0) {
                while (renderedLines.lastIndex > previousIndex) {
                    renderedLines.removeAt(renderedLines.lastIndex)
                }
                val previous = renderedLines[previousIndex]
                val followingText = parseInlineControls(trimmed)
                val followingLength = followingText.sumOf { it.text.length }
                val visibleLength = previous.parts.sumOf { it.text.length }
                val padding = (charactersPerLine - visibleLength - followingLength).coerceAtLeast(1)
                renderedLines[previousIndex] = previous.copy(
                    parts = previous.parts +
                        InlinePart(" ".repeat(padding), false, false, false) +
                        followingText.map { it.copy(bold = true) }
                )
                pendingRightText = false
                continue
            }
        }

        val rawContent = if (trimmed.length >= 2 && trimmed.startsWith("|") && trimmed.endsWith("|")) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            line
        }
        val rightNumberMatch = Regex("^(.*?)(?:\\.\\.b1\\s+r(\\d+))\\s*$")
            .matchEntire(rawContent)
        val inlineParts = if (rightNumberMatch != null) {
            val prefix = rightNumberMatch.groupValues[1].trimEnd()
            val number = rightNumberMatch.groupValues[2]
            val prefixParts = parseInlineControls(prefix)
            val prefixLength = prefixParts.sumOf { it.text.length }
            val padding = (charactersPerLine - prefixLength - number.length).coerceAtLeast(1)
            prefixParts + InlinePart(" ".repeat(padding), false, false, false) +
                InlinePart(number, true, false, false)
        } else {
            parseInlineControls(rawContent)
        }
        val visibleContent = inlineParts.joinToString(separator = "") { it.text }
        val isHeading = visibleContent.trim().isNotEmpty() &&
            visibleContent.trim().length <= 80 &&
            visibleContent.trim() == visibleContent.trim().uppercase() &&
            visibleContent.any { it.isLetter() }
        val alignedContent = if (centered) {
            val visibleLength = visibleContent.trim().length
            val padding = ((charactersPerLine - visibleLength) / 2).coerceAtLeast(0)
            listOf(InlinePart(" ".repeat(padding), false, false, false)) +
                parseInlineControls(visibleContent.trim())
        } else if (rightAligned) {
            val visibleLength = visibleContent.trim().length
            val padding = (charactersPerLine - visibleLength).coerceAtLeast(0)
            listOf(InlinePart(" ".repeat(padding), false, false, false)) + inlineParts
        } else {
            inlineParts
        }
        renderedLines += RenderedLine(
            alignedContent,
            bold || isHeading,
            italic,
            centered,
            rightAligned
        )

        // MagicIndex line controls are scoped to the next 0x0E/line.
        bold = false
        italic = false
        centered = false
        rightAligned = false
    }

    var renderedIndex = 1
    while (renderedIndex < renderedLines.size) {
        val number = renderedLines[renderedIndex].parts
            .joinToString(separator = "") { it.text }
            .trim()
        if (number.matches(Regex("\\d+"))) {
            val previousIndex = (renderedIndex - 1 downTo 0).firstOrNull { index ->
                renderedLines[index].parts.any { it.text.trim().isNotEmpty() }
            }
            if (previousIndex != null) {
                val previous = renderedLines[previousIndex]
                val previousTextLength = previous.parts.sumOf { it.text.length }
                val padding = (charactersPerLine - previousTextLength - number.length).coerceAtLeast(1)
                renderedLines[previousIndex] = previous.copy(
                    parts = previous.parts +
                        InlinePart(" ".repeat(padding),
                            bold = false,
                            italic = false,
                            underline = false
                        ) +
                        InlinePart(number, bold = true, italic = false, underline = false)
                )
                renderedLines.removeAt(renderedIndex)
                continue
            }
        }
        renderedIndex++
    }

    return buildAnnotatedString {
        renderedLines.forEachIndexed { index, renderedLine ->
            if (index > 0) append('\n')
            renderedLine.parts.forEach { part ->
                val style = SpanStyle(
                    fontWeight = if (renderedLine.bold || part.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (renderedLine.italic || part.italic) androidx.compose.ui.text.font.FontStyle.Italic
                    else androidx.compose.ui.text.font.FontStyle.Normal,
                    textDecoration = if (part.underline) TextDecoration.Underline else TextDecoration.None
                )
                withStyle(style) { append(part.text) }
            }
        }
    }
}

@Composable
fun ArchiveFileDialog(
    archiveName: String,
    files: List<ArchivedFile>,
    onResult: (List<ArchivedFile>?) -> Unit,
    targetDriveIndex: Int? = null,
    clearBeforeDownload: Boolean = false,
    onClearBeforeDownloadChange: (Boolean) -> Unit = {}
) {
    var selectedIndices by remember(files) { mutableStateOf<Set<Int>>(emptySet()) }
    var textPreview by remember { mutableStateOf<Pair<String, AnnotatedString>?>(null) }
    var clearDestination by remember(files, targetDriveIndex) { mutableStateOf(clearBeforeDownload) }
    val controller = remember { DiskController.instance }
    val targetFloppy = controller.getFloppy(targetDriveIndex ?: controller.currentDriveIndex)
    val simulatedFloppy = targetFloppy?.let {
        Floppy.createFromData(it.getRawData().copyOf()).apply {
            if (clearDestination) clearFiles()
        }
    }
    var selectionFits = true
    if (simulatedFloppy != null) {
        for (index in selectedIndices.sorted()) {
            val file = files[index]
            val (name, extension) = file.name.splitFilename()
            if (!simulatedFloppy.injectFile(name, extension, file.content)) {
                selectionFits = false
                break
            }
        }
    }
    val remainingSpace = simulatedFloppy?.getFreeSpace() ?: 0L

    fun dismiss(result: List<ArchivedFile>?) {
        onResult(result)
    }

    AlertDialog(
        onDismissRequest = { dismiss(null) },
        title = {
            Column {
                Text("Select files to download")
                Text(
                    text = archiveName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            ArchiveFileSelector(
                files = files,
                selectedIndices = selectedIndices,
                onSelectionChanged = { selectedIndices = it },
                onPreviewText = { file ->
                    val isZzz = file.name.substringAfterLast('.', "")
                        .equals("zzz", ignoreCase = true)
                    val previewBytes = if (isZzz) {
                        val nativeLib = NativeLib.getObject()
                        nativeLib.decompressSqueezed(file.content) ?: run {
                            ZXLog.wtf("Could not decompress ${file.name}.")
                            file.content
                        }
                    } else {
                        file.content
                    }

                    val content = previewBytes.toCpmPreviewText()
                    textPreview = file.name to content
                }
            )
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clearDestination = !clearDestination
                            onClearBeforeDownloadChange(clearDestination)
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = clearDestination,
                        onCheckedChange = {
                            clearDestination = it
                            onClearBeforeDownloadChange(it)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear active floppy before download")
                }
                if (selectedIndices.isNotEmpty() && targetFloppy != null) {
                    Text(
                        text = if (selectionFits) {
                            "Remaining after download: ${formatSize(remainingSpace)}"
                        } else {
                            "Files will not fit on the selected floppy"
                        },
                        color = if (selectionFits) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.Red
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { dismiss(null) }) {
                        Text("Back")
                    }
                    Row {
                        TextButton(
                            enabled = files.isNotEmpty(),
                            onClick = {
                                selectedIndices = files.indices.toSet()
                            }
                        ) {
                            Text("Select all")
                        }

                        TextButton(
                            enabled = selectedIndices.isNotEmpty(),
                            onClick = {
                                onResult(selectedIndices.sorted().map { files[it] })
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Download (${selectedIndices.size})")
                                val totalSize = selectedIndices.sumOf { files[it].content.size.toLong() }
                                Text(
                                    text = formatSize(totalSize),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectionFits || targetFloppy == null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Red
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {}
    )

    textPreview?.let { (fileName, content) ->
        ArchiveFileTextPreviewDialog(
            fileName = fileName,
            content = content,
            onDismissRequest = { textPreview = null }
        )
    }
}

@Composable
fun ArchiveFileTextPreviewDialog(
    fileName: String,
    content: AnnotatedString,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        text = {
            ArchiveFileTextPreviewContent(
                fileName = fileName,
                content = content,
                onClose = onDismissRequest
            )
        }
    )
}

@Composable
fun ArchiveFileSelector(
    files: List<ArchivedFile>,
    selectedIndices: Set<Int>,
    onSelectionChanged: (Set<Int>) -> Unit,
    onPreviewText: (ArchivedFile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.heightIn(max = 420.dp)) {
        itemsIndexed(files) { index, file ->
            val selected = index in selectedIndices
            fun toggleSelection() {
                val newSelection = if (selected) {
                    selectedIndices - index
                } else {
                    selectedIndices + index
                }
                onSelectionChanged(newSelection)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { toggleSelection() }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { toggleSelection() }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "${file.name} (${formatSize(file.content.size.toLong())})",
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    )

                    if (file.name.isText()) {
                        IconButton(
                            onClick = { onPreviewText(file) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Open document"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun ArchiveFileSelectorPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val sampleFiles = remember {
                listOf(
                    ArchivedFile(
                        "document.txt",
                        "Detta är innehållet i en textfil.".encodeToByteArray()
                    ),
                    ArchivedFile("image.png", ByteArray(1024)),
                    ArchivedFile("script.txt", "line 1\nline 2\nline 3".encodeToByteArray())
                )
            }
            var selected by remember { mutableStateOf(setOf(0)) }

            ArchiveFileSelector(
                files = sampleFiles,
                selectedIndices = selected,
                onSelectionChanged = { selected = it },
                onPreviewText = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArchiveFileDialogPreview() {
    MaterialTheme {
        ArchiveFileDialog(
            archiveName = "SAMPLE.ARK",
            files = listOf(
                ArchivedFile("README.TXT", "Sample content".encodeToByteArray()),
                ArchivedFile("INSTALL.BAT", "Echo setup".encodeToByteArray())
            ),
            onResult = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun ArchiveFileTextPreviewDialogPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.4f) // Simulerar dialog-bakgrund (scrim)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                ArchiveFileTextPreviewContent(
                    fileName = "README.TXT",
                    content = AnnotatedString("""
                        ABOVE CPM EMULATOR - Version 1.0
                        --------------------------------
                        
                        Detta är en testtext för att se att preview-fönstret
                        fungerar korrekt och att "Close"-knappen syns.
                        
                        Om texten är för lång ska den gå att scrolla, men
                        knapparna ska alltid stanna kvar längst ner i dialogen.
                        
                        Rad 1
                        Rad 2
                        Rad 3
                        Rad 4
                        Rad 5
                        Rad 6
                        Rad 7
                        Rad 8
                        Rad 9
                        Rad 10
                    """.trimIndent()),
                    onClose = {}
                )
            }
        }
    }
}

@Composable
fun ArchiveFileTextPreviewContent(
    fileName: String,
    content: AnnotatedString,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember(fileName, content) { mutableStateOf("") }
    var selectedMatchIndex by remember(fileName, content) { mutableIntStateOf(-1) }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val horizontalMargin = with(LocalDensity.current) { 16.dp.roundToPx() }
    var textLayout by remember(fileName, content) { mutableStateOf<TextLayoutResult?>(null) }
    val matches = remember(content.text, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            buildList {
                var fromIndex = 0
                while (fromIndex < content.text.length) {
                    val matchIndex = content.text.indexOf(searchQuery, fromIndex, ignoreCase = true)
                    if (matchIndex < 0) break
                    add(matchIndex)
                    fromIndex = matchIndex + searchQuery.length
                }
            }
        }
    }
    val displayedContent = remember(content, searchQuery, matches, selectedMatchIndex) {
        buildAnnotatedString {
            append(content)
            matches.getOrNull(selectedMatchIndex)?.let { matchStart ->
                addStyle(
                    SpanStyle(background = Color.Yellow.copy(alpha = 0.55f)),
                    matchStart,
                    matchStart + searchQuery.length
                )
            }
        }
    }
    fun goToNextMatch() {
        if (matches.isNotEmpty()) {
            selectedMatchIndex = (selectedMatchIndex + 1) % matches.size
        }
    }

    LaunchedEffect(searchQuery) {
        selectedMatchIndex = -1
        verticalScrollState.scrollTo(0)
    }

    LaunchedEffect(selectedMatchIndex, matches, textLayout) {
        val matchStart = matches.getOrNull(selectedMatchIndex) ?: return@LaunchedEffect
        val layout = textLayout ?: return@LaunchedEffect
        val line = layout.getLineForOffset(matchStart)
        verticalScrollState.animateScrollTo(layout.getLineTop(line).toInt().coerceAtLeast(0))

        val matchEnd = matchStart + searchQuery.length - 1
        val matchLeft = minOf(
            layout.getBoundingBox(matchStart).left,
            layout.getBoundingBox(matchEnd).left
        ).toInt().coerceAtLeast(0)
        val matchRight = maxOf(
            layout.getBoundingBox(matchStart).right,
            layout.getBoundingBox(matchEnd).right
        ).toInt()
        val visibleLeft = horizontalScrollState.value
        val visibleRight = visibleLeft + horizontalScrollState.viewportSize
        when {
            matchLeft < visibleLeft + horizontalMargin ->
                horizontalScrollState.animateScrollTo((matchLeft - horizontalMargin).coerceAtLeast(0))
            matchRight > visibleRight - horizontalMargin ->
                horizontalScrollState.animateScrollTo(
                    (matchRight - horizontalScrollState.viewportSize + horizontalMargin)
                        .coerceAtLeast(0)
                )
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 450.dp) // Begränsa höjden så Close-knappen syns
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
                    .background(
                        color = Color(0xFFEEEEEE),
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Find in file") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { goToNextMatch() })
                )
                Text(
                    text = "${if (selectedMatchIndex >= 0) selectedMatchIndex + 1 else 0}/${matches.size}",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
                TextButton(
                    onClick = { goToNextMatch() },
                    enabled = matches.isNotEmpty()
                ) {
                    Text("Next")
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(
                    text = displayedContent,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                        .padding(8.dp),
                    onTextLayout = { textLayout = it }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }
        }
    }
}
