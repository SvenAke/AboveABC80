/**
 * NativeLib: Interface to the native C++ ABC80 emulator core.
 * Screen rendering is handled by the Compose ABC80 screen.
 */
package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.cpu
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NativeLib {
    interface MemoryReadWatcher {
        fun onRead(address: Int): Boolean
    }

    interface MemoryWriteWatcher {
        fun onWrite(address: Int, data: Short, before: Short, changed: Boolean)
    }

    interface OnExecuteForListener {
        fun onExecuteFor(): Int
    }

    companion object {
        private var initialized = false
        private lateinit var instance: NativeLib

        fun getObject(): NativeLib {
            if (!initialized) {
                try {
                    loadNativeLibrary()
                    instance = NativeLib()
                    instance.registerInstanceNative()
                    initialized = true
                    TKN80.run(instance)
                    try {
                        instance.cassettePlayer.run()
                    } catch (e: Throwable) {
                        System.err.println("NativeLib: Cassette setup failed: ${e.message}")
                    }
                } catch (e: Throwable) {
                    // Fallback for preview mode or missing library
                    System.err.println("NativeLib: Could not initialize native library: ${e.message}")
                    instance = NativeLib()
                }
                initialized = true
            }
            return instance
        }
    }

    private external fun registerInstanceNative()

    private var lastLabel = ""
    private var labelCount = 0

    inner class TraceListener(val label: String) : MemoryReadWatcher {
        @OptIn(ExperimentalUnsignedTypes::class)
        override fun onRead(address: Int): Boolean {
            if (lastLabel == label) {
                ++labelCount
            } else {
                if (labelCount != 0) Abc80Log.trace("... $lastLabel($labelCount)")
                lastLabel = label
                labelCount = 0
                val cpu = cpu()
                var message = "Trace: $label ${cpu.cycle} ${address.toHex()} $cpu"
                val retAddr = (peek(cpu.sp + 1) shl 8) or peek(cpu.sp)
                val retAddr2 = (peek(cpu.sp + 3) shl 8) or peek(cpu.sp + 2)
                message += " RET -> ${retAddr.toHex()}, ${retAddr2.toHex()}"
                Abc80Log.trace(message)
            }
            return false
        }
    }

    fun addTraceListener(address: String) =
        addMemoryReadWatcher(label(address), TraceListener(address))

    fun label(label: String) = Assembler.instance.labels[label]
    fun label(label: Int) = Assembler.instance.labels[label]

    operator fun get(startAddress: Int, endAddress: Int) =
        getMemory().copyOfRange(startAddress, endAddress + 1)

    fun peek(address: Int) = peekMemory(address % 0xffff)
    fun peek(label: String) = peekMemory(label(label))
    fun peekw(address: Int) = peek(address + 1).shl(8) or peek(address)
    fun peekw(label: String) = peekw(label(label))

    fun disassemble(pc: Int): String = disassembleNative(pc)
    private external fun disassembleNative(pc: Int): String

    fun startEmulator(cpuFrequency: Int) = startEmulatorNative(cpuFrequency)
    private external fun startEmulatorNative(cpuFrequency: Int)

    val bus = Abc80Bus().also { it.run() }

    // Called from the native emulator thread for ports 0-7.
    val pio = Abc80Pio()
    val cassettePlayer = Abc80CassettePlayer()

    fun portRead(port: Int): Int {
        when (port) {
            0x03 -> { TKN80.setEnabled(false, this); return 0x40 }
            0x04 -> { TKN80.setEnabled(true, this); return 0x80 }
        }
        return try {
            if (port in 0x39..0x3B) cassettePlayer.read(port) else bus.read(port)
        } catch (e: Throwable) { 0xFF }
    }

    fun portWrite(port: Int, data: Int) {
        try {
            if (port in 0x39..0x3B) cassettePlayer.write(port, data) else bus.write(port, data)
        } catch (_: Throwable) {}
    }

    fun setRegisterB(value: Int) = setRegisterBNative(value)
    private external fun setRegisterBNative(value: Int)

    fun setZeroFlag(value: Boolean) = setZeroFlagNative(value)
    private external fun setZeroFlagNative(value: Boolean)

    fun stackPointer(): Int = getStackPointerNative()
    private external fun getStackPointerNative(): Int

    /** Latches a PIO port B interrupt with the given vector; a negative vector cancels it. */
    fun requestCassetteInterrupt(vector: Int) = requestCassetteInterruptNative(vector)
    private external fun requestCassetteInterruptNative(vector: Int)

    internal val commandHistory = ABC80CommandHistory()

    private fun recordCommand(code: Int) {
        commandHistory.record(code)
    }

    fun sendKey(code: Int) {
        recordCommand(code)
        sendKeyNative(code)
    }
    private external fun sendKeyNative(code: Int)
    fun pressKey(code: Int) {
        recordCommand(code)
        pressKeyNative(code)
    }
    private external fun pressKeyNative(code: Int)
    fun releaseKey(code: Int) = releaseKeyNative(code)
    private external fun releaseKeyNative(code: Int)
    fun releaseAllKeys() = releaseAllKeysNative()
    private external fun releaseAllKeysNative()

    fun readSoundSamples(count: Int): FloatArray {
        require(count in 1..4410) { "Sound sample count must be between 1 and 4410" }
        return readSoundSamplesNative(count)
    }
    private external fun readSoundSamplesNative(count: Int): FloatArray

    fun getMemory(): ByteArray = getMemoryNative()
    private external fun getMemoryNative(): ByteArray

    fun getCPU(): ByteArray = getProcessorStateNative()
    private external fun getProcessorStateNative(): ByteArray

    fun updateCPU(cpu: com.aboveware.aboveabc80.core.CPU) = updateCPUNative(cpu)
    private external fun updateCPUNative(cpu: com.aboveware.aboveabc80.core.CPU)

    fun getCPUString(): String = getProcessorStateStringNative()
    private external fun getProcessorStateStringNative(): String

    fun setProcessorState(state: ByteArray) = setProcessorStateNative(state)
    private external fun setProcessorStateNative(state: ByteArray)

    fun setZ80Registers(regs: Z80Registers) = setZ80RegistersNative(regs)
    private external fun setZ80RegistersNative(regs: Z80Registers)

    fun setRegisterA(value: Int) = setRegisterANative(value)
    private external fun setRegisterANative(value: Int)

    fun setRegisterHL(value: Int) = setRegisterHLNative(value)
    private external fun setRegisterHLNative(value: Int)

    fun setRegisterL(value: Int) = setRegisterLNative(value)
    private external fun setRegisterLNative(value: Int)

    fun addMemoryReadWatcher(address: String, watcher: MemoryReadWatcher) =
        addMemoryReadWatcherNative(label(address), watcher)

    fun addMemoryReadWatcher(address: Int, watcher: MemoryReadWatcher) =
        addMemoryReadWatcherNative(address, watcher)

    private external fun addMemoryReadWatcherNative(address: Int, watcher: MemoryReadWatcher)

    fun removeMemoryReadWatcher(address: String, watcher: MemoryReadWatcher) =
        removeMemoryReadWatcherNative(label(address), watcher)

    fun removeMemoryReadWatcher(address: Int, watcher: MemoryReadWatcher) =
        removeMemoryReadWatcherNative(address, watcher)

    private external fun removeMemoryReadWatcherNative(address: Int, watcher: MemoryReadWatcher)

    fun addMemoryWriteWatcher(address: String, watcher: MemoryWriteWatcher) =
        addMemoryWriteWatcherNative(label(address), watcher)

    fun addMemoryWriteWatcher(address: Int, watcher: MemoryWriteWatcher) =
        addMemoryWriteWatcherNative(address, watcher)

    private external fun addMemoryWriteWatcherNative(address: Int, watcher: MemoryWriteWatcher)

    fun removeMemoryWriteWatcher(address: String, watcher: MemoryWriteWatcher) =
        removeMemoryWriteWatcherNative(label(address), watcher)

    fun removeMemoryWriteWatcher(address: Int, watcher: MemoryWriteWatcher) =
        removeMemoryWriteWatcherNative(address, watcher)

    private external fun removeMemoryWriteWatcherNative(address: Int, watcher: MemoryWriteWatcher)

    fun patchMemory(address: String, value: Int) = patchMemory(label(address), value)
    external fun patchMemory(address: Int, value: Int)
    external fun peekMemory(address: Int): Int
    external fun clearPatches()

    fun setOnExecuteForListener(listener: OnExecuteForListener?) =
        setOnExecuteForListenerNative(listener)

    private external fun setOnExecuteForListenerNative(listener: OnExecuteForListener?)

    fun thaw() = freezeNative(false)
    fun freeze(freezing: Boolean = true) = freezeNative(freezing)

    private external fun freezeNative(freeze: Boolean)

    fun enableLogging(enable: Boolean) = enableLoggingNative(enable)
    private external fun enableLoggingNative(enable: Boolean)

    fun setFastSpeed(fast: Boolean) = setFastSpeedNative(fast)
    private external fun setFastSpeedNative(fast: Boolean)

    fun setCpuFrequency(frequency: Int) = setCpuFrequencyNative(frequency)
    private external fun setCpuFrequencyNative(frequency: Int)

    fun step() = stepNative()
    private external fun stepNative()

    fun isFastSpeed(): Boolean = isFastSpeedNative()
    private external fun isFastSpeedNative(): Boolean

    fun getTStates(): Long = getTStatesNative()
    private external fun getTStatesNative(): Long

    fun copyToMemory(address: Int, data: ByteArray) = copyToMemoryNative(address, data)
    private external fun copyToMemoryNative(address: Int, data: ByteArray)

    fun setRam(address: Int, data: ByteArray) = setRamNative(address, data)
    private external fun setRamNative(address: Int, data: ByteArray)

    fun unpackArchive(data: ByteArray): List<ArchivedFile> {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.remaining() >= Int.SIZE_BYTES) { "Ogiltigt arkivformat" }

        val fileCount = buffer.int.toLong() and 0xffffffffL
        require(fileCount <= Int.MAX_VALUE) { "För många filer i arkivet" }
        val files = ArrayList<ArchivedFile>(fileCount.toInt())

        repeat(fileCount.toInt()) {
            require(buffer.remaining() >= 13 + Int.SIZE_BYTES) {
                "Ofullständig arkivpost"
            }

            val nameBytes = ByteArray(13)
            buffer.get(nameBytes)
            val name = nameBytes
                .takeWhile { it.toInt() != 0 }
                .toByteArray()
                .toString(Charsets.US_ASCII)

            val contentSize = buffer.int.toLong() and 0xffffffffL
            require(contentSize <= buffer.remaining()) {
                "Ogiltig innehållsstorlek för $name"
            }

            val content = ByteArray(contentSize.toInt())
            buffer.get(content)
            files += ArchivedFile(name, content)
        }

        return files
    }

    fun extractArkArchive(arkData: ByteArray): List<ArchivedFile> =
        unpackArchive(extractArkArchiveNative(arkData))

    fun decompressSqueezed(data: ByteArray): ByteArray? =
        decompressSqueezedNative(data)

    fun extractLbrArchive(data: ByteArray): List<ArchivedFile> {
        val files = mutableListOf<ArchivedFile>()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        if (data.size < 32) return emptyList()

        val headerStatus = data[0].toInt() and 0xFF
        if (headerStatus != 0) return emptyList()

        // The first directory entry is the directory control entry. Its
        // LENGTH field at bytes 14-15 is the directory size in sectors.
        buffer.position(14)
        val directoryLengthSectors = buffer.short.toInt() and 0xFFFF
        val directoryEntries = directoryLengthSectors * 4
        if (directoryLengthSectors == 0 || directoryEntries * 32 > data.size) {
            return emptyList()
        }

        for (i in 1 until directoryEntries) {
            val entryPos = i * 32
            buffer.position(entryPos)
            val status = buffer.get().toInt() and 0xFF
            if (status == 0xFF) break
            if (status != 0x00) continue

            val nameBytes = ByteArray(8)
            buffer.get(nameBytes)
            val name = nameBytes.map { (it.toInt() and 0x7F).toByte() }.toByteArray()
                .decodeToString().trim()

            val extBytes = ByteArray(3)
            buffer.get(extBytes)
            val ext = extBytes.map { (it.toInt() and 0x7F).toByte() }.toByteArray()
                .decodeToString().trim()

            val offsetSectors = buffer.short.toInt() and 0xFFFF
            val lengthSectors = buffer.short.toInt() and 0xFFFF
            val padCount = data[entryPos + 26].toInt() and 0xFF

            val offset = offsetSectors * 128
            val length = lengthSectors * 128

            if (offset + length <= data.size && length > 0) {
                val content = data.copyOfRange(offset, offset + length)
                val actualLength = (content.size - padCount).coerceAtLeast(0)
                val finalContent = content.copyOf(actualLength)
                files.add(ArchivedFile("$name.$ext", finalContent))
            }
        }
        Abc80Log.terminal("Extracted ${files.size} files from LBR archive")
        return files
    }

    private external fun extractArkArchiveNative(arkData: ByteArray): ByteArray
    private external fun decompressSqueezedNative(data: ByteArray): ByteArray?
}

expect fun loadNativeLibrary()
