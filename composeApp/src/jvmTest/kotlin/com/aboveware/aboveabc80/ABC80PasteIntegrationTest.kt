package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.time.TimeSource

@OptIn(ExperimentalResourceApi::class)
class ABC80PasteIntegrationTest {
    @Test
    fun multilinePasteReachesBasicWithSwedishCharacterCodes() = runBlocking {
        val home = Files.createTempDirectory("abc80-paste-test").toFile()
        val previousHome = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        check(home.resolve(".aboveabc80").resolve("dr").mkdirs())
        val lib = NativeLib.getObject()
        val prompts = AtomicInteger()
        val watcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                prompts.incrementAndGet()
                lib.commandHistory.atPrompt = true
                return false
            }
        }
        try {
            lib.portRead(3)
            val memory = ByteArray(65536)
            Res.readBytes("files/prom.rom").copyInto(memory)
            Res.readBytes("files/dos.rom").copyInto(memory, 0x6000)
            Res.readBytes("files/printer.rom").copyInto(memory, 0x7800)
            lib.copyToMemory(0, memory)
            lib.addMemoryReadWatcher(0x00db, watcher)
            lib.startEmulator(3_000_000)
            lib.freeze(false)
            val started = TimeSource.Monotonic.markNow()
            while (prompts.get() == 0 && started.elapsedNow().inWholeMilliseconds < 10000) delay(10)
            assertTrue(prompts.get() > 0, "BASIC must be ready before paste")
            pasteABC80Text(lib, "10 PRINT \"ÅÄÖåäö\"\r\n20 PRINT 9876\nLIST\n")
            val deadline = TimeSource.Monotonic.markNow()
            var output = ""
            while (deadline.elapsedNow().inWholeMilliseconds < 10000) {
                val current = lib.getMemory()
                fun word(at: Int) = (current[at].toInt() and 255) or
                    ((current[at + 1].toInt() and 255) shl 8)
                val start = word(0xFE1C)
                val second = start + (current[start].toInt() and 255)
                output = decodeABC80Screen(current, false).joinToString("\n")
                if (output.contains("20 PRINT 9876") && word(second + 1) == 20) break
                delay(10)
            }
            assertTrue(output.contains("10 PRINT \"][\\}{|\""), output)
            assertTrue(output.contains("20 PRINT 9876"), output)
            assertTrue(!output.contains("ERR"), output)
            val loaded = lib.getMemory()
            fun word(at: Int) = (loaded[at].toInt() and 255) or
                ((loaded[at + 1].toInt() and 255) shl 8)
            val firstLine = word(0xFE1C)
            assertEquals(10, word(firstLine + 1))
            val secondLine = firstLine + (loaded[firstLine].toInt() and 255)
            assertEquals(20, word(secondLine + 1))
            delay(500)
            val before = prompts.get()
            pasteABC80Text(lib, "PRINT 7654\r")
            val wait = TimeSource.Monotonic.markNow()
            while (prompts.get() == before && wait.elapsedNow().inWholeMilliseconds < 10000) delay(10)
            assertTrue(prompts.get() > before)
            assertEquals("PRINT 7654", lib.commandHistory.recall(-1, ""))
            pasteABC80Text(lib, "PRINT 7654")
            delay(500)
            assertEquals("PRINT 7654", abc80PromptLine(lib.getMemory()))
        } finally {
            lib.freeze()
            lib.removeMemoryReadWatcher(0x00db, watcher)
            lib.bus.floppy.disks.close()
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }
}
