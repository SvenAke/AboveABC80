package com.aboveware.abc80

import aboveabc80.composeapp.generated.resources.Res
import aboveabc80.composeapp.generated.resources.disk_listing_failed
import aboveabc80.composeapp.generated.resources.disk_mount_failed
import com.aboveware.abc80.terminal.ComputerModel
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiskErrorMessageTest {
    @Test
    fun diskErrorsNameTheSelectedComputerModel() = runBlocking {
        for (model in ComputerModel.entries) {
            for (resource in listOf(Res.string.disk_mount_failed, Res.string.disk_listing_failed)) {
                val message = getString(resource, model.displayName)
                assertTrue(message.contains(model.displayName), message)
                assertFalse(message.contains("%1\$s"), message)
                if (model.isAbc800) {
                    assertFalse(message.contains("ABC80."), message)
                }
            }
        }
    }
}
