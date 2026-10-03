package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import aboveabc80.composeapp.generated.resources.windows_icon
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "above CP/M",
        icon = painterResource(Res.drawable.windows_icon)
    ) {
        App()
    }
}