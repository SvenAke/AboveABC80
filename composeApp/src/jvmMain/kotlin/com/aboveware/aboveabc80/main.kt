package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import aboveabc80.composeapp.generated.resources.windows_icon
    import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AboveABC80",
        icon = painterResource(Res.drawable.windows_icon)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ABC80_CASE_COLOR)
                .padding(horizontal = 16.dp)
        ) {
            App()
        }
    }
}