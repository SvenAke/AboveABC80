package com.aboveware.aboveabc80

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal fun characterChartCode(group: Int, row: Int): Int {
    require(group in 0..3 && row in 0..23)
    return 32 + group * 24 + row
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun CharacterSetDialog(onDismiss: () -> Unit) {
    val map = remember { Abc80MonitorCharacterMap() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ABC80 character set") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        text = {
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(0.74f).background(Color.White)
            ) {
                val groupWidth = size.width / 4
                val rowHeight = size.height / 25
                val px = groupWidth / (14 * map.charWidth)
                val py = rowHeight * 0.85f / map.charHeight
                fun glyph(code: Int, column: Float, row: Int, group: Int) {
                    map.forEach(code) { bits, y ->
                        map.forEachBit(bits.toInt()) { on, x ->
                            if (on) drawRect(
                                Color.Black,
                                Offset(
                                    group * groupWidth + (column * map.charWidth + x) * px,
                                    row * rowHeight + rowHeight * 0.075f + y * py
                                ),
                                Size(px, py)
                            )
                        }
                    }
                }
                fun label(text: String, column: Float, row: Int, group: Int) {
                    text.forEachIndexed { index, char ->
                        glyph(char.code, column + index, row, group)
                    }
                }
                for (group in 0..3) {
                    label("Kod", 0.5f, 0, group)
                    label("T", 5f, 0, group)
                    label("G", 11f, 0, group)
                    for (row in 0..23) {
                        val code = characterChartCode(group, row)
                        label(code.toString().padStart(3), 0.5f, row + 1, group)
                        if (code == 32) {
                            label("Blank", 4.5f, row + 1, group)
                        } else {
                            glyph(code, 5f, row + 1, group)
                        }
                        glyph(code or 0x80, 11f, row + 1, group)
                    }
                    if (group > 0) {
                        drawLine(
                            Color.Black,
                            Offset(group * groupWidth, rowHeight),
                            Offset(group * groupWidth, size.height)
                        )
                    }
                }
                drawLine(Color.Black, Offset(0f, rowHeight), Offset(size.width, rowHeight))
            }
        }
    )
}
