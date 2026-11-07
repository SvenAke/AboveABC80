package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ABC800SoundTest {
    @Test
    fun firmwareProducesPiezoSoundOnBothModels() = runBlocking {
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val home = Files.createTempDirectory("abc800-sound").toFile()
        val lib = NativeLib.getObject()
        lib.freeze(true)
        try {
            System.setProperty("user.home", home.absolutePath)
            DiskController.instance.closeForComputerModelChange()
            lib.setFastSpeed(false)
            for (model in listOf(ComputerModel.ABC800M, ComputerModel.ABC800C)) {
                TerminalManager.computerModel = model
                Assembler.instance.loadRomFiles()
                val count = AtomicInteger()
                val watcher = object : NativeLib.MemoryReadWatcher {
                    override fun onRead(address: Int): Boolean {
                        count.incrementAndGet()
                        return false
                    }
                }
                val bellAddress = if (model == ComputerModel.ABC800M) 0x022f else 0x0208
                lib.addMemoryReadWatcher(bellAddress, watcher)
                try {
                    lib.startEmulator(3_580_000)
                    delay(2000)
                    lib.readSoundSamples(4410)
                    count.set(0)
                    lib.sendKey('A'.code)
                    delay(60)
                    val click = lib.readSoundSamples(4410)
                    assertTrue(click.max() - click.min() > 0.1f, "$model key click was silent")
                    lib.sendKey(8)
                    delay(100)
                    val command = "PRINT CHR$(7)"
                    for (ch in command) {
                        lib.sendKey(ch.code)
                        delay(35)
                    }
                    delay(100)
                    lib.readSoundSamples(4410)
                    count.set(0)
                    lib.sendKey('\r'.code)
                    var min = 0f
                    var max = 0f
                    repeat(100) {
                        val samples = lib.readSoundSamples(441)
                        min = minOf(min, samples.min())
                        max = maxOf(max, samples.max())
                        delay(10)
                    }
                    assertTrue(count.get() == 200, "$model BASIC bell did not toggle the piezo: ${count.get()}")
                    assertTrue(min < -0.01f && max > 0.01f, "$model piezo output was silent")
                    lib.freeze(true)
                    assertTrue(lib.readSoundSamples(441).all { it == 0f })
                    lib.setFastSpeed(true)
                    lib.freeze(false)
                    delay(40)
                    assertTrue(lib.readSoundSamples(441).all { it == 0f })
                    lib.freeze(true)
                    lib.setFastSpeed(false)

                    lib.copyToMemory(0, byteArrayOf(0xc3.toByte(), 0, 0x80.toByte()))
                    lib.copyToMemory(0x8000, byteArrayOf(
                        0xf3.toByte(), 0x3e, 3, 0xd3.toByte(), 6, 0x18, 0xfa.toByte()
                    ))
                    lib.startEmulator(3_580_000)
                    delay(120)
                    val noSn = lib.readSoundSamples(4410)
                    assertTrue(noSn.max() - noSn.min() < 0.001f, "$model OUT 6 must not drive the SN76477")
                    lib.freeze(true)
                    Assembler.instance.loadRomFiles()
                    lib.copyToMemory(0, byteArrayOf(0xc3.toByte(), 0, 0x80.toByte()))
                    lib.copyToMemory(0x8000, byteArrayOf(
                        0xf3.toByte(),
                        0xdb.toByte(), 0x05, 0x32, 0x00, 0xa0.toByte(),
                        0x06, 0x70, 0x10, 0xfe.toByte(),
                        0xdb.toByte(), 0x1d, 0x32, 0x01, 0xa0.toByte(),
                        0x06, 0x70, 0x10, 0xfe.toByte(),
                        0x18, 0xec.toByte()
                    ))
                    lib.startEmulator(3_580_000)
                    delay(120)
                    assertEquals(0xff, lib.peek(0xa000), "$model port 05 read")
                    assertEquals(0xff, lib.peek(0xa001), "$model mirrored port 1d read")
                    val samples = lib.readSoundSamples(4410)
                    assertTrue(samples.max() - samples.min() > 0.1f, "$model mirrored reads must toggle piezo")
                    lib.freeze(true)
                } finally {
                    lib.removeMemoryReadWatcher(bellAddress, watcher)
                }
            }
        } finally {
            lib.freeze(true)
            DiskController.instance.closeForComputerModelChange()
            System.setProperty("user.home", previousHome)
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
            lib.freeze(false)
            home.deleteRecursively()
        }
    }
}
