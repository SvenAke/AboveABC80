package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import aboveabc80.composeapp.generated.resources.abc80
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font

@Composable
fun abc80FontFamily() = FontFamily(Font(Res.font.abc80))

@Composable
fun abc80Typography(): Typography {
    val font = abc80FontFamily()
    val defaults = Typography()
    return Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = font),
        displayMedium = defaults.displayMedium.copy(fontFamily = font),
        displaySmall = defaults.displaySmall.copy(fontFamily = font),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = font),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = font),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = font),
        titleLarge = defaults.titleLarge.copy(fontFamily = font),
        titleMedium = defaults.titleMedium.copy(fontFamily = font),
        titleSmall = defaults.titleSmall.copy(fontFamily = font),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = font),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = font),
        bodySmall = defaults.bodySmall.copy(fontFamily = font),
        labelLarge = defaults.labelLarge.copy(fontFamily = font),
        labelMedium = defaults.labelMedium.copy(fontFamily = font),
        labelSmall = defaults.labelSmall.copy(fontFamily = font)
    )
}
