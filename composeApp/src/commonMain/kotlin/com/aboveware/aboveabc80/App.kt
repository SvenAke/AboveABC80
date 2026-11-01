package com.aboveware.aboveabc80

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun App() {
    var appLanguage by remember { mutableStateOf(getAppLanguage()) }

    LaunchedEffect(Unit) {
        applyAppLanguage()
    }

    key(appLanguage) {
        MaterialTheme(typography = abc80Typography()) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .systemBarsPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    EmulatorScreen(
                        onLanguageChange = { newLang ->
                            setAppLanguage(newLang)
                            appLanguage = newLang
                        }
                    )
                }
            }
        }
    }
}
