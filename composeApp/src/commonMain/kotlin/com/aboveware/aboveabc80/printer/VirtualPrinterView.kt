/**
 * VirtualPrinterView: Visual presentation of the virtual printer.
 */
package com.aboveware.aboveabc80.printer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.Abc80Log

@Composable
fun VirtualPrinterView(modifier: Modifier = Modifier) {
    val printer = VirtualPrinter.instance
    // Removing the internal isVisible check as it's now handled by the caller (CPMScreen)
    // and adding a log to confirm composition.
    SideEffect {
        Abc80Log.terminal("VirtualPrinterView: Composed")
    }

    val listState = rememberLazyListState()

    LaunchedEffect(printer.lines.size) {
        if (printer.lines.isNotEmpty()) {
            listState.animateScrollToItem(printer.lines.size - 1)
        }
    }

    Box(
        modifier = modifier
            .padding(top = 16.dp, bottom = 16.dp, start = 16.dp)
            .fillMaxWidth(0.7f)
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
                    PrinterManager.currentPrinterType.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Row {
                    IconButton(
                        onClick = { printPrinterBuffer(printer.lines.toList()) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Print, "Print", tint = Color.White)
                    }
                    IconButton(onClick = { printer.formFeed() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Form Feed", tint = Color.White)
                    }
                    IconButton(
                        onClick = { printer.isVisible = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Hide", tint = Color.White)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val linesSnapshot = printer.lines.toList()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(count = linesSnapshot.size) { index ->
                        PrinterLine(linesSnapshot[index], index)
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterLine(line: IntArray, index: Int) {
    val printerType = PrinterManager.currentPrinterType

    val paperBaseColor = when (printerType) {
        PrinterType.DIABLO_630 -> Color(0xFFFFF5E1)
        else -> Color(0xFFF0F0F0) // Solid light gray/white
    }

    val barColor = when (printerType) {
        PrinterType.DIABLO_630 -> Color.Transparent
        else -> Color(0xFFD0E8D0).copy(alpha = 0.5f) // Green bars
    }

    val isBar = (index / 8) % 2 == 0 && barColor != Color.Transparent

    Box(modifier = Modifier.fillMaxWidth().height(2.4.dp).background(paperBaseColor)) {
        if (isBar) Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).background(barColor)
        )

        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            val dotWidth = size.width / line.size.toFloat()
            val dotColor = Color.Black

            // Visual scale for dots
            val visualRadius = 0.8.dp.toPx()

            for (i in line.indices) {
                val pixelVal = line[i]
                if (pixelVal != 0) {
                    val dx = i * dotWidth
                    if (dx > size.width) break

                    val finalColor = if (pixelVal == 2) Color.Red else dotColor

                    if (printerType == PrinterType.DIABLO_630) {
                        drawRect(
                            color = finalColor,
                            topLeft = Offset(dx, 0f),
                            size = Size(maxOf(dotWidth * 40, 2f), size.height)
                        )
                    } else {
                        drawCircle(
                            color = finalColor,
                            radius = visualRadius,
                            center = Offset(dx, size.height / 2)
                        )
                    }
                }
            }
        }
    }
}
