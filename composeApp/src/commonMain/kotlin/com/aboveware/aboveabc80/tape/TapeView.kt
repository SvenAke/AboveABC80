package com.aboveware.aboveabc80.tape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.getCurrentTimestamp
import com.aboveware.aboveabc80.saveLocalFile

@Composable
fun TapeView(modifier: Modifier = Modifier) {
    val tape = TapeController.instance

    if (!tape.isVisible) return

    val listState = rememberLazyListState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveFilename by remember { mutableStateOf("PUNCH") }

    // Auto-scroll to bottom when punching or reading
    val itemCount = if (tape.isPunching) tape.punchedTape.size else tape.readerIndex
    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Box(
        modifier = modifier
            .padding(top = 16.dp, bottom = 16.dp, end = 16.dp)
            .width(120.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (tape.isPunching) "PAPER TAPE PUNCH" else "PAPER TAPE READER",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Row {
                    if (tape.isPunching && tape.punchedTape.isNotEmpty()) {
                        IconButton(onClick = {
                            showSaveDialog = true
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Default.Download,
                                "Download",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = { tape.clear() }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            "Clear",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { tape.isVisible = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Hide",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (tape.isPunching) {
                    val punchedSnapshot = tape.punchedTape.toList()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(count = punchedSnapshot.size) { index ->
                            TapeRow(punchedSnapshot[index], index == punchedSnapshot.size - 1)
                        }
                    }
                } else {
                    val readerSnapshot = tape.readerTape.toList()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(count = readerSnapshot.size) { index ->
                            val isCurrent = index == tape.readerIndex - 1
                            TapeRow(readerSnapshot[index], isCurrent)
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Paper Tape") },
            text = {
                OutlinedTextField(
                    value = saveFilename,
                    onValueChange = { saveFilename = it },
                    label = { Text("Filename Prefix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val data = tape.punchedTape.toByteArray()
                    val timestamp = getCurrentTimestamp()
                    val prefix = saveFilename.trim().ifEmpty { "PUNCH" }
                    val finalName = "${prefix}_$timestamp.TAP"
                    if (saveLocalFile("puncher", finalName, data)) {
                        Abc80Log.terminal("Paper Tape Punch: Saved $finalName in 'puncher' folder")
                        tape.clear() // Remove animation and clear buffer after successful save
                    } else {
                        Abc80Log.wtf("Paper Tape Punch: Failed to save $finalName")
                    }
                    showSaveDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TapeRow(byte: Byte, isHighlighted: Boolean = false) {
    val tapeColor =
        if (isHighlighted) Color(0xFFFFFACD) else Color(0xFFF5F5DC) // Light yellow if highlighted
    val holeColor = Color.Black.copy(alpha = 0.7f)
    val sprocketColor = Color.Black.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(10.dp)
            .background(tapeColor)
            .then(
                if (isHighlighted) Modifier.border(
                    1.dp,
                    Color.Red.copy(alpha = 0.3f)
                ) else Modifier
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = size.width / 10f
            val dotRadius = 3.dp.toPx()
            val sprocketRadius = 1.5.dp.toPx()

            val value = byte.toInt().and(0xFF)

            // Tracks 1, 2, 3
            for (i in 0..2) {
                val bit = (value shr i) and 1
                if (bit == 1) {
                    drawCircle(
                        color = holeColor,
                        radius = dotRadius,
                        center = Offset(trackWidth * (i + 1), size.height / 2)
                    )
                }
            }

            // Sprocket
            drawCircle(
                color = sprocketColor,
                radius = sprocketRadius,
                center = Offset(trackWidth * 4, size.height / 2)
            )

            // Tracks 4, 5, 6, 7, 8
            for (i in 3..7) {
                val bit = (value shr i) and 1
                if (bit == 1) {
                    drawCircle(
                        color = holeColor,
                        radius = dotRadius,
                        center = Offset(trackWidth * (i + 2), size.height / 2)
                    )
                }
            }
        }
    }
}
