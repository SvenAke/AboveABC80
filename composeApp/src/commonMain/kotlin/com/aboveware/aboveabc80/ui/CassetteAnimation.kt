package com.aboveware.aboveabc80.ui

import aboveabc80.composeapp.generated.resources.Res
import aboveabc80.composeapp.generated.resources.cassette_cancel
import aboveabc80.composeapp.generated.resources.cassette_end_of_tape
import aboveabc80.composeapp.generated.resources.cassette_not_found
import aboveabc80.composeapp.generated.resources.cassette_restart_search
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aboveware.aboveabc80.Abc80CassetteStatus
import org.jetbrains.compose.resources.stringResource

@Composable
fun CassetteAnimation(modifier: Modifier = Modifier) {
    val activity = Abc80CassetteStatus.activity
    if (activity == Abc80CassetteStatus.Activity.Idle) return

    Abc80CassetteStatus.missingProgram?.let { filename ->
        AlertDialog(
            onDismissRequest = { Abc80CassetteStatus.cancel() },
            title = { Text(stringResource(Res.string.cassette_end_of_tape)) },
            text = { Text(stringResource(Res.string.cassette_not_found, filename)) },
            confirmButton = {
                TextButton(onClick = { Abc80CassetteStatus.restartSearch() }) {
                    Text(stringResource(Res.string.cassette_restart_search))
                }
            },
            dismissButton = {
                TextButton(onClick = { Abc80CassetteStatus.cancel() }) {
                    Text(stringResource(Res.string.cassette_cancel))
                }
            }
        )
    }

    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(activity) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { elapsedSeconds = (it - start) / 1_000_000_000f }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Cassette(
                elapsedSeconds = elapsedSeconds,
                recording = activity == Abc80CassetteStatus.Activity.Writing
            )
            Text(
                text = Abc80CassetteStatus.text,
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (activity == Abc80CassetteStatus.Activity.Reading) {
                TextButton(onClick = { Abc80CassetteStatus.cancel() }) {
                    Text(stringResource(Res.string.cassette_cancel), color = Color.White)
                }
            }
        }
    }
}

// Adapted from the cassette animation in aboveZXSpectrum.
@Composable
private fun Cassette(elapsedSeconds: Float, recording: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(280.dp, 170.dp)
            .shadow(16.dp, shape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))),
                shape
            )
            .border(3.dp, Color(0xFF3A3A3A), shape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFE6E6E6), RoundedCornerShape(6.dp))
                .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(6.dp))
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(22.dp)) {
                    drawRect(Color(0xFFD32F2F))
                    drawCircle(Color.White, radius = size.minDimension * 0.35f)
                    drawCircle(Color(0xFFD32F2F), radius = size.minDimension * 0.18f)
                }
                Spacer(Modifier.width(6.dp))
                Text("BASF", fontSize = 18.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
            }
            Text("C60", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF333333))
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(210.dp, 70.dp)
                .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                .border(2.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                .shadow(6.dp, RoundedCornerShape(8.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    color = Color(0xFF444444),
                    topLeft = Offset((elapsedSeconds * 90f) % 80f, size.height * 0.45f),
                    size = Size(140f, 6f)
                )
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CassetteWheel((elapsedSeconds * 180f) % 360f)
                CassetteWheel((elapsedSeconds * 210f) % 360f)
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(8.dp)
                    .background(if (recording) Color.Red else Color.DarkGray, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text("REC", color = if (recording) Color.Red else Color.Gray, fontSize = 10.sp)
        }
        CassetteScrew(Modifier.align(Alignment.TopStart))
        CassetteScrew(Modifier.align(Alignment.TopEnd))
        CassetteScrew(Modifier.align(Alignment.BottomStart))
        CassetteScrew(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun CassetteScrew(modifier: Modifier) {
    Box(
        modifier.size(16.dp)
            .background(Color(0xFF555555), CircleShape)
            .border(2.dp, Color.Black, CircleShape)
    )
}

@Composable
private fun CassetteWheel(rotation: Float) {
    Box(
        modifier = Modifier.size(70.dp)
            .background(Color(0xFF222222), CircleShape)
            .border(3.dp, Color.Black, CircleShape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            rotate(rotation) {
                for (i in 0 until 8) {
                    rotate(i * 45f) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(size.width / 2, 6f),
                            end = center,
                            strokeWidth = 4f
                        )
                    }
                }
            }
        }
    }
}
