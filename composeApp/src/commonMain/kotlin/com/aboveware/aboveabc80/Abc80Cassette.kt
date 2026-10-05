package com.aboveware.aboveabc80

import java.io.InputStream
import java.io.OutputStream

/**
 * The cassette in the recorder. The tape is kept in memory and persisted as a local file:
 * an 8-byte big-endian tape position followed by the tape contents (blocks of [Abc80Program.Block.BYTES]).
 */
@OptIn(ExperimentalUnsignedTypes::class)
object Abc80Cassette {
    private const val FOLDER = "cassette"
    private const val FILE = "current.tape"
    private const val HEADER = 8
    private const val BLOCK = Abc80Program.Block.BYTES

    class FileInfo {
        var name = ""
        var length = -1L
        var position = 0L
        var current = false
        var valid = false

        val displayName get() = name.eightPointThree().ifEmpty { "-" }
    }

    private class BlockVerificationException : Exception("Block verification error")

    private var tape = ByteArray(0)
    private var position = 0
    private var loaded = false
    private var inserted = false

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val stored = loadLocalFile(FOLDER, FILE)
        if (stored != null && stored.size >= HEADER) {
            inserted = true
            var pos = 0L
            for (i in 0 until HEADER) pos = (pos shl 8) or (stored[i].toLong() and 0xFF)
            tape = stored.copyOfRange(HEADER, stored.size)
            position = pos.toInt().coerceIn(0, tape.size)
        }
        Abc80CassetteStatus.hasTape = inserted
    }

    private fun persist() {
        val out = ByteArray(HEADER + tape.size)
        var pos = position.toLong()
        for (i in HEADER - 1 downTo 0) {
            out[i] = pos.toByte()
            pos = pos shr 8
        }
        tape.copyInto(out, HEADER)
        if (inserted) saveLocalFile(FOLDER, FILE, out) else deleteLocalFile(FOLDER, FILE)
        Abc80CassetteStatus.hasTape = inserted
    }

    fun anyCassette(): Boolean {
        ensureLoaded()
        return inserted
    }

    /** Overwrites the tape at the current position with the blocks of [program]. */
    fun save(program: Abc80Program) {
        if (program.fileBlocks.isEmpty()) return
        val data = program.fileBlocks.flatMap { it.toByteArray().asList() }.toByteArray()
        write(data)
    }

    private fun write(data: ByteArray) {
        ensureLoaded()
        inserted = true
        // Add some space after real data.
        val gap = Abc80Program.DataBlock(0).toByteArray()
        val end = position + data.size + gap.size
        val result = ByteArray(maxOf(tape.size, end))
        tape.copyInto(result)
        data.copyInto(result, position)
        gap.copyInto(result, position + data.size)
        tape = result
        position = end
        persist()
    }

    /** Loads the next file into [program] if its name matches (or [name] is a wildcard). */
    fun load(name: String, program: Abc80Program): FileInfo {
        if (program.isNotEmpty()) return FileInfo()
        program.init(name.eightPointThree())
        return load(program, -1)
    }

    private fun load(program: Abc80Program?, currentPosition: Int): FileInfo {
        ensureLoaded()
        val info = FileInfo()
        info.length = position.toLong()
        info.position = position.toLong()
        info.current = position == currentPosition
        var pos = position
        try {
            if (pos + BLOCK <= tape.size) {
                val nameBlock = Abc80Program.NameBlock()
                nameBlock.load(tape.copyOfRange(pos, pos + BLOCK))
                if (!nameBlock.verify()) throw BlockVerificationException()
                pos += BLOCK
                info.name = nameBlock.name
                Abc80Log.cassette("READ $nameBlock")
                program?.add(nameBlock)
                var blockNumber = 0
                while (pos + BLOCK <= tape.size) {
                    val dataBlock = Abc80Program.DataBlock(blockNumber++)
                    dataBlock.load(tape.copyOfRange(pos, pos + BLOCK))
                    pos += BLOCK
                    if (!dataBlock.verify()) break
                    program?.add(dataBlock)
                }
                info.length = (pos - position - BLOCK).toLong()
                position = pos
                persist()
            } else {
                program?.markEndOfTape()
                info.current = false
                return info
            }
        } catch (ex: BlockVerificationException) {
            Abc80Log.cassette("LOAD ${ex.message}")
            program?.markError()
            return info
        }
        info.valid = true
        return info
    }

    /** Lists the files on the tape; the file at the current tape position is marked as current. */
    fun list(): List<FileInfo> {
        ensureLoaded()
        val saved = position
        position = 0
        val infos = mutableListOf<FileInfo>()
        var info = load(null, saved)
        var anyCurrent = info.current
        while (info.valid) {
            infos.add(info)
            info = load(null, saved)
            if (info.current) anyCurrent = true
        }
        position = saved
        if (!anyCurrent) infos.add(FileInfo().apply {
            valid = true
            current = true
            name = "           "
            position = saved.toLong()
        })
        return infos
    }

    fun format() {
        Abc80Log.cassette("FORMAT")
        tape = ByteArray(0)
        position = 0
        loaded = true
        inserted = true
        persist()
    }

    fun rewind() {
        ensureLoaded()
        position = 0
        persist()
    }

    fun forward(): List<FileInfo> {
        val infos = list()
        val current = infos.indexOfFirst { it.current }
        if (current != -1) {
            if (current != infos.size - 1) position = infos[current + 1].position.toInt()
            else if (infos[current].length != -1L) position = (infos[current].position + infos[current].length).toInt()
            persist()
        }
        return list()
    }

    fun back(): List<FileInfo> {
        val infos = list()
        val current = infos.indexOfFirst { it.current }
        if (current > 0) {
            position = infos[current - 1].position.toInt()
            persist()
        }
        return list()
    }

    /** Imports a WAV/RAW recording (decoded) or a custom tape file (raw blocks); returns true on success. */
    fun import(stream: InputStream, name: String): Boolean =
        if (name.endsWith("WAV", true) || name.endsWith("RAW", true)) importWave(stream, name)
        else importCustom(stream, name)

    private fun importWave(stream: InputStream, name: String): Boolean {
        Abc80CassetteStatus.tapeName = ""
        val progressText = "Indexing cassette"
        Abc80CassetteStatus.show(progressText)
        try {
            val decoder = Abc80CassetteDecoder(progressText)
            if (!decoder.decode(stream)) return false
            format()
            decoder.files().forEach { write(it.rawData()) }
            rewind()
            Abc80CassetteStatus.tapeName = name
            return true
        } finally {
            Abc80CassetteStatus.hide()
        }
    }

    private fun importCustom(stream: InputStream, name: String): Boolean {
        Abc80CassetteStatus.tapeName = ""
        return try {
            val bytes = stream.readBytes()
            tape = bytes
            position = 0
            loaded = true
            inserted = true
            persist()
            Abc80CassetteStatus.tapeName = name.substringAfterLast("/")
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Exports the whole tape as raw blocks. */
    fun export(stream: OutputStream) {
        ensureLoaded()
        stream.write(tape)
        stream.flush()
    }

    fun exportBytes(): ByteArray {
        ensureLoaded()
        return tape.copyOf()
    }

    fun eject() {
        tape = ByteArray(0)
        position = 0
        loaded = true
        inserted = false
        Abc80CassetteStatus.tapeName = ""
        persist()
    }
}
