package com.aboveware.abc80

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CloudPreviewTextTest {
    @Test
    fun decodesSwedishAbcTextWithoutChangingBasicSyntaxOrBytes() {
        val source = "1 REM Ins{nd\r\n10 REM GRAFIKRUTINER F\\R ABC80\r\n" +
            "30 REM Best}r av f|ljande delrutiner:\r\n" +
            "50 REM Raderar sk{rmen och s{tter den\r\n" +
            "80 PRINT CHR$(91): A$=\"[]{}\\|^~\": REM V{rde=0 -> svart f{rg\r\n"
        val bytes = source.encodeToByteArray()
        val original = bytes.copyOf()
        val expected = "1 REM Ins\u00E4nd\r\n10 REM GRAFIKRUTINER F\u00D6R ABC80\r\n" +
            "30 REM Best\u00E5r av f\u00F6ljande delrutiner:\r\n" +
            "50 REM Raderar sk\u00E4rmen och s\u00E4tter den\r\n" +
            "80 PRINT CHR$(91): A$=\"\u00C4\u00C5\u00E4\u00E5\u00D6\u00F6\u00DC\u00FC\": REM V\u00E4rde=0 -> svart f\u00E4rg\r\n"
        assertEquals(expected, bytes.toCloudPreviewText())
        assertContentEquals(original, bytes)
    }

    @Test
    fun preservesUtf8SwedishTextAndRemovesBom() {
        val text = "10 REM \u00C5\u00C4\u00D6 \u00E5\u00E4\u00F6 \u00C9\u00E9\n20 PRINT A$"
        assertEquals(text, ("\uFEFF" + text).encodeToByteArray().toCloudPreviewText())
    }

    @Test
    fun decodesLatin1WithoutReplacementCharacters() {
        val text = "10 REM \u00C5\u00C4\u00D6 \u00E5\u00E4\u00F6\r\n20 REM f|rg"
        assertEquals(
            "10 REM \u00C5\u00C4\u00D6 \u00E5\u00E4\u00F6\r\n20 REM f\u00F6rg",
            text.toByteArray(Charsets.ISO_8859_1).toCloudPreviewText()
        )
    }
}
