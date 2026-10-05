package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import aboveabc80.composeapp.generated.resources.tkn80_start
import aboveabc80.composeapp.generated.resources.tkn80_restart
import org.jetbrains.compose.resources.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.core.StateManager
import com.aboveware.aboveabc80.printer.Diablo630ConfigView
import com.aboveware.aboveabc80.printer.Mx80ConfigView
import com.aboveware.aboveabc80.printer.PrinterManager
import com.aboveware.aboveabc80.printer.PrinterType
import com.aboveware.aboveabc80.terminal.TerminalColor
import com.aboveware.aboveabc80.terminal.TerminalManager
import com.aboveware.aboveabc80.terminal.TerminalType

@Composable
fun SettingsDialog(
    showCpuAndFps: Boolean,
    onCpuAndFpsVisibilityChange: (Boolean) -> Unit,
    cpuSpeedSetting: Int,
    onCpuSpeedChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showMx80Config by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var startWide by remember { mutableStateOf(getPersistedString("tkn80_start", "false").toBoolean()) }

    if (showMx80Config) {
        AlertDialog(
            onDismissRequest = { showMx80Config = false },
            confirmButton = {
                TextButton(onClick = { showMx80Config = false }) {
                    Text("Close", color = Color.White)
                }
            },
            title = {
                Text(
                    "${PrinterManager.currentPrinterType.displayName} Configuration",
                    color = Color.White
                )
            },
            text = {
                if (PrinterManager.currentPrinterType == PrinterType.DIABLO_630) {
                    Diablo630ConfigView()
                } else {
                    Mx80ConfigView()
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Snapshot Management
                Text(
                    "System Snapshots",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { StateManager.saveSnapshot("default") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save State")
                    }
                    OutlinedButton(
                        onClick = { StateManager.loadSnapshot("default") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Load State")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 2. Terminal Emulation
                Text(
                    "Terminal Emulation",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Column(modifier = Modifier.selectableGroup()) {
                    TerminalType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (TerminalManager.currentTerminalType == type),
                                    onClick = { TerminalManager.currentTerminalType = type },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (TerminalManager.currentTerminalType == type),
                                onClick = null
                            )
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 2. Terminal Color
                Text(
                    "Terminal Color",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(modifier = Modifier.selectableGroup()) {
                    TerminalColor.entries.forEach { colorType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (TerminalManager.terminalColor == colorType),
                                    onClick = { TerminalManager.terminalColor = colorType },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (TerminalManager.terminalColor == colorType),
                                onClick = null
                            )
                            Text(
                                text = colorType.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 3. Printer Selection
                Text(
                    "Printer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Column(modifier = Modifier.selectableGroup()) {
                    PrinterType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (PrinterManager.currentPrinterType == type),
                                    onClick = { PrinterManager.currentPrinterType = type },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (PrinterManager.currentPrinterType == type),
                                onClick = null
                            )
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp).weight(1f)
                            )
                            if (type == PrinterType.EPSON_MX80 || type == PrinterType.EPSON_FX80 || type == PrinterType.DIABLO_630) {
                                IconButton(onClick = { showMx80Config = true }) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = "Configure Printer"
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 3. Scanline Intensity
                Text(
                    "CRT Scanline Intensity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = TerminalManager.scanlineIntensity,
                    onValueChange = { TerminalManager.scanlineIntensity = it },
                    valueRange = 0f..0.5f,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = startWide,
                        onCheckedChange = {
                            startWide = it
                            setPersistedString("tkn80_start", it.toString())
                        }
                    )
                    Text(stringResource(Res.string.tkn80_start))
                }
                Text(stringResource(Res.string.tkn80_restart), style = MaterialTheme.typography.bodySmall)

                Text(
                    "CPU Speed",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = cpuSpeedSetting.toFloat(),
                        onValueChange = { onCpuSpeedChange(it.toInt()) },
                        valueRange = 1f..9f,
                        steps = 7,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (cpuSpeedSetting == 9) "MAX" else "$cpuSpeedSetting MHz",
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 4. Misc Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            onCpuAndFpsVisibilityChange(!showCpuAndFps)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showCpuAndFps,
                        onCheckedChange = onCpuAndFpsVisibilityChange
                    )
                    Text(
                        text = "Show CPU and FPS",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            TerminalManager.autoUppercase = !TerminalManager.autoUppercase
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = TerminalManager.autoUppercase,
                        onCheckedChange = { TerminalManager.autoUppercase = it }
                    )
                    Text(
                        text = "Auto Uppercase",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                OutlinedButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear saved settings")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear saved settings?") },
            text = {
                Text("This removes all saved application settings on this device. Disk images are not affected.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearPersistedSettings()
                        TerminalManager.resetToDefaults()
                        PrinterManager.resetToDefaults()
                        showClearConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
