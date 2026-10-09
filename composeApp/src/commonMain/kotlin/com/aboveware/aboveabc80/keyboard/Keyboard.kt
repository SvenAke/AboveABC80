package com.aboveware.aboveabc80.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aboveware.aboveabc80.Assembler
import com.aboveware.aboveabc80.NativeLib
import com.aboveware.aboveabc80.Abc80Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore
import kotlin.time.Duration.Companion.milliseconds

class KeyboardLed(val id: String) {
    var isOn by mutableStateOf(false)
}

enum class Keys(val value: UByte) {
    KeyCapsShift(0x00u), KeyZ(0x01u), KeyX(0x02u), KeyC(0x03u), KeyV(0x04u),
    KeyA(0x10u), KeyS(0x11u), KeyD(0x12u), KeyF(0x13u), KeyG(0x14u),
    KeyQ(0x20u), KeyW(0x21u), KeyE(0x22u), KeyR(0x23u), KeyT(0x24u),
    Key1(0x30u), Key2(0x31u), Key3(0x32u), Key4(0x33u), Key5(0x34u),
    Key0(0x40u), Key9(0x41u), Key8(0x42u), Key7(0x43u), Key6(0x44u),
    KeyP(0x50u), KeyO(0x51u), KeyI(0x52u), KeyU(0x53u), KeyY(0x54u),
    KeyEnter(0x60u), KeyL(0x61u), KeyK(0x62u), KeyJ(0x63u), KeyH(0x64u),
    KeySpace(0x70u), KeySymbolShift(0x71u), KeyM(0x72u), KeyN(0x73u), KeyB(0x74u),
    KeyNone(0xffu);

    fun row(): Int = (value.toInt() and 0b1111_0000) shr 4
    fun bit(): Int = (1 shl (value.toInt() and 0b0000_1111))

    fun bit(customBit: UByte): Int = (1 shl customBit.toInt())
}

@OptIn(ExperimentalUnsignedTypes::class)
class Keyboard {

    companion object {
        val instance = Keyboard()
    }

    //    Keyboard_Map:
    //      Bit     0   1   2   3   4
    //      DB &FE,"#","Z","X","C","V"
    //      DB &FD,"A","S","D","F","G"
    //      DB &FB,"Q","W","E","R","T"
    //      DB &F7,"1","2","3","4","5"
    //      DB &EF,"0","9","8","7","6"
    //      DB &DF,"P","O","I","U","Y"
    //      DB &BF,"#","L","K","J","H"
    //      DB &7F," ","#","M","N","B"
    //
    //    Bits are set to 0 for any key that is pressed and 1 for any key
    //    that is not pressed. Multiple key presses can be read simultaneously.

    private val lock = Any()
    private val keys = Array(8) { 0xFF.toUByte() }

    var onCharacter: ((Char) -> Unit)? = null
    var onCharacterReleased: ((Char) -> Unit)? = null
    var onKeyCodes: ((String, String?, String?, String?, String?, String?, String?) -> Unit)? = null
    var onKeyUpCodes: ((String) -> Unit)? = null
    var isRepeatActive = false
    private var repeatingJob: kotlinx.coroutines.Job? = null

    var leds by mutableStateOf(emptyMap<String, KeyboardLed>())

    var onFocusRequest: (() -> Unit)? = null
    fun requestTerminalFocus() {
        onFocusRequest?.invoke()
    }

    fun keysShiftZXCV() = synchronized(lock) { keys[0] }
    fun keysASDFG() = synchronized(lock) { keys[1] }
    fun keysQWERT() = synchronized(lock) { keys[2] }
    fun keys12345() = synchronized(lock) { keys[3] }
    fun keys09876() = synchronized(lock) { keys[4] }
    fun keysPOIUY() = synchronized(lock) { keys[5] }
    fun keysEnterLKJH() = synchronized(lock) { keys[6] }
    fun keySpaceSymShiftMNB() = synchronized(lock) { keys[7] }

    fun reset() {
        synchronized(lock) {
            for (i in 0..7) keys[i] = 0xFF.toUByte()
        }
    }

    fun readPort(hi: Int): Int {
        var result = 0xFF
        synchronized(lock) {
            for (i in 0..7) {
                if ((hi shr i) and 1 == 0) {
                    result = result and keys[i].toInt()
                }
            }
        }
        return result or 0xE0
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun pressBreak() = synchronized(lock) {
        onKeyDown(Keys.KeyCapsShift)
        onKeyDown(Keys.KeySpace)
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000.milliseconds)
            onKeyUp(Keys.KeySpace)
            onKeyUp(Keys.KeyCapsShift)
        }
    }

    override fun toString() = StringBuilder().apply {
        synchronized(lock) {
            keys.forEach {
                append(it.toBinary().takeLast(5)).append(",")
            }
        }
    }.toString()

    fun isCapsShiftPressed() = synchronized(lock) {
        (keys[0].toInt() and Keys.KeyCapsShift.bit()).toUInt() == 0x00u
    }

    fun isSymbolShiftPressed() = synchronized(lock) {
        (keys[7].toInt() and Keys.KeySymbolShift.bit()).toUInt() == 0x00u
    }

    fun onKeyDown(key: Keys) {
        synchronized(lock) {
            if (key != Keys.KeyNone) {
                if (isKeyPressed(key)) return
                keys[key.row()] = (keys[key.row()].toInt() and key.bit().inv()).toUByte()
            }
            dumpKey("onKeyDown", key.toString())
        }
    }

    private fun dumpKey(event: String, vararg components: String?) {
        val keyStr = components.filter { !it.isNullOrEmpty() }.joinToString(",")
        Abc80Log.keyboard("$event: $keyStr, shifted? ${isCapsShiftPressed()} symbolShifted? ${isSymbolShiftPressed()}")
    }

    fun onKeyUp(key: Keys) {
        synchronized(lock) {
            if (key != Keys.KeyNone) {
                if (!isKeyPressed(key)) return
                keys[key.row()] = (keys[key.row()].toInt() or key.bit()).toUByte()
            }
            dumpKey("onKeyUp", key.toString())
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onKeyEvent(char: Char) {
        onCharacter?.invoke(char)
        if (isRepeatActive) {
            repeatingJob?.cancel()
            repeatingJob = CoroutineScope(Dispatchers.Default).launch {
                delay(400.milliseconds) // Initial delay
                while (true) {
                    onCharacter?.invoke(char)
                    delay(50.milliseconds) // Repeat rate
                }
            }
        }
    }

    fun onKeyReleaseEvent(char: Char) {
        onCharacterReleased?.invoke(char)
        repeatingJob?.cancel()
        repeatingJob = null
    }

    fun onKeyDown(
        codes: String,
        label: String? = null,
        secondLabel: String? = null,
        thirdLabel: String? = null,
        fourthLabel: String? = null,
        fifthLabel: String? = null,
        sixthLabel: String? = null
    ) {
        synchronized(lock) {
            val parts = codes.split(",")
            if (parts.size == 2) {
                onKeyCodes?.invoke(
                    codes,
                    label,
                    secondLabel,
                    thirdLabel,
                    fourthLabel,
                    fifthLabel,
                    sixthLabel
                )
                val row = parts[0].toInt()
                val bitValue = parts[1].toUByte()
                val bit = (1 shl bitValue.toInt())
                if ((keys[row].toInt() and bit) == 0) return
                keys[row] = (keys[row].toInt() and bit.inv()).toUByte()
                dumpKey("onKeyDown", codes)
            } else if (codes.isNotEmpty()) {
                // For non-matrix codes, invoke the callback
                onKeyCodes?.invoke(
                    codes,
                    label,
                    secondLabel,
                    thirdLabel,
                    fourthLabel,
                    fifthLabel,
                    sixthLabel
                )

                if (onKeyCodes == null) codes.toIntOrNull()?.let { ascii ->
                    Abc80Log.keyboard("Keyboard: codes ascii $ascii")
                    onCharacter?.invoke(ascii.toChar())
                    dumpKey("onKeyDown Character", codes)
                }
            }
        }
    }

    fun onKeyUp(
        codes: String,
        label: String? = null,
        secondLabel: String? = null,
        thirdLabel: String? = null,
        fourthLabel: String? = null,
        fifthLabel: String? = null,
        sixthLabel: String? = null
    ) {
        synchronized(lock) {
            if (codes.isNotEmpty()) {
                onKeyUpCodes?.invoke(codes)
            }

            val parts = codes.split(",")
            if (parts.size == 2) {
                val row = parts[0].toInt()
                val bitValue = parts[1].toUByte()
                val bit = (1 shl bitValue.toInt())
                if ((keys[row].toInt() and bit) != 0) return
                keys[row] = (keys[row].toInt() or bit).toUByte()
                dumpKey("onKeyUp", codes)
            } else if (codes.isNotEmpty()) {
                // For ASCII/Characters, we log it for consistency.
                dumpKey(
                    "onKeyUp Character",
                    codes,
                    label,
                    secondLabel,
                    thirdLabel,
                    fourthLabel,
                    fifthLabel,
                    sixthLabel
                )
            }
        }
    }

    fun isKeyPressed(key: Keys) = synchronized(lock) {
        (keys[key.row()].toInt() and key.bit()).toUInt() == 0x00u
    }

    fun injectKeyDown(key: Keys) {
        onKeyDown(key)
        scan()
    }

    fun injectKeyUp(key: Keys) {
        onKeyUp(key)
        scan()
    }

    fun injectKey(key: Keys) {
        injectKeyDown(key)
        injectKeyUp(key)
    }

    fun injectKey(modifier: Keys, key: Keys) {
        onKeyDown(modifier)
        injectKeyDown(key)
        onKeyUp(key)
        onKeyUp(modifier)
        scan()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun scan() {
        if (!Assembler.instance.labels.isDefined("KEYBOARD")) return

        repeat(2) {
            NativeLib.getObject().apply {
                val scanSemaphore = Semaphore(0)
                val watcher = object : NativeLib.MemoryReadWatcher {
                    override fun onRead(address: Int): Boolean {
                        if (Assembler.instance.labels.isDefined("KEY_SCANNED")) {
                            addMemoryReadWatcher(
                                "KEY_SCANNED",
                                object : NativeLib.MemoryReadWatcher {
                                    override fun onRead(address: Int): Boolean {
                                        scanSemaphore.release()
                                        return true
                                    }
                                })
                        } else {
                            scanSemaphore.release()
                        }
                        return true
                    }
                }
                addMemoryReadWatcher("KEYBOARD", watcher)
                try {
                    // Use a timeout to avoid hanging during LOAD or other long operations
                    if (!scanSemaphore.tryAcquire(50, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        removeMemoryReadWatcher("KEYBOARD", watcher)
                    }
                } catch (e: Exception) {
                    removeMemoryReadWatcher("KEYBOARD", watcher)
                }
            }
        }
    }
}

private fun UByte.toBinary(): String {
    return this.toInt().toString(2).padStart(8, '0')
}
