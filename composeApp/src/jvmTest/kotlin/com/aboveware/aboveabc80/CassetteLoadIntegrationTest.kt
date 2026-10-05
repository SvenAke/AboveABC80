package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.core.CPU
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.TimeSource

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalResourceApi::class)
class CassetteLoadIntegrationTest {
    @Test
    fun basicLoadsNamedProgramAfterAnotherFileOnTape() = runBlocking {
        val home = Files.createTempDirectory("abc80-cassette-test").toFile()
        val previousHome = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        var native: NativeLib? = null
        val bitDelay = AtomicInteger(-1)
        val unexpectedRecording = AtomicBoolean(false)
        val recordedPulseCount = AtomicInteger(0)
        val writePulseWatcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                if (Abc80CassetteStatus.activity == Abc80CassetteStatus.Activity.Writing) {
                    recordedPulseCount.incrementAndGet()
                }
                return false
            }
        }
        val readBlockBoundaryWatcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                if (Abc80CassetteStatus.activity != Abc80CassetteStatus.Activity.Reading) {
                    unexpectedRecording.set(true)
                }
                return false
            }
        }
        val delayWatcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                if (bitDelay.get() == -1) bitDelay.set(CPU().a)
                return false
            }
        }
        try {
            check(home.resolve(".aboveabc80").resolve("dr").mkdirs())
            val lib = NativeLib.getObject()
            native = lib
            val startupPreference = getPersistedString("tkn80_start", "false")
            assertEquals(startupPreference.toBoolean(), TKN80.enabled)
            assertEquals(64, lib.portRead(3))
            val memory = ByteArray(0x10000)
            Res.readBytes("files/prom.rom").copyInto(memory)
            Res.readBytes("files/dos.rom").copyInto(memory, 0x6000)
            Res.readBytes("files/printer.rom").copyInto(memory, 0x7800)
            lib.copyToMemory(0, memory)
            val other = "10 REM ${"X".repeat(60)}\r".repeat(40)
            val music = "10 PRINT \"MUSIK\"\r" + "20 REM ${"Y".repeat(60)}\r".repeat(30)
            val tape = file("OTHER   BAS", other) + file("MUSIK   BAS", music)
            assertTrue(Abc80Cassette.import(tape.inputStream(), "test.tape"))
            lib.addMemoryReadWatcher(0x05a0, delayWatcher)
            lib.addMemoryReadWatcher(0x0618, readBlockBoundaryWatcher)
            lib.addMemoryReadWatcher(0x0448, writePulseWatcher)
            lib.enableLogging(false)
            lib.startEmulator(3_000_000)
            Thread.sleep(1000)
            "LOAD CAS:MUSIK.BAS\r".forEach { lib.sendKey(it.code) }
            val searchStarted = TimeSource.Monotonic.markNow()
            var foundNameSince: kotlin.time.TimeMark? = null
            var foundNameDuration: Long? = null
            while (searchStarted.elapsedNow().inWholeMilliseconds < 12000) {
                val status = Abc80CassetteStatus.text
                if (status == "Winding (found OTHER.BAS)") {
                    if (foundNameSince == null) foundNameSince = TimeSource.Monotonic.markNow()
                } else if (foundNameSince != null && foundNameDuration == null) {
                    foundNameDuration = foundNameSince.elapsedNow().inWholeMilliseconds
                }
                if (foundNameDuration != null && !Abc80CassetteStatus.visible) break
                Thread.sleep(5)
            }
            assertTrue(
                (foundNameDuration ?: 0) >= 950,
                "The skipped program name must remain visible for about a second: $foundNameDuration ms"
            )
            assertEquals(1, bitDelay.get(), "The CPU must use the patched cassette delay operand")
            assertFalse(Abc80CassetteStatus.visible, "LOAD must finish at normal CPU speed")
            type(lib, "LIST 1-10\r")
            Thread.sleep(1000)
            val output = screen(lib)
            assertTrue(output.contains("10 PRINT") && output.contains("\"MUSIK\""), output)

            "LOAD CAS:MUSIK.BAS\r".forEach { lib.sendKey(it.code) }
            await("End of tape must offer to restart the pending LOAD") {
                Abc80CassetteStatus.missingProgram != null
            }
            assertEquals("MUSIK.BAS", Abc80CassetteStatus.missingProgram)
            Abc80CassetteStatus.restartSearch()
            await("Restart must load the requested program without retyping LOAD") {
                Abc80CassetteStatus.missingProgram == null && !Abc80CassetteStatus.visible
            }
            type(lib, "LIST 1-10\r")
            assertTrue(screen(lib).contains("10 PRINT \"MUSIK\""), screen(lib))

            "LOAD CAS:MISSING.BAS\r".forEach { lib.sendKey(it.code) }
            await("A missing program must offer to restart or cancel") {
                Abc80CassetteStatus.missingProgram != null
            }
            assertEquals("MISSING.BAS", Abc80CassetteStatus.missingProgram)
            Abc80CassetteStatus.cancel()
            await("Cancel must exit the pending cassette LOAD") { !Abc80CassetteStatus.visible }
            assertEquals(null, Abc80CassetteStatus.missingProgram)
            assertFalse(unexpectedRecording.get(), "LOAD must stay in Reading between cassette blocks")
            assertTrue(tape.contentEquals(Abc80Cassette.exportBytes()), "LOAD must not record onto the tape")

            Abc80Cassette.format()
            type(lib, "SAVE CAS:REC.BAS\r")
            await("SAVE must finish writing the tape") { !Abc80CassetteStatus.visible }
            assertTrue(Abc80Cassette.list().any { it.displayName == "REC.BAS" },
                "SAVE must still write a valid cassette file")
            assertTrue(recordedPulseCount.get() > 0, "SAVE must activate the REC indicator")

            type(lib, "PRINT CHR$(23%);\"5j/\";CHR$(22%);\"5\"\r")
            val graphicsRows = decodeABC80ScreenGlyphs(lib.getMemory())
            assertTrue(graphicsRows.any { it.contains("\u00b5\u00ea\u00af 5") },
                "BASIC CHR$(23) must render mosaics and CHR$(22) must restore text")
            lib.setFastSpeed(true)
            for (wide in listOf(true, false)) {
                val width = if (wide) 80 else 40
                val result = if (wide) 128 else 64
                type(lib, "PRINT INP(${if (wide) 4 else 3})\r")
                assertEquals(wide, TKN80.enabled)
                assertTrue(screen(lib).contains(result.toString()), "INP must return $result")
                lib.copyToMemory(TKN80.rowAddress(20, !wide), ByteArray(if (wide) 40 else 80) { 'S'.code.toByte() })
                type(lib, "PRINT CHR$(12);TAB(${width - 1});\"ZX\"\r")
                await("Printing must finish at $width columns") {
                    decodeABC80Screen(lib.getMemory(), wide)[0][width - 1] == 'Z'
                }
                val rows = decodeABC80Screen(lib.getMemory(), wide)
                assertEquals('Z', rows[0][width - 1], "BASIC must write the last column ($width):\n${screen(lib)}")
                assertEquals('X', rows[1][0], "BASIC must wrap at $width columns")
                assertTrue(decodeABC80Screen(lib.getMemory(), !wide).all { it.isBlank() },
                    "Clear-screen must also clear the inactive display buffer")
                type(lib, "NEW\r")
                type(lib, "10 FOR I=1 TO 25\r")
                type(lib, "20 PRINT TAB(${width - 1});\"Q\"\r")
                type(lib, "30 NEXT I\r")
                type(lib, "RUN\r")
                val scrollingStarted = TimeSource.Monotonic.markNow()
                while (scrollingStarted.elapsedNow().inWholeMilliseconds < 12000 &&
                    !decodeABC80Screen(lib.getMemory(), wide).takeLast(4).any { it.startsWith("ABC80") }) {
                    Thread.sleep(10)
                }
                assertTrue(decodeABC80Screen(lib.getMemory(), wide).takeLast(4).any { it.startsWith("ABC80") },
                    "The scrolling program must finish at $width columns, CPU ${CPU(lib.getCPU())}:\n${screen(lib)}")
                assertTrue(decodeABC80Screen(lib.getMemory(), wide).take(2).any { it[width - 1] == 'Q' },
                    "Scrolling must copy all $width columns:\n${screen(lib)}")
            }
            assertEquals(startupPreference, getPersistedString("tkn80_start", "false"),
                "Runtime switching must not overwrite the startup preference")
        } finally {
            Abc80CassetteStatus.cancel()
            native?.freeze()
            native?.setFastSpeed(false)
            native?.let { TKN80.setEnabled(false, it) }
            native?.removeMemoryReadWatcher(0x05a0, delayWatcher)
            native?.removeMemoryReadWatcher(0x0618, readBlockBoundaryWatcher)
            native?.removeMemoryReadWatcher(0x0448, writePulseWatcher)
            Abc80Cassette.eject()
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }

    private fun await(message: String, condition: () -> Boolean) {
        val started = TimeSource.Monotonic.markNow()
        while (!condition() && started.elapsedNow().inWholeMilliseconds < 12000) {
            Thread.sleep(10)
        }
        assertTrue(condition(), message)
    }

    private fun type(lib: NativeLib, command: String) {
        command.forEach { lib.sendKey(it.code) }
        Thread.sleep(command.length * 70L)
    }

    private fun screen(lib: NativeLib) = decodeABC80Screen(lib.getMemory()).joinToString("\n")

    private fun file(name: String, text: String): ByteArray {
        val header = ByteArray(256)
        header[0] = -1
        header[1] = -1
        header[2] = -1
        name.toByteArray().copyInto(header, 3)
        val dataBlocks = (text + "\u0000").toByteArray().asList().chunked(252)
            .mapIndexed { number, bytes ->
                val data = ByteArray(256)
                data[1] = number.toByte()
                data[2] = (number shr 8).toByte()
                bytes.toByteArray().copyInto(data, 3)
                data[255] = 3
                block(data)
            }
        return block(header) + dataBlocks.flatMap { it.asList() }.toByteArray() +
            ByteArray(Abc80Program.Block.BYTES)
    }

    private fun block(data: ByteArray): ByteArray {
        val result = ByteArray(Abc80Program.Block.BYTES)
        byteArrayOf(0x16, 0x16, 0x16, 0x02).copyInto(result, 32)
        data.copyInto(result, 36)
        result[292] = 3
        val checksum = 3 + data.sumOf { it.toInt() and 0xff }
        result[293] = checksum.toByte()
        result[294] = (checksum shr 8).toByte()
        return result
    }
}
