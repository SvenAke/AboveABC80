package com.aboveware.aboveabc80.core

import aboveabc80.composeapp.generated.resources.Res
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.NativeLib
import com.aboveware.aboveabc80.loadLocalDisk
import com.aboveware.aboveabc80.saveLocalDisk
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DiskController {
    enum class Source { RESOURCE, LOCAL }
    data class MountedDisk(val name: String, val source: Source)

    companion object {
        val instance = DiskController()
        const val MAX_DRIVES = 2
    }

    val mountedDisks = mutableStateMapOf<Int, MountedDisk>()
    var lastAccessTime by mutableStateOf(0L)
        private set
    var lastAccessDriveIndex by mutableStateOf(0)
        private set
    fun recordAccess(drive: Int) {
        lastAccessDriveIndex = drive
        lastAccessTime = System.currentTimeMillis()
    }
    @Volatile var currentDriveIndex = 0
        set(value) { field = value.coerceIn(0 until MAX_DRIVES) }

    fun getFloppy(index: Int): AbcDiskImage? =
        if (mountedDisks.containsKey(index)) AbcDiskImage(NativeLib.getObject().bus.floppy.snapshot(index)) else null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadFromResource(index: Int, resourcePath: String) {
        require(index in 0 until MAX_DRIVES)
        val image = Res.readBytes(resourcePath)
        val name = resourcePath.substringAfterLast('/')
        check(NativeLib.getObject().bus.floppy.mount(index, image, name)) { "Kunde inte montera $name" }
        mountedDisks[index] = MountedDisk(name, Source.RESOURCE)
    }

    fun loadFromLocal(index: Int, name: String): String? {
        require(index in 0 until MAX_DRIVES)
        val data = loadLocalDisk(name) ?: run {
            Abc80Log.wtf("Kunde inte läsa disketten $name")
            return null
        }
        val image = try {
            AbcDiskImage.createFromData(data).getRawData()
        } catch (e: IllegalArgumentException) {
            Abc80Log.wtf("Kunde inte läsa disketten $name: ${e.message}")
            return null
        }
        val actualName = if (name.endsWith(".imd", true)) name.substringBeforeLast('.') + ".DSK" else name
        if (actualName != name && !saveLocalDisk(actualName, image)) {
            Abc80Log.wtf("Kunde inte spara konverterad diskett $actualName")
            return null
        }
        if (!NativeLib.getObject().bus.floppy.mount(index, image, actualName)) return null
        mountedDisks[index] = MountedDisk(actualName, Source.LOCAL)
        Abc80Log.diskett("DR$index: mounted $actualName (${image.size} bytes)")
        return actualName
    }

    fun clearDrive(index: Int) {
        require(index in 0 until MAX_DRIVES)
        mountedDisks.remove(index)
        NativeLib.getObject().bus.floppy.unmount(index)
    }
}
