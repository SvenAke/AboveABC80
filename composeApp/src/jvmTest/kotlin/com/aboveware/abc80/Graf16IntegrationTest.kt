package com.aboveware.abc80

import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class Graf16IntegrationTest {
    @Test
    fun decodesAndRestoresGraf16() = runBlocking {
        val source = System.getenv("ABC800_GRAF16_TD0")
        assumeTrue("Set ABC800_GRAF16_TD0 to a local Graf16 TD0 image", source != null)
        val image = java.io.File(requireNotNull(source)).readBytes()
        val raw = TeleDiskImage.decode(image).rawData
        assertEquals(163840, raw.size)
        val hash = MessageDigest.getInstance("SHA-256").digest(raw)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals("f16e41cc9c3d26f4e1cddc1e7ebfb4f37de7adea7549f503d11e3a63f30b91fe", hash)
        val files = assertNotNull(Abc80FloppyLayout.list(raw)).map { it.first }
        assertTrue(files.containsAll(listOf("START.BAC", "GHMENY.BAC", "BASICINI.SYS")))
        for (size in listOf(12, image.size / 2)) {
            assertFailsWith<IllegalArgumentException> { TeleDiskImage.decode(image.copyOf(size)) }
        }
        val corrupted = image.copyOf()
        corrupted[100] = (corrupted[100].toInt() xor 1).toByte()
        assertFailsWith<IllegalArgumentException> { TeleDiskImage.decode(corrupted) }
        val home = Files.createTempDirectory("graf16-test").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val lib = NativeLib.getObject()
        lib.freeze(true)
        try {
            System.setProperty("user.home", home.absolutePath)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = ComputerModel.ABC800M
            assertTrue(saveLocalDisk("GRAF16.TD0", image))
            assertEquals("GRAF16.TD0", DiskController.instance.loadFromLocal(0, "GRAF16.TD0"))
            assertContentEquals(raw, lib.bus.floppy.snapshot(0))
            DiskController.instance.closeForComputerModelChange()
            assertEquals("GRAF16.TD0", DiskController.instance.restoreFromLocal(0, "GRAF16.TD0"))
            assertContentEquals(raw, lib.bus.floppy.snapshot(0))
        } finally {
            lib.freeze(true)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
            lib.freeze(false)
            home.deleteRecursively()
        }
    }
}
