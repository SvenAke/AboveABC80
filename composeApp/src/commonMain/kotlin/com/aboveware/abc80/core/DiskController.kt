package com.aboveware.abc80.core

import aboveabc80.composeapp.generated.resources.Res
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.aboveware.abc80.Abc80Log
import com.aboveware.abc80.NativeLib
import com.aboveware.abc80.Abc80FloppyLayout
import com.aboveware.abc80.getComputerModelStorageDir
import com.aboveware.abc80.getPersistedString
import com.aboveware.abc80.setPersistedString
import com.aboveware.abc80.loadLocalDisk
import com.aboveware.abc80.listLocalDisks
import com.aboveware.abc80.isDiskImageName
import com.aboveware.abc80.terminal.ComputerModel
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DiskController {
    enum class Source { RESOURCE, LOCAL, DRIVE }
    data class MountedDisk(val name: String, val source: Source)

    companion object {
        val instance = DiskController()
        const val MAX_POSSIBLE_DRIVES = 8
        const val MAX_DRIVES = 8
        const val UNMOUNTED_PREFERENCE = "NONE"

        fun drivePreferenceKey(model: ComputerModel, index: Int): String {
            require(index in 0 until MAX_POSSIBLE_DRIVES)
            return "drive_${model.name}_$index"
        }
    }

    var maxDrives by mutableIntStateOf(
        getPersistedString("abc80_max_drives", "3").toIntOrNull()?.coerceIn(2, MAX_POSSIBLE_DRIVES) ?: 3
    )

    fun setDriveCount(count: Int) {
        val newMax = count.coerceIn(2, MAX_POSSIBLE_DRIVES)
        maxDrives = newMax
        setPersistedString("abc80_max_drives", newMax.toString())
        for (i in newMax until MAX_POSSIBLE_DRIVES) {
            clearDrive(i)
        }
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
        set(value) { field = value.coerceIn(0 until maxDrives) }

    fun getFloppy(index: Int): AbcDiskImage? =
        if (mountedDisks.containsKey(index)) AbcDiskImage(NativeLib.getObject().bus.floppy.snapshot(index)) else null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadFromResource(index: Int, resourcePath: String) {
        require(index in 0 until MAX_POSSIBLE_DRIVES)
        val image = Res.readBytes(resourcePath)
        val name = resourcePath.substringAfterLast('/')
        check(NativeLib.getObject().bus.floppy.mount(index, image, name)) { "Kunde inte montera $name" }
        mountedDisks[index] = MountedDisk(name, Source.RESOURCE)
    }

    fun loadFromLocal(index: Int, name: String): String? {
        require(index in 0 until MAX_POSSIBLE_DRIVES)
        val data = loadLocalDisk(name) ?: run {
            Abc80Log.wtf("Kunde inte läsa disketten $name")
            return null
        }
        return mountLocalImage(index, name, data)
    }

    fun restoreFromLocal(index: Int, name: String): String? {
        require(index in 0 until MAX_POSSIBLE_DRIVES)
        val driveImage = getComputerModelStorageDir("dr").resolve("$index.dsk")
        if (driveImage.isFile && driveImage.length() > 0) {
            val image = try {
                AbcDiskImage.createFromData(driveImage.readBytes()).getRawData()
            } catch (e: IllegalArgumentException) {
                Abc80Log.wtf("Could not read saved drive image DR$index: ${e.message}")
                null
            }
            if (image != null && Abc80FloppyLayout.detectFormatName(image) != null) {
                val libraryName = matchingLibraryDisk(image)
                return mountImage(index, libraryName ?: name, image, if (libraryName == null) {
                    Source.DRIVE
                } else {
                    Source.LOCAL
                }, restoreAddressMarks = true)
            }
            if (image != null) {
                Abc80Log.wtf("Saved drive image DR$index has an unsupported format; restoring $name instead")
            }
        }
        return loadFromLocal(index, name)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun restoreFromDriveImage(index: Int): String? {
        require(index in 0 until MAX_POSSIBLE_DRIVES)
        val driveImage = getComputerModelStorageDir("dr").resolve("$index.dsk")
        if (!driveImage.isFile || driveImage.length() == 0L) return null
        val image = try {
            AbcDiskImage.createFromData(driveImage.readBytes()).getRawData()
        } catch (e: IllegalArgumentException) {
            Abc80Log.wtf("Could not read saved drive image DR$index: ${e.message}")
            return null
        }
        if (Abc80FloppyLayout.detectFormatName(image) == null) {
            Abc80Log.wtf("Saved drive image DR$index has an unsupported format")
            return null
        }
        val libraryName = matchingLibraryDisk(image)
        val name = libraryName ?: "DR$index.DSK"
        return mountImage(index, name, image, if (libraryName == null) Source.DRIVE else Source.LOCAL,
            restoreAddressMarks = true)
    }

    private fun matchingLibraryDisk(image: ByteArray): String? =
        listLocalDisks().filter(::isDiskImageName).firstOrNull { name ->
            val library = loadLocalDisk(name)
            if (library == null) false else try {
                AbcDiskImage.createFromData(library).getRawData().contentEquals(image)
            } catch (e: IllegalArgumentException) {
                Abc80Log.wtf("Cannot compare library disk $name: ${e.message}")
                false
            }
        }

    private fun mountLocalImage(index: Int, name: String, data: ByteArray): String? =
        mountImage(index, name, data, Source.LOCAL)

    private fun mountImage(
        index: Int, name: String, data: ByteArray, source: Source, restoreAddressMarks: Boolean = false
    ): String? {
        val image = try {
            AbcDiskImage.createFromData(data).getRawData()
        } catch (e: IllegalArgumentException) {
            Abc80Log.wtf("Kunde inte läsa disketten $name: ${e.message}")
            return null
        }
        if (!NativeLib.getObject().bus.floppy.mount(index, data, name, restoreAddressMarks)) return null
        mountedDisks[index] = MountedDisk(name, source)
        Abc80Log.diskette("DR$index: mounted $name (${image.size} bytes)")
        return name
    }

    fun clearDrive(index: Int) {
        require(index in 0 until MAX_POSSIBLE_DRIVES)
        mountedDisks.remove(index)
        NativeLib.getObject().bus.floppy.unmount(index)
    }

    fun closeForComputerModelChange() {
        NativeLib.getObject().bus.floppy.closeForComputerModelChange()
        mountedDisks.clear()
        currentDriveIndex = 0
        lastAccessDriveIndex = 0
        lastAccessTime = 0L
    }
}
