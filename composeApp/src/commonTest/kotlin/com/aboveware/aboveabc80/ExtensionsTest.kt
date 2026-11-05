package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionsTest {
    @Test
    fun isTextIdentifiesTextFiles() {
        assertTrue("test.sys".isText())
        assertTrue("CMDINT.SYS".isText())
        assertTrue("document.txt".isText())
        assertTrue("README.TXT".isText())
        assertTrue("script.rem".isText())
        assertTrue("PROGRAM.BAS".isText())

        assertFalse("image.png".isText())
        assertFalse("system.dsk".isText())
        assertFalse("binary.bac".isText())
        assertFalse("program.abs".isText())
    }
}
