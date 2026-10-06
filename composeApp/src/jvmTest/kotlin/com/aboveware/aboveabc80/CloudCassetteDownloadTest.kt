package com.aboveware.aboveabc80

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CloudCassetteDownloadTest {
    @Test
    fun cassetteFoldersUseInternalStorageEvenFromTheGeneralCloudBrowser() {
        assertEquals("tapes", cloudDownloadFolder("abc80/cassettes/", null))
        assertEquals("tapes", cloudDownloadFolder("abc80/cassettes/subfolder/", null))
        assertEquals("tapes", cloudDownloadFolder("abc80/", "tapes"))
        assertNull(cloudDownloadFolder("abc80/programs/", null))
        assertNull(cloudDownloadFolder("abc80/", null))
    }

    @Test
    fun savedCloudTapeIsVisibleInSavedTapesAndDoesNotModifyDisks() = runBlocking {
        val home = Files.createTempDirectory("cloud-cassette-storage").toFile()
        val previousHome = System.getProperty("user.home")
        val data = byteArrayOf(10, 20, 30)
        val disk = byteArrayOf(40, 50, 60)
        val client = HttpClient(MockEngine { respond(data) })
        try {
            System.setProperty("user.home", home.absolutePath)
            assertTrue(saveLocalDisk("test.dsk", disk))
            FirebaseRestStorageProvider("test", client).downloadToFile(
                "abc80/cassettes/test.wav", getLocalFileDirectory("tapes").resolve("test.wav")
            )
            assertEquals(listOf("test.wav"), listLocalFiles("tapes"))
            assertContentEquals(data, loadLocalFile("tapes", "test.wav"))
            assertContentEquals(disk, loadLocalDisk("test.dsk"))
            assertEquals(listOf("test.dsk"), listLocalDisks())
        } finally {
            client.close()
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }

    @Test
    fun cancellationRemovesPartialDownloadAndPreservesSavedTape() = runBlocking {
        val directory = Files.createTempDirectory("cloud-cassette-cancel").toFile()
        val original = byteArrayOf(1, 2, 3)
        val destination = directory.resolve("cassette.wav").apply { writeBytes(original) }
        val client = HttpClient(MockEngine { respond(ByteArray(128 * 1024)) })
        try {
            assertFailsWith<CancellationException> {
                FirebaseRestStorageProvider("test", client).downloadToFile("cassette.wav", destination) {
                    throw CancellationException("Test cancellation")
                }
            }
            assertContentEquals(original, destination.readBytes())
            assertEquals(listOf("cassette.wav"), directory.list()!!.toList())
        } finally {
            client.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun streamsCassetteLargerThanTypicalAndroidHeap() = runBlocking {
        val directory = Files.createTempDirectory("cloud-cassette-test").toFile()
        val size = 320L * 1024 * 1024
        var progress = 0L
        val engine = MockEngine { request ->
            assertEquals("/v0/b/test/o/abc80%2Fcassettes%2Ftest%20%23+.wav", request.url.encodedPath)
            assertEquals("media", request.url.parameters["alt"])
            val channel = ByteChannel(autoFlush = true)
            launch(Dispatchers.IO) {
                try {
                    val block = ByteArray(64 * 1024) { (it % 251).toByte() }
                    repeat((size / block.size).toInt()) { channel.writeFully(block) }
                } finally {
                    channel.close()
                }
            }
            respond(channel, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, size.toString()))
        }
        val client = HttpClient(engine)
        try {
            val destination = directory.resolve("cassette.wav")
            FirebaseRestStorageProvider("test", client).downloadToFile(
                "abc80/cassettes/test #+.wav", destination, size
            ) { received ->
                assertTrue(received >= progress)
                progress = received
            }
            assertEquals(size, destination.length())
            assertEquals(size, progress)
            destination.inputStream().use { input ->
                val actual = ByteArray(64 * 1024)
                assertEquals(actual.size, input.read(actual))
                assertContentEquals(ByteArray(actual.size) { (it % 251).toByte() }, actual)
            }
            assertEquals(listOf("cassette.wav"), directory.list()!!.toList())
        } finally {
            client.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun failedDownloadPreservesExistingTapeAndRemovesPartialFile() = runBlocking {
        val directory = Files.createTempDirectory("cloud-cassette-failure").toFile()
        val original = byteArrayOf(1, 2, 3)
        val destination = directory.resolve("cassette.wav").apply { writeBytes(original) }
        val client = HttpClient(MockEngine {
            respond(byteArrayOf(4, 5), HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "10"))
        })
        try {
            assertFailsWith<IllegalStateException> {
                FirebaseRestStorageProvider("test", client).downloadToFile("cassette.wav", destination)
            }
            assertContentEquals(original, destination.readBytes())
            assertEquals(listOf("cassette.wav"), directory.list()!!.toList())
        } finally {
            client.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun httpErrorIsReportedInsteadOfSavingAnErrorPage() = runBlocking {
        val directory = Files.createTempDirectory("cloud-cassette-http").toFile()
        val client = HttpClient(MockEngine { respond("Forbidden", HttpStatusCode.Forbidden) })
        try {
            val failure = assertFailsWith<IllegalStateException> {
                FirebaseRestStorageProvider("test", client).downloadToFile("cassette.wav", directory.resolve("cassette.wav"))
            }
            assertTrue(failure.message!!.contains("403"))
            assertTrue(directory.list()!!.isEmpty())
        } finally {
            client.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun completeDownloadReplacesExistingTape() = runBlocking {
        val directory = Files.createTempDirectory("cloud-cassette-replace").toFile()
        val destination = directory.resolve("cassette.wav").apply { writeBytes(byteArrayOf(1)) }
        val downloaded = byteArrayOf(2, 3, 4)
        val client = HttpClient(MockEngine { respond(downloaded) })
        try {
            FirebaseRestStorageProvider("test", client).downloadToFile("cassette.wav", destination)
            assertContentEquals(downloaded, destination.readBytes())
            assertEquals(listOf("cassette.wav"), directory.list()!!.toList())
        } finally {
            client.close()
            directory.deleteRecursively()
        }
    }
}
