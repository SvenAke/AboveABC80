package com.aboveware.abovecpm

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
    bucketName: String = "abovecpm-1a939.firebasestorage.app"
) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val baseUrl = "https://firebasestorage.googleapis.com/v0/b/$bucketName/o"

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
