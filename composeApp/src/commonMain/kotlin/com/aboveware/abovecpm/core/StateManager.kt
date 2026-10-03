package com.aboveware.abovecpm.core

import com.aboveware.abovecpm.NativeLib
import com.aboveware.abovecpm.loadLocalDisk
import com.aboveware.abovecpm.saveLocalDisk
import com.aboveware.abovecpm.terminal.TerminalManager
import com.aboveware.abovecpm.terminal.TerminalType

object StateManager {

    fun saveSnapshot(name: String): Boolean {
        val lib = NativeLib.getObject()
        val memory = lib.getMemory()
        val cpuState = lib.getCPU()

        val terminalType = TerminalManager.currentTerminalType.ordinal
        val terminalData = TerminalManager.activeTerminal.getScreenState()

        val controller = DiskController.instance
        val driveIndex = controller.currentDriveIndex
        // For mounts, we just save the names.
        val mounts = StringBuilder()
        for (i in 0 until DiskController.MAX_DRIVES) {
            val m = controller.mountedDisks[i]
            if (m != null) {
                mounts.append("${m.source.name}:${m.name}\n")
            } else {
                mounts.append("NONE\n")
            }
        }

        val mountData = mounts.toString().encodeToByteArray()

        // Final layout:
        // [4: Header "SNAP"]
        // [4: Int Memory Size 65536]
        // [65536: Memory]
        // [4: Int CPU Size]
        // [N: CPU State]
        // [1: Terminal Type]
        // [4: Int Terminal Data Size]
        // [N: Terminal Data]
        // [4: Int Mount Data Size]
        // [N: Mount Data]
        // [1: Current Drive Index]

        val snapshotData = "SNAP".encodeToByteArray() +
                intToBytes(memory.size) + memory +
                intToBytes(cpuState.size) + cpuState +
                byteArrayOf(terminalType.toByte()) +
                intToBytes(terminalData.size) + terminalData +
                intToBytes(mountData.size) + mountData +
                byteArrayOf(driveIndex.toByte())

        return saveLocalDisk("$name.snapshot", snapshotData)
    }

    fun loadSnapshot(name: String): Boolean {
        val data = loadLocalDisk("$name.snapshot") ?: return false
        if (data.size < 12) return false

        var offset = 0
        val header = data.copyOfRange(offset, offset + 4).decodeToString()
        if (header != "SNAP") return false
        offset += 4

        val memSize = bytesToInt(data, offset)
        offset += 4
        val memory = data.copyOfRange(offset, offset + memSize)
        offset += memSize

        val cpuSize = bytesToInt(data, offset)
        offset += 4
        val cpuState = data.copyOfRange(offset, offset + cpuSize)
        offset += cpuSize

        val terminalType = data[offset++].toInt() and 0xFF
        if (terminalType < TerminalType.entries.size) {
            TerminalManager.currentTerminalType = TerminalType.entries[terminalType]
        }

        val termSize = bytesToInt(data, offset)
        offset += 4
        val terminalData = data.copyOfRange(offset, offset + termSize)
        offset += termSize

        TerminalManager.activeTerminal.setScreenState(terminalData)

        val mountSize = bytesToInt(data, offset)
        offset += 4
        val mountData = data.copyOfRange(offset, offset + mountSize).decodeToString()
        offset += mountSize

        val driveIndex = data[offset++].toInt() and 0xFF

        // Restore mounts
        val lines = mountData.split("\n")
        val controller = DiskController.instance
        for (i in 0 until DiskController.MAX_DRIVES) {
            if (i < lines.size) {
                val line = lines[i]
                if (line != "NONE" && line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    val diskName = parts[1]
                    controller.loadFromLocal(i, diskName)
                }
            }
        }
        controller.currentDriveIndex = driveIndex

        val lib = NativeLib.getObject()
        lib.freeze()
        lib.setRam(0, memory)
        lib.setProcessorState(cpuState)
        lib.thaw()

        return true
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    private fun bytesToInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
    }
}
