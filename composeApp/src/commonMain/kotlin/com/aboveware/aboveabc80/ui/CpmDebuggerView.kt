package com.aboveware.aboveabc80.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aboveware.aboveabc80.core.CpmDebugger
import com.aboveware.aboveabc80.toHex

@Composable
fun CpmDebuggerView(modifier: Modifier = Modifier) {
    val debugger = CpmDebugger.instance
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentLineIndex = remember(debugger.currentPc) {
        debugger.getLineForPc(debugger.currentPc)
    }

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex != -1) {
            listState.animateScrollToItem(maxOf(0, currentLineIndex - 5))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CP/M Debugger (PC: ${debugger.currentPc.toHex()})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { debugger.togglePause() }) {
                    Icon(
                        imageVector = if (debugger.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause/Resume",
                    )
                }

                IconButton(
                    onClick = { debugger.step() },
                    enabled = debugger.isPaused
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Step"
                    )
                }
            }

            // Source View
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                itemsIndexed(debugger.lines) { index, line ->
                    val isCurrentLine = index == currentLineIndex
                    val isBreakpoint =
                        if (line.address != -1) debugger.isBreakpoint(line.address) else false

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCurrentLine) Color(0xFF333300) else Color.Transparent)
                            .clickable(enabled = line.address != -1) {
                                debugger.toggleBreakpoint(line.address)
                            }
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Breakpoint Indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isBreakpoint) Color.Red else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(Modifier.width(4.dp))

                        Text(
                            text = if (line.address != -1) line.address.toHex() else "    ",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.width(45.dp)
                        )

                        Text(
                            text = line.hex.padEnd(8),
                            color = Color(0xFF00AA00),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.width(70.dp)
                        )

                        Text(
                            text = line.code,
                            color = if (isCurrentLine) Color.Yellow else Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
