package com.aboveware.aboveabc80

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.decodeURLPart
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CloudDisplayNameTest {
    @Test
    fun decodesSwedishLettersFromLatin1AndWindows1252Mojibake() {
        assertEquals("åäöÅÄÖ.bas", "Ã¥Ã¤Ã¶Ã\u0085Ã\u0084Ã\u0096.bas".toCloudDisplayName())
        assertEquals("åäöÅÄÖ.bas", "Ã¥Ã¤Ã¶Ã…Ã„Ã–.bas".toCloudDisplayName())
    }

    @Test
    fun decodesDosNamesWithoutChangingCorrectNamesOrExtensions() {
        assertEquals("åäöÅÄÖ.bas", "σΣ÷┼─╓.bas".toCloudDisplayName())
        assertEquals("besök.grf", "bes÷k.grf".toCloudDisplayName())
        for (name in listOf("besök.grp", "ÅÄÖåäö.TXT", "README", "file-123.zip")) {
            assertEquals(name, name.toCloudDisplayName())
        }
    }

    @Test
    fun displayConversionDoesNotChangeDownloadIdentity() = runBlocking {
        val node = StorageNode("bes÷k.grf", "abc80/bes÷k.grf", false)
        assertEquals("besök.grf", node.displayName)
        assertEquals("bes÷k.grf", node.name)
        assertEquals("abc80/bes÷k.grf", node.fullName)
        val bytes = byteArrayOf(0, 1, 255.toByte())
        val client = HttpClient(MockEngine { request ->
            assertEquals("/v0/b/test/o/abc80/bes÷k.grf", request.url.encodedPath.decodeURLPart())
            assertEquals("media", request.url.parameters["alt"])
            respond(bytes)
        })
        val provider = FirebaseRestStorageProvider("test", client)
        try {
            assertContentEquals(bytes, provider.downloadFile(node.fullName))
        } finally {
            provider.close()
        }
    }
}
