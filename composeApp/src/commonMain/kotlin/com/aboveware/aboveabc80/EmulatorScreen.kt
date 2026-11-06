/**
 * EmulatorScreen: Main screen for the ABC80 emulator.
 *
 * Recent fixes:
 * - Optimized state using mutableIntStateOf for FPS tracking.
 * - Updated delay() calls to use the Duration API.
 * - Fixed clipboard paste logic to avoid suspension issues within lambdas.
 * - Suppressed deprecation for LocalClipboardManager for KMP compatibility.
 * - Cleaned up unused imports, parameters, and improved overall code style.
 */
package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.*
import aboveabc80.composeapp.generated.resources.cassette_title
import aboveabc80.composeapp.generated.resources.cpu_label
import aboveabc80.composeapp.generated.resources.fps_label
import aboveabc80.composeapp.generated.resources.menu
import aboveabc80.composeapp.generated.resources.sound_playback_failed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aboveware.aboveabc80.core.AudioPlayer
import com.aboveware.aboveabc80.core.SoundOutput
import com.aboveware.aboveabc80.core.Abc80Debugger
import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.keyboard.Keyboard
import com.aboveware.aboveabc80.printer.VirtualPrinter
import com.aboveware.aboveabc80.printer.VirtualPrinterView
import com.aboveware.aboveabc80.tape.TapeController
import com.aboveware.aboveabc80.tape.TapeView
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import com.aboveware.aboveabc80.ui.Abc80DebuggerView
import com.aboveware.aboveabc80.ui.CassetteDialog
import com.aboveware.aboveabc80.ui.CassetteAnimation
import com.aboveware.aboveabc80.ui.DiskManagerDialog
import androidx.compose.material.icons.filled.Album
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalResourceApi::class)
private suspend fun restoreDisksForModel(model: ComputerModel, migrateLegacySettings: Boolean = false) {
    check(TerminalManager.computerModel == model) { "Cannot restore disks for an inactive computer model" }
    val controller = DiskController.instance
    var driveZeroLoaded = false
    var driveZeroExplicitlyUnmounted = false
    for (index in 0 until DiskController.MAX_DRIVES) {
        val preferenceKey = DiskController.drivePreferenceKey(model, index)
        val legacyValue = if (migrateLegacySettings) {
            getPersistedString("drive_$index")
        } else ""
        val persistedValue = getPersistedString(preferenceKey)
        val persistence = persistedValue.ifEmpty { legacyValue }
        if (persistence == DiskController.UNMOUNTED_PREFERENCE) {
            if (index == 0) driveZeroExplicitlyUnmounted = true
        } else if (persistence.isNotEmpty()) {
            val source = persistence.substringBefore(":", "LOCAL")
            val diskName = persistence.substringAfter(":", persistence)
            val loaded = if (source == DiskController.Source.DRIVE.name) {
                controller.restoreFromDriveImage(index) != null
            } else {
                controller.restoreFromLocal(index, diskName) != null
            }
            if (!loaded) Abc80Log.wtf("Could not restore $diskName in DR$index for ${model.displayName}")
            if (index == 0) driveZeroLoaded = loaded
            if (loaded && persistedValue.isEmpty() && legacyValue.isNotEmpty()) {
                setPersistedString(preferenceKey, persistence)
            }
        } else {
            val restoredDrive = controller.restoreFromDriveImage(index)
            if (restoredDrive != null) {
                setPersistedString(
                    preferenceKey,
                    "${DiskController.Source.DRIVE.name}:$restoredDrive"
                )
                if (index == 0) driveZeroLoaded = true
            }
        }
        if (migrateLegacySettings && legacyValue.isNotEmpty()) {
            setPersistedString("drive_$index", "")
        }
    }

    if (!driveZeroLoaded && !driveZeroExplicitlyUnmounted) {
        if ("SYSTEM.DSK" !in listLocalDisks()) {
            val image = Res.readBytes("files/system.dsk")
            check(saveLocalDisk("SYSTEM.DSK", image)) { "Could not save SYSTEM.DSK" }
        }
        check(controller.loadFromLocal(0, "SYSTEM.DSK") != null) {
            "Could not mount SYSTEM.DSK for ${model.displayName}"
        }
        setPersistedString(DiskController.drivePreferenceKey(model, 0), "LOCAL:SYSTEM.DSK")
    }
}

@Composable
fun EmulatorScreen(startWithStorageOpen: Boolean = false, onLanguageChange: (String) -> Unit = {}) {
    val assembler = remember { Assembler.instance }
    val nativeLib = remember { NativeLib.getObject() }
    val keyboard = remember { Keyboard.instance }
    val initialModel = TerminalManager.computerModel
    var cpuSpeedSetting by remember {
        mutableIntStateOf(
            getPersistedString("cpu_speed_mhz", initialModel.defaultCpuSpeedSetting.toString())
                .toIntOrNull()?.coerceIn(1, 9) ?: initialModel.defaultCpuSpeedSetting
        )
    }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val audioPlayer = remember { AudioPlayer() }
    var soundVolume by remember {
        mutableIntStateOf(getPersistedString("sound_volume_percent", "100").toIntOrNull()?.coerceIn(0, 400) ?: 100)
    }
    val soundOutput = remember { SoundOutput(audioPlayer.sampleRate, soundVolume) }
    val focusRequester = remember { FocusRequester() }
    val pressedKeys = remember { mutableMapOf<Key, Int>() }
    val screenSelection = remember { ABC80Selection() }
    DisposableEffect(nativeLib) {
        val watcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                if (TerminalManager.computerModel != ComputerModel.ABC80) return false
                nativeLib.commandHistory.atPrompt = true
                nativeLib.dosActive = false
                return false
            }
        }
        nativeLib.addMemoryReadWatcher(0x00db, watcher)
        onDispose { nativeLib.removeMemoryReadWatcher(0x00db, watcher) }
    }

    var showStorageDialog by remember { mutableStateOf(value = startWithStorageOpen) }
    var showSettingsDialog by remember { mutableStateOf(value = false) }
    var showDiskManagerDialog by remember { mutableStateOf(value = false) }
    var showCassetteDialog by remember { mutableStateOf(value = false) }
    var showFontDialog by remember { mutableStateOf(value = false) }
    var showTapeStorageDialog by remember { mutableStateOf(value = false) }

    var cassetteStatusVisible by remember { mutableStateOf(Abc80CassetteStatus.visible) }
    var cassetteStatusText by remember { mutableStateOf(Abc80CassetteStatus.text) }
    var cassetteStatusActivity by remember { mutableStateOf(Abc80CassetteStatus.activity) }
    var cassetteMissingProgram by remember { mutableStateOf(Abc80CassetteStatus.missingProgram) }

    LaunchedEffect(Unit) {
        while (true) {
            cassetteStatusVisible = Abc80CassetteStatus.visible
            cassetteStatusText = Abc80CassetteStatus.text
            cassetteStatusActivity = Abc80CassetteStatus.activity
            cassetteMissingProgram = Abc80CassetteStatus.missingProgram
            delay(50.milliseconds)
        }
    }

    var flashState by remember { mutableStateOf(value = false) }
    var isFastSpeedMode by remember { mutableStateOf(nativeLib.isFastSpeed()) }
    var speedDialOpen by remember { mutableStateOf(value = false) }
    var speedDialTestOpen by remember { mutableStateOf(value = false) }
    var showCharacterSet by remember { mutableStateOf(false) }
    if (showCharacterSet) {
        CharacterSetDialog(onDismiss = { showCharacterSet = false })
    }
    var fps by remember { mutableIntStateOf(0) }
    var currentCpuMHz by remember {
        mutableStateOf(initialModel.cpuSpeedLabel(cpuSpeedSetting))
    }
    var showCpuAndFps by remember {
        mutableStateOf(getPersistedString("show_cpu_and_fps", "true").toBoolean())
    }
    val frameCount = remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val soundPlaybackFailed = stringResource(Res.string.sound_playback_failed)
    val computerModelChangeFailed = stringResource(Res.string.settings_computer_model_change_failed)
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var recallingCommand by remember { mutableStateOf(false) }
    fun recallCommand(code: Int): Boolean {
        if (TerminalManager.computerModel.isAbc800) return false
        if (code != 10 && code != 11) return false
        if (!nativeLib.commandHistory.atPrompt || TerminalManager.activeTerminal.isSetupVisible) return false
        if (recallingCommand) return true
        val current = abc80PromptLine(nativeLib.getMemory()) ?: return false
        val replacement = nativeLib.commandHistory.recall(if (code == 11) -1 else 1, current) ?: return false
        recallingCommand = true
        scope.launch {
            try {
                pasteABC80Text(nativeLib, "\b".repeat(current.length) + replacement)
            } finally {
                recallingCommand = false
            }
        }
        return true
    }
    var pasting by remember { mutableStateOf(false) }
    fun pasteScreenClipboard() {
        if (pasting) return
        val text = clipboardManager.getText()?.text ?: return
        screenSelection.clear()
        pasting = true
        scope.launch {
            try {
                pasteABC80Text(nativeLib, text)
            } catch (e: IllegalArgumentException) {
                Abc80Log.wtf("ABC80 paste failed: ${e.message}")
                snackbarHostState.showSnackbar(requireNotNull(e.message))
            } finally {
                pasting = false
            }
        }
    }

    var showDebugger by remember { mutableStateOf(value = false) }

    // Handle ULA port watching and Program Running watch
    DisposableEffect(Unit) {
        Abc80Debugger.instance.onBreakpointHit = {
            showDebugger = true
        }

        onDispose {
            nativeLib.releaseAllKeys()
            TerminalManager.activeTerminal.onKeyInput = null
            TerminalManager.activeTerminal.disconnect()
        }
    }

    LaunchedEffect(audioPlayer, nativeLib) {
        launch(Dispatchers.IO) {
            try {
                audioPlayer.start()
                while (isActive) {
                    audioPlayer.play(
                        soundOutput.process(
                            samples = nativeLib.readSoundSamples(audioPlayer.sampleRate / 100),
                            activity = Abc80CassetteStatus.activity,
                            cassetteBit = nativeLib.pio.B.cassette
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Abc80Log.terminal("Sound playback failed: ${e.message}")
                scope.launch { snackbarHostState.showSnackbar(soundPlaybackFailed) }
            } finally {
                audioPlayer.stop()
            }
        }
    }

    DisposableEffect(TerminalManager.activeTerminal) {
        val activeTerminal = TerminalManager.activeTerminal
        Abc80Log.keyboard("EmulatorScreen: Attaching keyboard listeners to ${activeTerminal::class.simpleName}")
        activeTerminal.connect()
        val touchKeys = java.util.concurrent.ConcurrentHashMap<String, Int>()
        val touchCode = ThreadLocal<String>()
        activeTerminal.onKeyInput = { char ->
            if (TerminalManager.computerModel.isAbc800) {
                // XML key dispatch already supplies machine bytes, including 8-bit PF codes.
                val code = if (touchCode.get() != null) char.code else abc800mKeyCode(char)
                code?.let(nativeLib::sendKey)
            } else {
                val code = if (char.code in 0..31) char.code else abc80CharCode(char)
                code?.let {
                    if (recallCommand(it)) return@let
                    val source = touchCode.get()
                    if (source != null) {
                        touchKeys[source] = it
                        nativeLib.pressKey(it)
                    } else if (it in touchKeys.values) {
                        nativeLib.pressKey(it)
                    } else {
                        nativeLib.sendKey(it)
                    }
                }
            }
        }
        keyboard.onKeyCodes = { codes, l1, l2, l3, l4, l5, l6 ->
            Abc80Log.keyboard("EmulatorScreen: onKeyCodes received codes='$codes' label='$l1'")
            touchCode.set(codes)
            try {
                activeTerminal.keyboard.handleKeyEvent(codes, l1, l2, l3, l4, l5, l6)
            } finally {
                touchCode.remove()
            }
        }
        keyboard.onKeyUpCodes = { codes ->
            touchKeys.remove(codes)?.let { nativeLib.releaseKey(it) }
            activeTerminal.keyboard.handleKeyRelease(codes)
        }
        keyboard.onFocusRequest = {
            focusRequester.requestFocus()
        }
        keyboard.leds = activeTerminal.keyboard.leds
        keyboard.isRepeatActive = false // Handle repeating in TerminalKeyboard
        keyboard.onCharacter = { char ->
            if (activeTerminal.isSetupVisible) {
                activeTerminal.onKeyEvent(char)
            } else if (TerminalManager.computerModel.isAbc800) {
                abc800mKeyCode(char)?.let(nativeLib::sendKey)
            } else {
                abc80CharCode(char)?.let { if (!recallCommand(it)) nativeLib.pressKey(it) }
            }
            activeTerminal.triggerClick()
        }
        keyboard.onCharacterReleased = { char ->
            if (!TerminalManager.computerModel.isAbc800)
                abc80CharCode(char)?.let { nativeLib.releaseKey(it) }
        }

        activeTerminal.onKeyClick = {
            triggerKeyClick(haptic)
        }

        activeTerminal.onBell = {
            triggerBell(haptic)
        }
        onDispose {
            activeTerminal.onKeyInput = null
            activeTerminal.keyboard.stopRepeating()
            nativeLib.releaseAllKeys()
            pressedKeys.clear()
            keyboard.onCharacter = null
            keyboard.onCharacterReleased = null
            keyboard.onKeyCodes = null
            keyboard.onKeyUpCodes = null
            keyboard.onFocusRequest = null
        }
    }

    LaunchedEffect(speedDialOpen) {
        if (!speedDialOpen) speedDialTestOpen = false
    }

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
            TerminalManager.computerModel.cpuSpeedLabel(cpuSpeedSetting)
        }
        TerminalManager.cpuSpeed = currentCpuMHz

        while (true) {
            delay(100.milliseconds)
            val currentFastMode = nativeLib.isFastSpeed()

            if (currentFastMode != lastFastMode) {
                currentCpuMHz = if (currentFastMode) {
                    "MAX"
                } else {
                    TerminalManager.computerModel.cpuSpeedLabel(cpuSpeedSetting)
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

        while (true) {
            frameCount.intValue++
            delay(10.milliseconds)
        }
    }

    // Load persisted disks on startup
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val terminal = TerminalManager.activeTerminal
        terminal.reset()

        try {
            withContext(Dispatchers.IO) {
                restoreDisksForModel(TerminalManager.computerModel, migrateLegacySettings = true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Abc80Log.wtf("Failed to restore disks: ${e.message}")
        }

        assembler.start()

        // Wait for composition to settle then request focus
        repeat(10) {
            delay(500.milliseconds)
            try {
                focusRequester.requestFocus()
                Abc80Log.keyboard("EmulatorScreen: Focus requested successfully")
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
            Abc80Log.keyboard("EmulatorScreen: Printer hidden, requesting focus back to terminal")
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
                    Abc80Log.keyboard("EmulatorScreen: Column focus changed: ${state.isFocused}")
                    if (!state.isFocused) {
                        pressedKeys.clear()
                        nativeLib.releaseAllKeys()
                    }
                }
                .focusable()
                .pointerInput(Unit) {
                    detectTapGestures {
                        Abc80Log.keyboard("EmulatorScreen: Screen tapped, requesting focus")
                        focusRequester.requestFocus()
                    }
                }
                .onKeyEvent { keyEvent ->
                    if (!keyEvent.isCtrlPressed && !keyEvent.isShiftPressed &&
                        (keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown) &&
                        nativeLib.commandHistory.atPrompt && !TerminalManager.activeTerminal.isSetupVisible
                    ) {
                        if (keyEvent.type == KeyEventType.KeyUp) return@onKeyEvent true
                        if (recallCommand(if (keyEvent.key == Key.DirectionUp) 11 else 10)) {
                            return@onKeyEvent true
                        }
                    }
                    val copy = keyEvent.isCtrlPressed && keyEvent.key == Key.C && screenSelection.range != null
                    if (copy) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            clipboardManager.setText(AnnotatedString(selectedABC80Text(
                                decodeABC80Screen(nativeLib.getMemory()), requireNotNull(screenSelection.range)
                            )))
                        }
                        return@onKeyEvent true
                    }
                    val paste = (keyEvent.isShiftPressed && keyEvent.key == Key.Insert) ||
                        (keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.V)
                    if (paste) {
                        if (keyEvent.type == KeyEventType.KeyDown) pasteScreenClipboard()
                        return@onKeyEvent true
                    }
                    val handled = handleTerminalKeyEvent(keyEvent, pressedKeys)
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
                    .onFocusChanged { state -> isFocused.value = state.isFocused },
                contentAlignment = Alignment.BottomCenter
            ) {
                ABC80Screen(
                    nativeLib = nativeLib,
                    selection = screenSelection,
                    onRequestFocus = { focusRequester.requestFocus() }
                )
                if (screenSelection.range != null) {
                    Row(
                        Modifier.align(Alignment.BottomCenter).background(MaterialTheme.colorScheme.surface)
                    ) {
                        TextButton(onClick = {
                            clipboardManager.setText(AnnotatedString(selectedABC80Text(
                                decodeABC80Screen(nativeLib.getMemory()), requireNotNull(screenSelection.range)
                            )))
                            focusRequester.requestFocus()
                        }) { Text(stringResource(Res.string.screen_copy)) }
                        TextButton(onClick = {
                            pasteScreenClipboard()
                            focusRequester.requestFocus()
                        }, enabled = !pasting) { Text(stringResource(Res.string.menu_paste)) }
                        TextButton(onClick = {
                            screenSelection.clear()
                            focusRequester.requestFocus()
                        }) { Text(stringResource(Res.string.ui_deselect_all)) }
                    }
                }

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
                                cpuSpeedSetting = if (isFastSpeedMode) 9 else (if (cpuSpeedSetting == 9) initialModel.defaultCpuSpeedSetting else cpuSpeedSetting)
                                setPersistedString("cpu_speed_mhz", cpuSpeedSetting.toString())
                                focusRequester.requestFocus()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFastSpeedMode) {
                                    stringResource(Res.string.menu_fast_mode)
                                } else {
                                    stringResource(Res.string.cpu_label)
                                },
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


                    // Cassette indicator: shown while the tape is moving
                    if (!TerminalManager.computerModel.isAbc800 &&
                        cassetteStatusVisible &&
                        cassetteStatusActivity == Abc80CassetteStatus.Activity.Idle) {
                        Surface(
                            color = Color.White.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { showCassetteDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    text = " $cassetteStatusText",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "  ✕",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.clickable { Abc80CassetteStatus.cancel() }
                                )
                            }
                        }
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
                                    contentDescription = stringResource(Res.string.menu_disks_accessibility),
                                    tint = if (isBlinking) Color.Red else Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    mountedDisks.keys.sorted().forEach { driveIndex ->
                                        Text(
                                            text = "DR$driveIndex:",
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

                    if (!TerminalManager.computerModel.isAbc800) {
                        val cassetteButtonDescription = stringResource(Res.string.cassette_title)
                        Surface(
                            color = Color.White.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .semantics { contentDescription = cassetteButtonDescription }
                                .clickable(role = Role.Button) { showCassetteDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Canvas(Modifier.size(16.dp)) {
                                    val ink = Color.Black.copy(alpha = 0.7f)
                                    val outline = Stroke(width = 1.dp.toPx())
                                    drawRect(ink, Offset(0f, size.height * 0.15f),
                                        Size(size.width, size.height * 0.7f), style = outline)
                                    for (x in listOf(0.3f, 0.7f)) {
                                        drawCircle(ink, size.width * 0.12f,
                                            Offset(size.width * x, size.height * 0.42f), style = outline)
                                    }
                                    drawRect(ink, Offset(size.width * 0.25f, size.height * 0.65f),
                                        Size(size.width * 0.5f, size.height * 0.2f), style = outline)
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

                if (!TerminalManager.computerModel.isAbc800) {
                    CassetteAnimation(
                        modifier = Modifier.zIndex(2f),
                        activity = cassetteStatusActivity,
                        text = cassetteStatusText,
                        missingProgram = cassetteMissingProgram
                    )
                }

                if (!TerminalManager.activeTerminal.isSetupVisible && printer.isVisible) {
                    Abc80Log.terminal("EmulatorScreen: Showing VirtualPrinterView (isVisible=${printer.isVisible})")
                    VirtualPrinterView(modifier = Modifier.align(Alignment.CenterStart).zIndex(1f))
                }

                if (TapeController.instance.isVisible) {
                    TapeView(modifier = Modifier.align(Alignment.CenterEnd).zIndex(1f))
                }

                if (showDebugger) {
                    Abc80DebuggerView(modifier = Modifier.align(Alignment.BottomCenter))
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
                                            stringResource(Res.string.menu_restart),
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
                                            assembler.restart()
                                            focusRequester.requestFocus()
                                        },
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ) {
                                        Icon(
                                            Icons.Default.Replay,
                                            contentDescription = stringResource(Res.string.menu_restart)
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
                                            stringResource(Res.string.menu_settings),
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
                                            contentDescription = stringResource(Res.string.menu_settings)
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
                                            stringResource(Res.string.menu_paste),
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
                                            pasteScreenClipboard()
                                            focusRequester.requestFocus()
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Icon(
                                            Icons.Default.ContentPaste,
                                            contentDescription = stringResource(Res.string.menu_paste_terminal)
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
                                            stringResource(Res.string.menu_disks),
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
                                            contentDescription = stringResource(Res.string.disk_manager_title)
                                        )
                                    }
                                }

                                // Cassette
                                if (!TerminalManager.computerModel.isAbc800) {
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
                                                stringResource(Res.string.menu_cassettes),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                        SmallFloatingActionButton(
                                            onClick = {
                                                speedDialOpen = false
                                                showCassetteDialog = true
                                            },
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ) {
                                            Icon(Icons.Default.Album, contentDescription = stringResource(Res.string.menu_cassettes))
                                        }
                                    }
                                }

                                // Test Submenu
                                AnimatedVisibility(
                                    visible = speedDialTestOpen,
                                    enter = fadeIn() + expandVertically() + slideInVertically { it / 2 },
                                    exit = fadeOut() + shrinkVertically() + slideOutVertically { it / 2 }
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
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
                                                    stringResource(Res.string.menu_character_set),
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
                                                    showCharacterSet = true
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Icon(
                                                    Icons.Default.Science,
                                                    contentDescription = stringResource(Res.string.menu_character_set)
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
                                                    stringResource(Res.string.menu_debugger),
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
                                                    contentDescription = stringResource(Res.string.menu_debugger)
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
                                                stringResource(Res.string.menu_test),
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
                                                contentDescription = stringResource(Res.string.menu_test)
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            soundVolume = soundVolume,
            onSoundVolumeChange = {
                soundVolume = it
                soundOutput.volumePercent = it
                setPersistedString("sound_volume_percent", it.toString())
            },
            showCpuAndFps = showCpuAndFps,
            onCpuAndFpsVisibilityChange = {
                showCpuAndFps = it
                setPersistedString("show_cpu_and_fps", it.toString())
            },
            cpuSpeedSetting = if (nativeLib.isFastSpeed()) 9 else cpuSpeedSetting,
            onCpuSpeedChange = {
                cpuSpeedSetting = it
                setPersistedString("cpu_speed_mhz", it.toString())
                val model = TerminalManager.computerModel
                val frequency = model.cpuFrequencyHz(it)
                val fastMode = (it == 9)
                nativeLib.setFastSpeed(fastMode)
                isFastSpeedMode = fastMode
                nativeLib.setCpuFrequency(frequency)
                currentCpuMHz = if (fastMode) "MAX" else model.cpuSpeedLabel(it)
                TerminalManager.cpuSpeed = currentCpuMHz
            },
            onComputerModelChange = { model ->
                scope.launch {
                    val previousModel = TerminalManager.computerModel
                    if (previousModel == model) return@launch
                    val wasFrozen = nativeLib.isFrozen()
                    nativeLib.freeze(true)
                    try {
                        DiskController.instance.closeForComputerModelChange()
                        TerminalManager.computerModel = model
                        assembler.loadRomFiles()
                        withContext(Dispatchers.IO) {
                            restoreDisksForModel(model)
                        }
                        nativeLib.resetCpu()
                    } catch (e: Exception) {
                        TerminalManager.computerModel = previousModel
                        Abc80Log.wtf("Computer model change failed: ${e.message}")
                        var rollbackError: Exception? = null
                        try {
                            DiskController.instance.closeForComputerModelChange()
                            assembler.loadRomFiles()
                            withContext(Dispatchers.IO) {
                                restoreDisksForModel(previousModel)
                            }
                            nativeLib.resetCpu()
                        } catch (restoreError: Exception) {
                            rollbackError = restoreError
                            Abc80Log.wtf("Could not restore disks for ${previousModel.displayName}: ${restoreError.message}")
                        }
                        snackbarHostState.showSnackbar(
                            listOfNotNull(computerModelChangeFailed, e.message, rollbackError?.message)
                                .joinToString(" ")
                        )
                        return@launch
                    } finally {
                        if (!wasFrozen) nativeLib.freeze(false)
                    }
                    cpuSpeedSetting = model.defaultCpuSpeedSetting
                    setPersistedString("cpu_speed_mhz", cpuSpeedSetting.toString())
                    nativeLib.setFastSpeed(false)
                    nativeLib.setCpuFrequency(model.cpuFrequencyHz(cpuSpeedSetting))
                    currentCpuMHz = model.cpuSpeedLabel(cpuSpeedSetting)
                    TerminalManager.cpuSpeed = currentCpuMHz
                }
            },
            onDosRomChange = {
                scope.launch {
                    val wasFrozen = nativeLib.isFrozen()
                    nativeLib.freeze(true)
                    try {
                        assembler.loadRomFiles()
                        nativeLib.resetCpu()
                    } finally {
                        if (!wasFrozen) nativeLib.freeze(false)
                    }
                }
            },
            onLanguageChange = onLanguageChange,
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
        DiskManagerDialog(
            onLoadFile = { drive, fileName ->
                showDiskManagerDialog = false
                focusRequester.requestFocus()
                scope.launch {
                    try {
                        loadABC80DiskFile(nativeLib, drive, fileName, run = false)
                    } catch (e: IllegalArgumentException) {
                        Abc80Log.wtf("Could not load $fileName: ${e.message}")
                        snackbarHostState.showSnackbar(requireNotNull(e.message))
                    } catch (e: IllegalStateException) {
                        Abc80Log.wtf("Could not load $fileName: ${e.message}")
                        snackbarHostState.showSnackbar(requireNotNull(e.message))
                    }
                }
            },
            onRunFile = { drive, fileName ->
                showDiskManagerDialog = false
                focusRequester.requestFocus()
                scope.launch {
                    try {
                        loadABC80DiskFile(nativeLib, drive, fileName, run = true)
                    } catch (e: IllegalArgumentException) {
                        Abc80Log.wtf("Could not run $fileName: ${e.message}")
                        snackbarHostState.showSnackbar(requireNotNull(e.message))
                    } catch (e: IllegalStateException) {
                        Abc80Log.wtf("Could not run $fileName: ${e.message}")
                        snackbarHostState.showSnackbar(requireNotNull(e.message))
                    }
                }
            }
        ) {
            showDiskManagerDialog = false
            focusRequester.requestFocus()
        }
    }

    if (showCassetteDialog && !TerminalManager.computerModel.isAbc800) {
        CassetteDialog(
            onLoadFile = { name ->
                showCassetteDialog = false
                focusRequester.requestFocus()
                scope.launch { pasteABC80Text(nativeLib, "LOAD CAS:$name\r") }
            },
            onRunFile = { name ->
                showCassetteDialog = false
                focusRequester.requestFocus()
                scope.launch {
                    if (Abc80Cassette.list().firstOrNull { it.current }?.executableFormat == Abc80ExecutableFormat.ABS) {
                        try {
                            runABC80AbsoluteFile(nativeLib, "CAS:$name")
                        } catch (e: IllegalStateException) {
                            Abc80Log.wtf("Could not run $name: ${e.message}")
                            snackbarHostState.showSnackbar(requireNotNull(e.message))
                        }
                        return@launch
                    }
                    pasteABC80Text(nativeLib, "LOAD CAS:$name\r")
                    val started = TimeSource.Monotonic.markNow()
                    while (!Abc80CassetteStatus.visible && started.elapsedNow().inWholeMilliseconds < 3000) delay(20)
                    while (Abc80CassetteStatus.visible) delay(50)
                    delay(300)
                    pasteABC80Text(nativeLib, "RUN\r")
                }
            }
        ) {
            showCassetteDialog = false
            focusRequester.requestFocus()
        }
    }

    if (showTapeStorageDialog) {
        LocalFileDialog(
            folder = "puncher",
            title = stringResource(Res.string.load_paper_tape),
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
        title = { Text(stringResource(Res.string.font_select_title)) },
        text = {
            Column {
                TextButton(
                    onClick = { onFontSelected("DEFAULT") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.font_standard_ascii))
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
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ui_cancel)) }
        }
    )
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadFont(fontFile: String) {
    val terminal = TerminalManager.activeTerminal
    if (fontFile == "DEFAULT") {
        "\u001B(B".forEach { terminal.putChar(it) }
        terminal.putChar('\u000F') // SI (G0 -> GL)
        Abc80Log.terminal("EmulatorScreen: Reset to standard font")
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

        "\u001B(B".forEach { terminal.putChar(it) }
        terminal.putChar('\u000F') // SI (G0 into GL)

        Abc80Log.terminal("EmulatorScreen: Font $fontFile loaded and designated to G0")
    } catch (e: Exception) {
        Abc80Log.wtf("EmulatorScreen: Failed to load font $fontFile: ${e.message}")
    }
}

private fun handleTerminalKeyEvent(
    event: KeyEvent,
    pressedKeys: MutableMap<Key, Int>
): Boolean {
    val type = event.type
    val key = event.key
    val isCtrl = event.isCtrlPressed
    val isShift = event.isShiftPressed
    var charValue = event.char
    val activeTerminal = TerminalManager.activeTerminal
    if (type == KeyEventType.KeyUp) {
        pressedKeys.remove(key)?.let {
            NativeLib.getObject().releaseKey(it)
            return true
        }
    }

    if (!activeTerminal.isSetupVisible) {
        if (key == Key.CapsLock) {
            if (type == KeyEventType.KeyDown && getRepeatCount(event) == 0) {
                activeTerminal.keyboard.leds["upper_case"]?.let {
                    it.isOn = !it.isOn
                    Abc80Log.keyboard("Physical Caps Lock toggled Upper Case LED: ${it.isOn}")
                }
            }
            return true
        }
        if (!isShift && !isCtrl && charValue?.isUpperCase() == true) {
            activeTerminal.keyboard.leds["upper_case"]?.isOn = true
        }
        val upperCase = activeTerminal.keyboard.leds["upper_case"]?.isOn == true
        if (TerminalManager.computerModel.isAbc800) {
            if (key == Key.Enter || key == Key.NumPadEnter) charValue = '\r'
            val effectiveChar = if (upperCase && !isShift) charValue?.uppercaseChar() else charValue
            val code = abc800PhysicalKeyCode(key, effectiveChar, isShift, isCtrl)
            if (code != null) {
                if (shouldForwardABC800MKey(type == KeyEventType.KeyDown, getRepeatCount(event))) {
                    NativeLib.getObject().sendKey(code)
                }
                return true
            }
        }
        val code = abc80KeyCode(key, charValue, isShift || upperCase, isCtrl)
        if (code != null) {
            if (type == KeyEventType.KeyDown) {
                pressedKeys[key] = code
                if (code == 3) {
                    NativeLib.getObject().sendKey(3)
                } else {
                    NativeLib.getObject().pressKey(code)
                }
            }
            return true
        }
    }
    if (key == Key.Enter || key == Key.NumPadEnter) {
        charValue = '\r'
    } else if (charValue == '\n') {
        return true // Skip redundant LF
    }

    Abc80Log.keyboard("EmulatorScreen: handleTerminalKeyEvent activeTerminal=${activeTerminal::class.simpleName} type=$type, key=$key, char='$charValue', ctrl=$isCtrl, shift=$isShift")

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
        Abc80Log.keyboard("EmulatorScreen: Physical key handled by terminal keyboard")
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
            Abc80Log.keyboard("EmulatorScreen: sending char to terminal: char=${char.code} ('$char')")
            activeTerminal.onKeyEvent(char)
            activeTerminal.triggerClick()
        }
        return true
    }
    return false
}

internal fun shouldForwardABC800MKey(isKeyDown: Boolean, repeatCount: Int): Boolean =
    isKeyDown && repeatCount == 0


@Composable
expect fun PlatformKeyboard(keyboard: Keyboard, modifier: Modifier = Modifier)

expect fun getRepeatCount(event: KeyEvent): Int

expect fun getEventTime(event: KeyEvent): Long

expect fun triggerKeyClick(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback)

expect fun triggerBell(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback)

expect fun createPlatformBitmap(width: Int, height: Int): Any
expect fun Any.toImageBitmap(): ImageBitmap
