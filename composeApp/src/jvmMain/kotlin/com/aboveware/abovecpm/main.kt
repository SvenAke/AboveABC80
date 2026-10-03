package com.aboveware.abovecpm

import abovecpm.composeapp.generated.resources.Res
import abovecpm.composeapp.generated.resources.windows_icon
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