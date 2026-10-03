/**
 * CPMScreen: Main screen for the AboveCPM emulator and terminal interface.
 *
 * Recent fixes:
 * - Optimized state using mutableIntStateOf for FPS tracking.
 * - Updated delay() calls to use the Duration API.
 * - Fixed clipboard paste logic to avoid suspension issues within lambdas.
 * - Suppressed deprecation for LocalClipboardManager for KMP compatibility.
 * - Cleaned up unused imports, parameters, and improved overall code style.
 */
package com.aboveware.abovecpm

import abovecpm.composeapp.generated.resources.Res
import abovecpm.composeapp.generated.resources.cpu_label
import abovecpm.composeapp.generated.resources.fps_label
import abovecpm.composeapp.generated.resources.menu
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aboveware.abovecpm.core.Audio
import com.aboveware.abovecpm.core.AudioPlayer
import com.aboveware.abovecpm.core.Beeper
import com.aboveware.abovecpm.core.BIOS
import com.aboveware.abovecpm.core.CpmDebugger
import com.aboveware.abovecpm.core.DiskController
import com.aboveware.abovecpm.keyboard.Keyboard
import com.aboveware.abovecpm.printer.VirtualPrinter
import com.aboveware.abovecpm.printer.VirtualPrinterView
import com.aboveware.abovecpm.tape.TapeController
import com.aboveware.abovecpm.tape.TapeView
import com.aboveware.abovecpm.terminal.CharacterSet
import com.aboveware.abovecpm.terminal.TerminalManager
import com.aboveware.abovecpm.terminal.TerminalType
import com.aboveware.abovecpm.terminal.TerminalView
import com.aboveware.abovecpm.ui.CpmDebuggerView
import com.aboveware.abovecpm.ui.DiskManagerDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CPMScreen(startWithStorageOpen: Boolean = false) {
    val assembler = remember { Assembler.instance }
    val nativeLib = remember { NativeLib.getObject() }
    val keyboard = remember { Keyboard.instance }
    var cpuSpeedSetting by remember {
        mutableIntStateOf(
            getPersistedString("cpu_speed_mhz", (Constants.CPU_FREQUENCY / 1_000_000).toString())
                .toIntOrNull()?.coerceIn(1, 9) ?: (Constants.CPU_FREQUENCY / 1_000_000)
        )
    }
    val beeper = remember {
        Beeper(cpuFrequency = cpuSpeedSetting.coerceAtMost(8) * 1_000_000)
    }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val audioPlayer = remember { AudioPlayer() }
    val focusRequester = remember { FocusRequester() }

    var showStorageDialog by remember { mutableStateOf(value = startWithStorageOpen) }
    var showSettingsDialog by remember { mutableStateOf(value = false) }
    var showDiskManagerDialog by remember { mutableStateOf(value = false) }
    var showFontDialog by remember { mutableStateOf(value = false) }
    var showTapeStorageDialog by remember { mutableStateOf(value = false) }

    var flashState by remember { mutableStateOf(value = false) }
    var isFastSpeedMode by remember { mutableStateOf(nativeLib.isFastSpeed()) }
    var speedDialOpen by remember { mutableStateOf(value = false) }
    var speedDialTestOpen by remember { mutableStateOf(value = false) }
    var fps by remember { mutableIntStateOf(0) }
    var currentCpuMHz by remember {
        mutableStateOf(if (cpuSpeedSetting == 9) "MAX" else "$cpuSpeedSetting.0 MHz")
    }
    var showCpuAndFps by remember {
        mutableStateOf(getPersistedString("show_cpu_and_fps", "true").toBoolean())
    }
    val frameCount = remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var showDebugger by remember { mutableStateOf(value = false) }

    // Handle ULA port watching and Program Running watch
    DisposableEffect(Unit) {
        CpmDebugger.instance.init(scope)
        CpmDebugger.instance.onBreakpointHit = {
            showDebugger = true
        }

        audioPlayer.start()
        Audio.instance.setOnEarChangedListener { level, tStates ->
            beeper.onEarChanged(level, tStates.toLong())
        }

        onDispose {
            TerminalManager.activeTerminal.disconnect()
            audioPlayer.stop()
            Audio.instance.setOnEarChangedListener(null)
        }
    }

    LaunchedEffect(TerminalManager.activeTerminal, TerminalManager.autoUppercase) {
        val activeTerminal = TerminalManager.activeTerminal
        ZXLog.keyboard("CPMScreen: Attaching keyboard listeners to ${activeTerminal::class.simpleName}")
        activeTerminal.connect()
        keyboard.onKeyCodes = { codes, l1, l2, l3, l4, l5, l6 ->
            ZXLog.keyboard("CPMScreen: onKeyCodes received codes='$codes' label='$l1'")
            activeTerminal.keyboard.handleKeyEvent(codes, l1, l2, l3, l4, l5, l6)
        }
        keyboard.onKeyUpCodes = { codes ->
            activeTerminal.keyboard.handleKeyRelease(codes)
        }
        keyboard.onFocusRequest = {
            focusRequester.requestFocus()
        }
        keyboard.leds = activeTerminal.keyboard.leds
        keyboard.isRepeatActive = false // Handle repeating in TerminalKeyboard
        keyboard.onCharacter = { char ->
            activeTerminal.onKeyEvent(char)
            activeTerminal.triggerClick()
        }

        activeTerminal.onKeyClick = {
            triggerKeyClick(haptic)
        }

        activeTerminal.onBell = {
            triggerBell(haptic)
        }
    }

    LaunchedEffect(speedDialOpen) {
        if (!speedDialOpen) speedDialTestOpen = false
    }

    // Register for LOAD and SAVE events
    assembler.AsmFileReader()

    // Setup Tape Controller callbacks
    LaunchedEffect(Unit) {
        TapeController.instance.onRequireTape = {
            if (TapeController.instance.readerTape.isEmpty() && !showTapeStorageDialog) {
                showTapeStorageDialog = true
            }
        }
    }

    LaunchedEffect(showTapeStorageDialog) {
        if (showTapeStorageDialog) {
            nativeLib.freeze(true)
        } else {
            nativeLib.freeze(false)
        }
    }

    // Flash timer toggling every 320ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(320.milliseconds)
            flashState = !flashState
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000.milliseconds)
            fps = frameCount.intValue
            frameCount.intValue = 0
        }
    }

    LaunchedEffect(Unit) {
        var lastFastMode = nativeLib.isFastSpeed()
        currentCpuMHz = if (lastFastMode) {
            "MAX"
        } else {
            "${cpuSpeedSetting.coerceAtMost(8)}.0 MHz"
        }
        TerminalManager.cpuSpeed = currentCpuMHz

        while (true) {
            delay(100.milliseconds)
            val currentFastMode = nativeLib.isFastSpeed()

            if (currentFastMode != lastFastMode) {
                currentCpuMHz = if (currentFastMode) {
                    "MAX"
                } else {
                    "${cpuSpeedSetting.coerceAtMost(8)}.0 MHz"
                }
                TerminalManager.cpuSpeed = currentCpuMHz
                lastFastMode = currentFastMode
            }
        }
    }

    LaunchedEffect(Unit) {
        yield()
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
            delay(100.milliseconds)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }

        beeper.reset(nativeLib.getTStates())

        // Audio update loop: move to a separate coroutine to avoid blocking the UI
        launch(Dispatchers.Default) {
            while (true) {
                val currentTStates = nativeLib.getTStates()
                val samples = beeper.getPendingSamples(currentTStates)
                if (samples.isNotEmpty()) {
                    audioPlayer.play(samples)
                }
                delay(10.milliseconds)
            }
        }

        while (true) {
            frameCount.intValue++
            delay(10.milliseconds)
        }
    }

    // Load persisted disks on startup
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        CharacterSet.load()
        val terminal = TerminalManager.activeTerminal
        terminal.reset() // Force a full reset to refresh character sets

        var aLoaded = false
        for (i in 0 until DiskController.MAX_DRIVES) {
            val persistence = getPersistedString("drive_$i")
            if (persistence.isNotEmpty()) {
                val diskName =
                    if (persistence.contains(":")) persistence.substringAfter(":") else persistence
                DiskController.instance.loadFromLocal(i, diskName)
                if (i == 0) aLoaded = true
            }
        }

        // Auto-create SYSTEM.DSK if Drive A is still empty
        if (!aLoaded) {
            val localDisks = listLocalDisks()
            if (!localDisks.contains("SYSTEM.DSK")) {
                try {
                    @OptIn(ExperimentalResourceApi::class)
                    val data = Res.readBytes("files/CMP22.DSK")
                    saveLocalDisk("SYSTEM.DSK", data)
                } catch (e: Exception) {
                    ZXLog.wtf("Failed to create SYSTEM.DSK: ${e.message}")
                }
            }
            DiskController.instance.loadFromLocal(0, "SYSTEM.DSK")
            setPersistedString("drive_0", "LOCAL:SYSTEM.DSK")
        }

        // Wait for composition to settle then request focus
        repeat(10) {
            delay(500.milliseconds)
            try {
                focusRequester.requestFocus()
                ZXLog.keyboard("CPMScreen: Focus requested successfully")
            } catch (e: Exception) {
                // Ignore, will retry
            }
        }
    }

    // Auto-request focus when printer is hidden
    val printer = remember { VirtualPrinter.instance }
    var isFirstPrinterCheck by remember { mutableStateOf(true) }
    LaunchedEffect(printer.isVisible) {
        if (!printer.isVisible && !isFirstPrinterCheck) {
            focusRequester.requestFocus()
            ZXLog.keyboard("CPMScreen: Printer hidden, requesting focus back to terminal")
        }
        isFirstPrinterCheck = false
    }

    Scaffold(
        containerColor = TerminalManager.activeTerminal.backgroundColor,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .padding(12.dp)
                        .border(
                            4.dp,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.shapes.extraSmall,
                        ),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(data.visuals.message)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalManager.activeTerminal.backgroundColor)
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = padding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
                .imePadding()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    ZXLog.keyboard("CPMScreen: Column focus changed: ${state.isFocused}")
                }
                .focusable()
                .pointerInput(Unit) {
                    detectTapGestures {
                        ZXLog.keyboard("CPMScreen: Screen tapped, requesting focus")
                        focusRequester.requestFocus()
                    }
                }
                .onKeyEvent { keyEvent ->
                    val handled = handleTerminalKeyEvent(keyEvent, scope, clipboardManager)
                    if (handled) return@onKeyEvent true

                    // Fallback for F3 if handleTerminalKeyEvent failed for some reason
                    if (keyEvent.key == Key.F3 && keyEvent.type == KeyEventType.KeyDown) {
                        TerminalManager.activeTerminal.toggleSetup()
                        return@onKeyEvent true
                    }
                    false
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Container for screen and border
            val isFocused = remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onFocusChanged { state -> isFocused.value = state.isFocused }
                    .border(if (isFocused.value) 2.dp else 0.dp, Color.Green.copy(alpha = 0.5f))
                    .padding(if (isFocused.value) 2.dp else 0.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                TerminalView()

                if (showCpuAndFps) Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .clickable {
                                isFastSpeedMode = !isFastSpeedMode
                                nativeLib.setFastSpeed(isFastSpeedMode)
                            focusRequester.requestFocus()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFastSpeedMode) "FAST " else stringResource(Res.string.cpu_label),
                                color = Color.Red.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = currentCpuMHz,
                                color = Color.Red.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compose LED
                    if (TerminalManager.currentTerminalType == TerminalType.VT320) {
                        val composeLedOn = keyboard.leds["Compose"]?.isOn ?: false
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(10.dp)
                                .background(
                                    color = if (composeLedOn) Color(0xFF32CD32) else Color.Black.copy(
                                        alpha = 0.1f
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                        )
                    }

                    // Disk Indicator
                    val mountedDisks = DiskController.instance.mountedDisks
                    if (mountedDisks.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { showDiskManagerDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                val lastAccess = DiskController.instance.lastAccessTime
                                var isBlinking by remember { mutableStateOf(false) }

                                LaunchedEffect(lastAccess) {
                                    if (lastAccess > 0) {
                                        isBlinking = true
                                        delay(100.milliseconds)
                                        isBlinking = false
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Disks",
                                    tint = if (isBlinking) Color.Red else Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    mountedDisks.keys.sorted().forEach { driveIndex ->
                                        Text(
                                            text = ('A' + driveIndex).toString(),
                                            color = if (
                                                isBlinking &&
                                                driveIndex == DiskController.instance.lastAccessDriveIndex
                                            ) {
                                                Color.Red
                                            } else {
                                                Color.Black
                                            },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showCpuAndFps) Surface(
                        color = Color.White.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(horizontal = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.fps_label),
                                color = Color.Blue.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = fps.toString(),
                                color = Color.Blue.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (!TerminalManager.activeTerminal.isSetupVisible && printer.isVisible) {
                    ZXLog.terminal("CPMScreen: Showing VirtualPrinterView (isVisible=${printer.isVisible})")
                    VirtualPrinterView(modifier = Modifier.align(Alignment.CenterStart).zIndex(1f))
                }

                if (TapeController.instance.isVisible) {
                    TapeView(modifier = Modifier.align(Alignment.CenterEnd).zIndex(1f))
                }

                if (showDebugger) {
                    CpmDebuggerView(modifier = Modifier.align(Alignment.BottomCenter))
                }

                // Floating Action Button Menu (Above Keyboard)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        AnimatedVisibility(
                            visible = speedDialOpen,
                            enter = fadeIn() + expandVertically() + slideInVertically { it / 2 },
                            exit = fadeOut() + shrinkVertically() + slideOutVertically { it / 2 }
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                // Restart System
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            "Restart",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    SmallFloatingActionButton(
                                        onClick = {
                                            speedDialOpen = false
                                            BIOS.instance.restart()
                                            focusRequester.requestFocus()
                                        },
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ) {
                                        Icon(
                                            Icons.Default.Replay,
                                            contentDescription = "Restart"
                                        )
                                    }
                                }

                                // Settings
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            "Settings",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    SmallFloatingActionButton(
                                        onClick = {
                                            speedDialOpen = false
                                            showSettingsDialog = true
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                }

                                // Paste
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            "Paste",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    SmallFloatingActionButton(
                                        onClick = {
                                            speedDialOpen = false
                                            clipboardManager.getText()?.text?.let { text ->
                                                val activeTerminal = TerminalManager.activeTerminal
                                                val sanitized = text.replace("\r\n", "\r").replace("\n", "\r")
                                                scope.launch {
                                                    for (char in sanitized) {
                                                        activeTerminal.onKeyEvent(char)
                                                        activeTerminal.triggerClick()
                                                        delay(10.milliseconds)
                                                    }
                                                }
                                            }
                                            focusRequester.requestFocus()
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Icon(
                                            Icons.Default.ContentPaste,
                                            contentDescription = "Paste to Terminal"
                                        )
                                    }
                                }

                                // Disk Manager
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            "Floppies...",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    SmallFloatingActionButton(
                                        onClick = {
                                            speedDialOpen = false
                                            showDiskManagerDialog = true
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Icon(


                                            Icons.Default.Storage,
                                            contentDescription = "Disk Manager"
                                        )
                                    }
                                }

                                // Test Submenu
                                AnimatedVisibility(
                                    visible = speedDialTestOpen,
                                    enter = fadeIn() + expandVertically() + slideInVertically { it / 2 },
                                    exit = fadeOut() + shrinkVertically() + slideOutVertically { it / 2 }
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        // Action: Test Printer
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    "Test Printer",
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    speedDialOpen = false
                                                    scope.launch {
                                                        val command = "PIP LST:=BOOT.PRN\r\n"
                                                        for (char in command) {
                                                            Keyboard.instance.onKeyEvent(char)
                                                            delay(10.milliseconds)
                                                        }
                                                    }
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Icon(
                                                    Icons.Default.Print,
                                                    contentDescription = "Test Printer"
                                                )
                                            }
                                        }

                                        // Action: Test ESC/P
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    "Test ESC/P",
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    speedDialOpen = false
                                                    scope.launch {
                                                        val printer =
                                                            VirtualPrinter.instance
                                                        printer.activeTestData.forEach { line ->
                                                            line.forEach { char ->
                                                                printer.printChar(char)
                                                                // Small delay to prevent blocking UI and allow audio to play
                                                                if (char.code == 0x07) delay(200.milliseconds)
                                                                else if (char.code == 0x0A || char.code == 0x0D) delay(
                                                                    10.milliseconds
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Icon(
                                                    Icons.Default.Description,
                                                    contentDescription = "Test ESC/P"
                                                )
                                            }
                                        }

                                        // Action: Test Paste to Terminal
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    "Test Paste",
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    speedDialOpen = false
                                                    clipboardManager.getText()?.text?.let { text ->
                                                        val activeTerminal = TerminalManager.activeTerminal
                                                        val sanitized = text.replace("\r\n", "\r").replace("\n", "\r")
                                                        scope.launch {
                                                            for (char in sanitized) {
                                                                activeTerminal.onKeyEvent(char)
                                                                activeTerminal.triggerClick()
                                                                delay(10.milliseconds)
                                                            }
                                                        }
                                                    }
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentPaste,
                                                    contentDescription = "Paste to Terminal"
                                                )
                                            }
                                        }

                                        // Action: Test Load Font
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    "Test Load Font",
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    speedDialOpen = false
                                                    showFontDialog = true
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Icon(
                                                    Icons.Default.FontDownload,
                                                    contentDescription = "Load Font"
                                                )
                                            }
                                        }

                                        // Reset Terminal (Startup Test)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    "Reset Terminal",
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    speedDialOpen = false
                                                    TerminalManager.activeTerminal.reset()
                                                },
                                                containerColor = Color.Red,
                                                contentColor = Color.White
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Reset Terminal"
                                                )
                                            }
                                        }

                                        // Debugger Toggle
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    "Debugger",
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    speedDialOpen = false
                                                    showDebugger = !showDebugger
                                                },
                                                containerColor = if (showDebugger) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = if (showDebugger) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Icon(
                                                    Icons.Default.BugReport,
                                                    contentDescription = "Debugger"
                                                )
                                            }
                                        }
                                    }
                                }

                                if (Constants.SHOW_TEST_FAB) {
                                    // Test Toggle Button
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 4.dp,
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Text(
                                                "Test",
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                ),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                        SmallFloatingActionButton(
                                            onClick = {
                                                speedDialTestOpen = !speedDialTestOpen
                                            },
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ) {
                                            Icon(
                                                if (speedDialTestOpen) Icons.Default.ExpandMore else Icons.Default.Science,
                                                contentDescription = "Test"
                                            )
                                        }
                                    }
                                }

                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                speedDialOpen = !speedDialOpen
                                if (!speedDialOpen) {
                                    focusRequester.requestFocus()
                                }
                            },
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                if (speedDialOpen) Icons.Default.Close
                                else Icons.Default.Menu,
                                contentDescription = stringResource(Res.string.menu)
                            )
                        }
                    }
                }
            }

            key(TerminalManager.activeTerminal.keyboardXmlResId) {
                PlatformKeyboard(keyboard, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            showCpuAndFps = showCpuAndFps,
            onCpuAndFpsVisibilityChange = {
                showCpuAndFps = it
                setPersistedString("show_cpu_and_fps", it.toString())
            },
            cpuSpeedSetting = cpuSpeedSetting,
            onCpuSpeedChange = {
                cpuSpeedSetting = it
                setPersistedString("cpu_speed_mhz", it.toString())
                val frequency = it.coerceAtMost(8) * 1_000_000
                nativeLib.setFastSpeed(it == 9)
                nativeLib.setCpuFrequency(frequency)
                beeper.setCpuFrequency(frequency)
                currentCpuMHz = if (it == 9) "MAX" else "$it.0 MHz"
                TerminalManager.cpuSpeed = currentCpuMHz
            },
            onDismiss = {
                showCpuAndFps = getPersistedString("show_cpu_and_fps", "true").toBoolean()
                showSettingsDialog = false
                focusRequester.requestFocus()
            }
        )
    }

    if (showStorageDialog) {
        StorageFileDialog(
            onDismiss = {
                showStorageDialog = false
                focusRequester.requestFocus()
            }
        )
    }

    if (showDiskManagerDialog) {
        DiskManagerDialog {
            showDiskManagerDialog = false
            focusRequester.requestFocus()
        }
    }

    if (showTapeStorageDialog) {
        LocalFileDialog(
            folder = "puncher",
            title = "Load Paper Tape",
            onDismiss = {
                showTapeStorageDialog = false
                TapeController.instance.isWaitingForTape = false // Stop blocking if cancelled
                focusRequester.requestFocus()
            },
            onFileSelected = { _, data ->
                showTapeStorageDialog = false
                TapeController.instance.loadReaderTape(data)
                focusRequester.requestFocus()
            }
        )
    }

    if (showFontDialog) {
        FontSelectionDialog(
            onDismiss = {
                showFontDialog = false
                focusRequester.requestFocus()
            },
            onFontSelected = { fontFile ->
                showFontDialog = false
                scope.launch {
                    loadFont(fontFile)
                    focusRequester.requestFocus()
                }
            }
        )
    }
}

@Composable
fun FontSelectionDialog(onDismiss: () -> Unit, onFontSelected: (String) -> Unit) {
    val fonts = listOf(
        "birger.fnt", "birger2.fnt", "boothill.fnt", "future.fnt",
        "gothic.fnt", "jetpac.fnt", "mercy.fnt", "powerslave.fnt"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Font") },
        text = {
            Column {
                TextButton(
                    onClick = { onFontSelected("DEFAULT") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Standard ASCII")
                }
                HorizontalDivider()
                fonts.forEach { font ->
                    TextButton(
                        onClick = { onFontSelected(font) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(font.removeSuffix(".fnt").replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadFont(fontFile: String) {
    val terminal = TerminalManager.activeTerminal
    if (fontFile == "DEFAULT") {
        "\u001B(B".forEach { terminal.putChar(it) }
        terminal.putChar('\u000F') // SI (G0 -> GL)
        ZXLog.terminal("CPMScreen: Reset to standard font")
        return
    }

    try {
        val bytes = Res.readBytes("files/$fontFile")

        // Scan for existing DCS header and ST footer
        var i = 0
        // Skip leading ESC P or 0x90
        if (i + 1 < bytes.size && bytes[i].toInt() == 0x1B && bytes[i + 1].toInt() == 0x50) {
            i += 2
        } else if (i < bytes.size && (bytes[i].toInt() and 0xFF) == 0x90) {
            i += 1
        }

        // Skip potential ST at the end to avoid double-terminating
        var endLimit = bytes.size
        if (endLimit >= 2 && bytes[endLimit - 2].toInt() == 0x1B && bytes[endLimit - 1].toInt() == 0x5C) {
            endLimit -= 2
        } else if (endLimit >= 1 && (bytes[endLimit - 1].toInt() and 0xFF) == 0x9C) {
            endLimit -= 1
        }

        // Start our own clean 7-bit DCS
        terminal.putChar('\u001B')
        terminal.putChar('P')

        while (i < endLimit) {
            val u = bytes[i].toInt() and 0xFF
            // Translate internal 8-bit ST (0x9C) to 7-bit ESC \ just in case
            if (u == 0x9C) {
                terminal.putChar('\u001B')
                terminal.putChar('\\')
            } else if (u == 0x90) { // Translate internal 8-bit DCS (0x90) to 7-bit ESC P
                terminal.putChar('\u001B')
                terminal.putChar('P')
            } else {
                terminal.putChar(u.toChar())
            }
            i++
        }

        // Send 7-bit end: ESC \
        terminal.putChar('\u001B')
        terminal.putChar('\\')

        // Wait a bit for the parser to finish
        delay(50.milliseconds)

        // Designate DRCS as G0 and invoke GL
        if (terminal is com.aboveware.abovecpm.terminal.VT320) {
            val designator = terminal.graphics.characterSets.drcsFontBuffer.designator()
            ZXLog.terminal("CPMScreen: DRCS Designator detected as '$designator'")
            if (designator.isNotEmpty()) {
                "\u001B($designator".forEach { terminal.putChar(it) }
            } else {
                "\u001B(B".forEach { terminal.putChar(it) }
            }
        } else {
            "\u001B(B".forEach { terminal.putChar(it) }
        }
        terminal.putChar('\u000F') // SI (G0 into GL)

        ZXLog.terminal("CPMScreen: Font $fontFile loaded and designated to G0")
    } catch (e: Exception) {
        ZXLog.wtf("CPMScreen: Failed to load font $fontFile: ${e.message}")
    }
}

private fun handleTerminalKeyEvent(
    event: KeyEvent,
    scope: CoroutineScope,
    clipboardManager: ClipboardManager
): Boolean {
    val type = event.type
    val key = event.key
    val isCtrl = event.isCtrlPressed
    val isShift = event.isShiftPressed
    var charValue = event.char
    val activeTerminal = TerminalManager.activeTerminal

    // Check for paste shortcut (Ctrl+V or Shift+Insert)
    if (type == KeyEventType.KeyDown && ((isCtrl && key == Key.V) || (isShift && key == Key.Insert))) {
        clipboardManager.getText()?.text?.let { text ->
            val activeTerminal = TerminalManager.activeTerminal
            val sanitized = text.replace("\r\n", "\r").replace("\n", "\r")
            scope.launch {
                for (char in sanitized) {
                    activeTerminal.onKeyEvent(char)
                    activeTerminal.triggerClick()
                    delay(10.milliseconds)
                }
            }
        }
        return true
    }

    if (type == KeyEventType.KeyDown &&
        !isCtrl &&
        !isShift &&
        !activeTerminal.isSetupVisible &&
        !BIOS.instance.isTransientProgramRunning
    ) {
        val direction = when (key) {
            Key.DirectionUp -> -1
            Key.DirectionDown -> 1
            else -> 0
        }
        if (direction != 0 && TerminalManager.commandHistory.navigate(direction) {
                activeTerminal.onKeyEvent(it)
                activeTerminal.triggerClick()
            }
        ) {
            return true
        }
    }

    if (key == Key.Enter || key == Key.NumPadEnter) {
        charValue = '\r'
    } else if (charValue == '\n') {
        return true // Skip redundant LF
    }

    ZXLog.keyboard("CPMScreen: handleTerminalKeyEvent activeTerminal=${activeTerminal::class.simpleName} type=$type, key=$key, char='$charValue', ctrl=$isCtrl, shift=$isShift")

    val isRepeat = getRepeatCount(event) > 0
    val timeMillis = getEventTime(event)

    if (activeTerminal.keyboard.handlePhysicalKeyEvent(
            key,
            type,
            isCtrl,
            isShift,
            isRepeat,
            timeMillis
        )
    ) {
        ZXLog.keyboard("CPMScreen: Physical key handled by terminal keyboard")
        return true
    }

    val char = when (key) {
        else -> {
            var c = charValue
            if (c != null) {
                c = activeTerminal.keyboard.transformChar(c, isShift)
            }

            if (c != null && isCtrl) {
                when (val code = c.code) {
                    in 97..122 -> (code - 96).toChar() // a-z
                    in 65..90 -> (code - 64).toChar() // A-Z
                    else -> {
                        when (c) {
                            '[' -> '\u001B' // ESC
                            ']' -> '\u001D' // GS
                            '\\' -> '\u001C' // FS
                            '^' -> '\u001E' // RS (Home)
                            '_' -> '\u001F' // US
                            else -> c
                        }
                    }
                }
            } else {
                c
            }
        }
    }

    if (char != null && char.code != 0) {
        // Only handle KeyDown for character input to avoid double letters on Desktop
        if (type == KeyEventType.KeyDown) {
            ZXLog.keyboard("CPMScreen: sending char to terminal: char=${char.code} ('$char')")
            activeTerminal.onKeyEvent(char)
            activeTerminal.triggerClick()
        }
        return true
    }
    return false
}


@Composable
expect fun PlatformKeyboard(keyboard: Keyboard, modifier: Modifier = Modifier)

expect fun getRepeatCount(event: KeyEvent): Int

expect fun getEventTime(event: KeyEvent): Long

expect fun triggerKeyClick(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback)

expect fun triggerBell(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback)

expect fun createPlatformBitmap(width: Int, height: Int): Any
expect fun Any.toImageBitmap(): ImageBitmap
