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
import kotlin.test.assertFailsWith
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
            while (searchStarted.elapsedNow().inWholeMilliseconds < 30000) {
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
            assertFalse(Abc80CassetteStatus.visible,
                "LOAD must finish at normal CPU speed: ${Abc80CassetteStatus.text}, ${CPU(lib.getCPU())}")
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
            lib.setFastSpeed(false)
            assertFailsWith<IllegalArgumentException> { lib.readSoundSamples(0) }
            assertFailsWith<IllegalArgumentException> { lib.readSoundSamples(4411) }
            val soundPort = AtomicInteger(-1)
            val soundOutWatcher = object : NativeLib.MemoryReadWatcher {
                override fun onRead(address: Int): Boolean {
                    val cpu = CPU(lib.getCPU())
                    if (cpu.bc and 0xff == 6) soundPort.set(cpu.l)
                    return false
                }
            }
            val bellWatcher = object : NativeLib.MemoryReadWatcher {
                override fun onRead(address: Int): Boolean {
                    soundPort.set(0x83)
                    return false
                }
            }
            lib.addMemoryReadWatcher(0x213b, soundOutWatcher)
            lib.addMemoryReadWatcher(0x01f2, bellWatcher)
            try {
                assertSound(lib, soundPort, 3, "OUT 6,3\r", "BASIC OUT must produce a VCO tone")
                assertSound(lib, soundPort, 11, "OUT 6,11\r", "BASIC OUT must produce noise")
                muteSound(lib, soundPort)
                assertSound(lib, soundPort, 0x83, "PRINT CHR$(7)\r", "The BASIC ROM bell must produce sound")
                assertMusicMachineCode(lib)
                System.getenv("ABC80_MUSIC_TAPE")?.let { path ->
                    verifyInstalledMusic(lib, java.io.File(path).readBytes())
                }
                lib.setFastSpeed(true)
                assertTrue(lib.readSoundSamples(441).all { it == 0.0f }, "MAX speed must mute audio")
                lib.setFastSpeed(false)
                lib.freeze()
                assertTrue(lib.readSoundSamples(441).all { it == 0.0f }, "A frozen emulator must mute audio")
            } finally {
                lib.removeMemoryReadWatcher(0x213b, soundOutWatcher)
                lib.removeMemoryReadWatcher(0x01f2, bellWatcher)
            }
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
        val accepted = AtomicBoolean(false)
        val completed = AtomicBoolean(false)
        val inputWatcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                accepted.set(true)
                return false
            }
        }
        val promptWatcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                if (accepted.get()) completed.set(true)
                return false
            }
        }
        lib.addMemoryReadWatcher(0x02e5, inputWatcher)
        lib.addMemoryReadWatcher(0x00ed, promptWatcher)
        try {
            command.forEach { lib.sendKey(it.code) }
            await("BASIC must return to its prompt after $command") { completed.get() }
        } finally {
            lib.removeMemoryReadWatcher(0x02e5, inputWatcher)
            lib.removeMemoryReadWatcher(0x00ed, promptWatcher)
        }
    }

    private fun muteSound(lib: NativeLib, soundPort: AtomicInteger) {
        soundPort.set(-1)
        "OUT 6,0\r".forEach { lib.sendKey(it.code) }
        await("BASIC must disable sound before the next test") { soundPort.get() == 0 }
        lib.readSoundSamples(4410)
        Thread.sleep(120)
        assertTrue(lib.readSoundSamples(4410).all { it == 0.0f }, "Each sound test must start silent")
    }

    private fun assertSound(lib: NativeLib, soundPort: AtomicInteger, expectedPort: Int, command: String, message: String) {
        muteSound(lib, soundPort)
        soundPort.set(-1)
        command.forEach { lib.sendKey(it.code) }
        val started = TimeSource.Monotonic.markNow()
        var minimum = 0.0f
        var maximum = 0.0f
        while (started.elapsedNow().inWholeMilliseconds < 8000 &&
            (soundPort.get() != expectedPort || minimum >= -0.01f || maximum <= 0.01f)) {
            val samples = lib.readSoundSamples(441)
            assertEquals(441, samples.size)
            assertTrue(samples.all { it.isFinite() && it in -1.0f..1.0f }, "PCM must be finite and normalized")
            minimum = minOf(minimum, samples.min())
            maximum = maxOf(maximum, samples.max())
            Thread.sleep(10)
        }
        assertEquals(expectedPort, soundPort.get(), "The BASIC command must reach sound port 6:\n${screen(lib)}")
        assertTrue(minimum < -0.01f && maximum > 0.01f, "$message: range $minimum..$maximum\n${screen(lib)}")
    }

    private fun screen(lib: NativeLib) = decodeABC80Screen(lib.getMemory()).joinToString("\n")

    private fun assertMusicMachineCode(lib: NativeLib) {
        type(lib, "OUT 6,0\r")
        type(lib, "NEW\r")
        type(lib, "10 A%=INP(56):IF A%<128 THEN 10\r")
        type(lib, "20 IF A%=193 THEN D%=1000 ELSE D%=499\r")
        type(lib, "30 POKE -108,D%,SWAP%(D%):Z%=CALL(65408%):GOTO 10\r")
        val originalI = CPU(lib.getCPU()).iReg
        val originalInterruptMemory = lib.getMemory().copyOfRange(0xfa00, 0xfa38)
        lib.copyToMemory(0xfa00, intArrayOf(
            62, 250, 237, 71, 201, 245, 219, 56, 254, 131, 32, 3, 50, 7, 254,
            62, 128, 50, 245, 253, 62, 77, 50, 247, 253, 241, 251, 237, 77
        ).map { it.toByte() }.toByteArray())
        lib.copyToMemory(0xfa34, byteArrayOf(5, 0xfa.toByte(), 0x94.toByte(), 5))
        lib.copyToMemory(0xff80, intArrayOf(
            219, 56, 33, 255, 255, 119, 219, 56, 190, 32, 18, 62, 0,
            211, 6, 62, 57, 211, 6, 1, 232, 3, 11, 120, 177, 32, 251,
            24, 233, 62, 57, 211, 6, 201
        ).map { it.toByte() }.toByteArray())
        type(lib, "Z%=CALL(64000%)\r")
        val loops = AtomicInteger(0)
        val watcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                loops.incrementAndGet()
                return false
            }
        }
        lib.addMemoryReadWatcher(0xff93, watcher)
        try {
            "RUN\r".forEach { lib.sendKey(it.code) }
            Thread.sleep(250)
            val low = heldMusicTone(lib, 'A', loops)
            val high = heldMusicTone(lib, 'K', loops)
            assertTrue(high in (low * 1.6)..(low * 2.4),
                "Halving MUSIK's delay must double the measured pitch: A=$low Hz, K=$high Hz")
        } finally {
            lib.releaseAllKeys()
            lib.removeMemoryReadWatcher(0xff93, watcher)
            lib.freeze()
            Thread.sleep(50)
            val state = lib.getCPU()
            // I follows the ten 16-bit register pairs in the native snapshot.
            state[20] = originalI.toByte()
            lib.setProcessorState(state)
            lib.copyToMemory(0xfa00, originalInterruptMemory)
            lib.freeze(false)
            lib.sendKey(3)
            Thread.sleep(250)
            type(lib, "\r")
        }
    }

    private fun heldMusicTone(lib: NativeLib, key: Char, loops: AtomicInteger): Double {
        lib.pressKey(key.code)
        try {
            Thread.sleep(250)
            lib.readSoundSamples(4410)
            val initialLoops = loops.get()
            val samples = mutableListOf<Float>()
            repeat(60) {
                samples.addAll(lib.readSoundSamples(441).toList())
                Thread.sleep(10)
            }
            assertTrue(loops.get() - initialLoops > 40, "A held key must keep MUSIK's tone loop running")
            assertTrue(samples.all { it.isFinite() && it in -1f..1f })
            val baseline = samples.min()
            val peaks = samples.zipWithNext().count { (a, b) ->
                a <= baseline + 0.02f && b > baseline + 0.02f && b < -0.1f
            }
            val frequency = peaks * 44100.0 / samples.size
            println("MUSIK $key: $frequency Hz, range $baseline..${samples.max()}")
            assertTrue(frequency in 80.0..280.0,
                "MUSIK must produce a sustained periodic tone, not silence, DC or a click: $frequency Hz")
            return frequency
        } finally {
            lib.releaseKey(key.code)
            Thread.sleep(150)
            val stoppedLoops = loops.get()
            Thread.sleep(100)
            assertEquals(stoppedLoops, loops.get(), "Releasing the key must stop MUSIK's tone loop")
        }
    }

    private fun verifyInstalledMusic(lib: NativeLib, tape: ByteArray) {
        assertTrue(Abc80Cassette.import(tape.inputStream(), "music-test.tape"))
        "LOAD CAS:MUSIK.BAS\r".forEach { lib.sendKey(it.code) }
        await("The installed MUSIK.BAS LOAD must start") { Abc80CassetteStatus.visible }
        val loadingStarted = TimeSource.Monotonic.markNow()
        while (Abc80CassetteStatus.visible && loadingStarted.elapsedNow().inWholeMilliseconds < 30000) Thread.sleep(10)
        assertFalse(Abc80CassetteStatus.visible, "The installed MUSIK.BAS must load")
        val active = AtomicInteger(0)
        val watcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                active.incrementAndGet()
                return false
            }
        }
        lib.addMemoryReadWatcher(0xff93, watcher)
        try {
            "RUN\r".forEach { lib.sendKey(it.code) }
            await("MUSIK.BAS must show its keyboard") { screen(lib).contains("spela") }
            Thread.sleep(250)
            val low = heldMusicTone(lib, 'A', active)
            val high = heldMusicTone(lib, 'K', active)
            assertTrue(high in (low * 1.6)..(low * 2.4), "Installed MUSIK must play distinct pitches")
            lib.sendKey(3)
            Thread.sleep(500)
        } finally {
            lib.releaseAllKeys()
            lib.removeMemoryReadWatcher(0xff93, watcher)
        }
    }

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
