package com.aboveware.aboveabc80

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FirebaseStorageListResponse(
    val items: List<FirebaseStorageItem> = emptyList(),
    val prefixes: List<String> = emptyList()
)

@Serializable
data class FirebaseStorageItem(
    val name: String,
    val size: String? = null
)

data class StorageNode(
    val name: String,
    val fullName: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val children: MutableList<StorageNode> = mutableListOf()
)

class FirebaseRestStorageProvider(
    bucketName: String = "abovecpm-1a939.firebasestorage.app",
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    private val baseUrl = "https://firebasestorage.googleapis.com/v0/b/$bucketName/o"

    fun close() = client.close()

    suspend fun downloadToFile(
        fileName: String,
        destination: File,
        expectedSize: Long = 0,
        onProgress: (Long) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val directory = checkNotNull(destination.parentFile)
        check(directory.isDirectory || directory.mkdirs()) { "Could not create download directory" }
        val temporary = File.createTempFile("cloud-", ".part", directory)
        try {
            client.prepareGet("$baseUrl/${fileName.encodeURLPathPart()}") {
                parameter("alt", "media")
            }.execute { response ->
                check(response.status.value in 200..299) {
                    "HTTP ${response.status.value} downloading ${destination.name}"
                }
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(64 * 1024)
                var received = 0L
                temporary.outputStream().use { output ->
                    while (true) {
                        val count = channel.readAvailable(buffer, 0, buffer.size)
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        received += count
                        onProgress(received)
                    }
                }
                val requiredSize = contentLength ?: expectedSize.takeIf { it > 0 }
                check(requiredSize == null || received == requiredSize) {
                    "Incomplete download: $received of $requiredSize bytes"
                }
                check(expectedSize <= 0 || received == expectedSize) {
                    "Unexpected download size: $received of $expectedSize bytes"
                }
                check(received > 0) { "Downloaded file is empty" }
            }
            replaceDownloadedFile(temporary, destination)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Abc80Log.wtf("Cloud download failed for ${destination.name}: ${e.message}")
            throw e
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                Abc80Log.wtf("Could not remove partial download ${temporary.name}")
            }
        }
    }

    private fun replaceDownloadedFile(temporary: File, destination: File) {
        if (!destination.exists()) {
            check(temporary.renameTo(destination)) { "Could not save ${destination.name}" }
            return
        }
        val backup = File.createTempFile("cloud-", ".backup", destination.parentFile)
        check(backup.delete()) { "Could not prepare download backup" }
        check(destination.renameTo(backup)) { "Could not replace ${destination.name}" }
        if (!temporary.renameTo(destination)) {
            if (!backup.renameTo(destination)) {
                throw IOException("Could not restore ${destination.name}; previous file is in ${backup.name}")
            }
            throw IOException("Could not save ${destination.name}")
        }
        if (!backup.delete()) Abc80Log.wtf("Could not remove download backup ${backup.name}")
    }

    suspend fun listFiles(prefix: String = ""): List<StorageNode> = coroutineScope {
        try {
            val response: FirebaseStorageListResponse = client.get(baseUrl) {
                parameter("delimiter", "/")
                if (prefix.isNotEmpty()) {
                    parameter("prefix", prefix)
                }
            }.body()

            val folders = response.prefixes.map {
                val name = it.removeSuffix("/").substringAfterLast("/")
                StorageNode(name, it, true)
            }.sortedBy { it.name.lowercase() }

            val files = response.items.map { item ->
                async {
                    val name = item.name.substringAfterLast("/")
                    val size = getFileSize(item.name)
                    StorageNode(name, item.name, false, size)
                }
            }.awaitAll().sortedBy { it.name.lowercase() }

            folders + files
        } catch (e: Exception) {
            println("Error listing files: ${e.message}")
            emptyList()
        }
    }

    suspend fun getFileSize(fileName: String): Long {
        return try {
            val encodedName = fileName.replace("/", "%2F").replace(" ", "%20")
            val url = "$baseUrl/$encodedName"
            val response: FirebaseStorageItem = client.get(url).body()
            response.size?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun downloadFile(fileName: String): ByteArray? {
        return try {
            // Encode the fileName path for the URL
            val encodedName = fileName.replace("/", "%2F").replace(" ", "%20")
            val downloadUrl = "$baseUrl/$encodedName?alt=media"
            val response: ByteArray = client.get(downloadUrl).body()
            response
        } catch (e: Exception) {
            println("Error downloading file: ${e.message}")
            null
        }
    }

    suspend fun uploadFile(fileName: String, data: ByteArray): Boolean {
        return try {
            val encodedName = fileName.replace("/", "%2F").replace(" ", "%20")
            val uploadUrl = "$baseUrl/$encodedName"
            client.post(uploadUrl) {
                setBody(data)
            }
            true
        } catch (e: Exception) {
            println("Error uploading file: ${e.message}")
            false
        }
    }

    suspend fun deleteFile(fileName: String): Boolean {
        return try {
            val encodedName = fileName.replace("/", "%2F").replace(" ", "%20")
            val deleteUrl = "$baseUrl/$encodedName"
            client.delete(deleteUrl)
            true
        } catch (e: Exception) {
            println("Error deleting file: ${e.message}")
            false
        }
    }
}
