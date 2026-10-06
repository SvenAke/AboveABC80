package com.aboveware.aboveabc80

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.lang.Integer.max
import kotlin.experimental.and
import kotlin.experimental.or

@ExperimentalUnsignedTypes
@OptIn(ExperimentalUnsignedTypes::class)
class Abc80FloppyLayout(formatName: String, val drive: Int) {
    private val formats = mapOf(
        "fd2" to Abc80Floppy.DiskFormat(SSSD(), 0, 6, directorySectors(), 8),
        "dd80" to Abc80Floppy.DiskFormat(SSSD(), 0, 6, directorySectors(), 8, 0x6ef, 0x300, 0x600),
        "fd2d" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "dd82" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "abc830" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "mo" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, directorySectors(), 8),
        "abc830-ufd" to Abc80Floppy.DiskFormat(SSDD(), 0, 6, Abc80Floppy.DirectorySectors(16, 0), 16),
        "fd4d" to Abc80Floppy.DiskFormat(DSDD(), 0, 6, directorySectors(), 8),
        "dd84" to Abc80Floppy.DiskFormat(DSDD(), 0, 6, directorySectors(), 8),
        "abc832" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, Abc80Floppy.DirectorySectors(16, 0), 16),
        "abc832-ufd" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, directorySectors(), 8),
        "abc834" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, Abc80Floppy.DirectorySectors(16, 0), 16),
        "mf" to Abc80Floppy.DiskFormat(DSQD(), 2, 14, Abc80Floppy.DirectorySectors(16, 0), 16)
    )

    private fun directorySectors() = Abc80Floppy.DirectorySectors(8, 16)

    // Format	Encoding	Sector size	Sectors/track	Tracks
    // SSSD		FM(?)		256 bytes	 8		        40          80kB
    // SSDD		MFM		    256 bytes	16		        40          160kB
    // DSDD		MFM		    256 bytes	16		        40 x2       320kB
    // DSQD		MFM		    256 bytes	16		        80 x2       640kB
    private fun DSQD() = Abc80Floppy.SectorFormat(80, 2, 16)
    private fun DSDD() = Abc80Floppy.SectorFormat(40, 2, 16)
    private fun SSDD() = Abc80Floppy.SectorFormat(40, 1, 16)
    private fun SSSD() = Abc80Floppy.SectorFormat(40, 1, 8)

    val format = formats[formatName]

    companion object {
        private val imageFormats = listOf("fd2", "abc830", "fd4d", "abc832")

        /** Lists "NAME.EXT" and size in bytes for each file on a raw disk image, or null if the size matches no known format. */
        fun list(image: ByteArray): List<Pair<String, Int>>? {
            val layout = imageFormats.map { Abc80FloppyLayout(it, 0) }
                .firstOrNull { it.format?.sectorFormat?.size == image.size } ?: return null
            return layout.dir(image).map { entry ->
                val (name, length) = entry.split("!")
                val base = name.take(8).trim()
                val ext = name.drop(8).trim()
                (if (ext.isEmpty()) base else "$base.$ext") to length.toInt()
            }
        }
    }

    private var offset = 0
    private var fileNumber: Int = 0
    private var libraryOffset: Int = 0
    private var sector = 0L
    var status: UByte = 0b11001001u
    private var randomAccessFile: RandomAccessFile? = null
    private fun size() = format!!.sectorFormat.size
    private var buffer = ByteArray(256) { 0x00 }
    private var pointer = -1
    fun open(create: Boolean = false): Abc80FloppyLayout {
        close()
        Abc80Log.floppy("OPEN ${diskPath()}")
        val file = File(diskPath())
        if (!file.exists() || create) {
            Abc80Log.floppy("CREATE")
            file.delete()
            file.createNewFile()
        }
        if (file.exists()) {
            randomAccessFile = RandomAccessFile(file, "rwd")
            Abc80Log.floppy("OPEN ${diskPath()} exists ${volumeName()}")
        }
        return this
    }

    private fun diskPath(): String {
        val dir = File(getAppStorageDir(), "dr").apply { if (!exists()) mkdirs() }
        return File(dir, "$drive.dsk").absolutePath
    }

    fun read(): Byte {
        if (pointer >= buffer.size) return 0x00
        return buffer[pointer++]
    }

    fun write(data: UByte) {
        buffer[pointer++] = data.toByte()
        if (pointer > 255) {
            randomAccessFile?.apply {
                seek(offset.toLong())
                write(buffer)
            }
        }
    }

    fun open(physicalFileNumber: Int, physicalSectorNumber: Int) {
        offset = offset(physicalFileNumber, physicalSectorNumber)
        block(offset)
        pointer = 0
    }

    private fun offset(physicalFileNumber: Int, physicalSectorNumber: Int): Int {
        fileNumber = physicalFileNumber.and(0b00001111)
        libraryOffset = physicalFileNumber.and(0b11110000).shr(4)
        sector = physicalSectorNumber.shr(5) * 256L
        val secPerCluster = 1.shl(format!!.clusterShift)

        var offset =
            ((physicalFileNumber.shl(3) + physicalSectorNumber.shr(5)) * secPerCluster +
                    physicalSectorNumber.and(31))
        val interleaveFactory = 7
        val interleaveMask = 15
        offset = offset.and(interleaveMask.inv()).or((offset * interleaveFactory).and(interleaveMask))
        offset = offset.shl(8)
        return offset
    }

    private fun block(address: Int) {
        randomAccessFile?.apply {
            seek(address.toLong())
            read(buffer)
        }
    }

    fun close() {
        Abc80Log.floppy("CLOSE")
        randomAccessFile?.close()
        randomAccessFile = null
    }

    private fun volumeName(): String {
        if (randomAccessFile?.length()?.toInt() != size())
            return "NO DISKETTE"
        block(0)
        return if (buffer[0x87].toInt().toChar() !in 'A'..'~') "DR$drive:"
        else {
            val ascii = StringBuilder()
            for (index in 0x87..0xff) {
                if (buffer[index] == 0x0d.toByte()) return ascii.toString()
                val asChar = buffer[index].toInt().toChar()
                ascii.append(if (asChar in ' '..'~') asChar else ".")
            }
            ascii.toString()
        }
    }

    fun format() = load(ByteArrayInputStream(create()), "")

    fun load(stream: InputStream, name: String): Boolean = try {
        open(create = true)
        Abc80Log.floppy("FLOPPY $name ${stream.available().toLong()} ${size()} $randomAccessFile")
        when {
            stream.available() != size() -> false
            randomAccessFile != null -> {
                randomAccessFile?.apply {
                    seek(0)
                    while (stream.read(buffer) == buffer.size) {
                        write(buffer)
                    }
                    seek(0)
                    close()
                    open()
                }
                true
            }
            else -> false
        }
    } catch (e: IOException) {
        Abc80Log.floppy("LOAD $e")
        false
    }
    /**
     * Read 'n' parse directory sector
     */
    private fun dir(bytes: ByteArray): MutableList<String> {
        val usedDirectorySectors = usedDirectorySectors(bytes)
        val filenames = mutableListOf<String>()
        for (sectorNumber in 16 until 16 + format!!.numberOfDirectorySectors) {
            if (!usedDirectorySectors[sectorNumber - 16]) continue
            val sectorStartOffset = sectorNumber.shl(8) + (sectorNumber - 16) * 0x600
            for (index in 16 until 256 step 16) {
                val offset = sectorStartOffset + index
                if (bytes[offset].toInt() == 0x00 || bytes[offset].toInt() == -1) continue
                var hsect = bytes[offset].toInt().shl(3) + bytes[offset + 1].toInt().shr(5)
                hsect = hsect.shl(format.clusterShift)
                if (hsect == 0 ||
                    (bytes[offset + 1].toInt() and bytes[offset + 1].toInt()) == -1 ||
                    bytes[offset + 4].toInt() == 0x00 ||
                    bytes[offset + 4].toInt() == -1
                ) continue   // Empty slot

                val length = getFileSize(bytes, offset(bytes[offset].toUByte().toInt(), bytes[offset + 1].toUByte().toInt()))
                val name = StringBuilder()
                for (n in 4..14)
                    name.append(bytes[offset + n].toInt().toChar())
                name.append("!")
                name.append(length)
                filenames.add(name.toString())
            }
        }
        return filenames
    }

    private fun getFileSize(bytes: ByteArray, oo: Int): Int {
        var sum = 0
        for (k in 4..240 step 2) {
            if (bytes[oo + k].toUByte().toInt() == 255) break
            sum += bytes[oo + k + 1].and(31) + 1
        }
        return sum * 256
    }

    private fun usedDirectorySectors(bytes: ByteArray): MutableList<Boolean> {
        val userDirectorySectors = mutableListOf<Boolean>()
        for (index in 0..7) {
            userDirectorySectors.add(bytes[format!!.usedDirectorySectorsIndex + index].toUByte().toInt() > 1)
        }
        return userDirectorySectors
    }

    fun eject() = if (File(diskPath()).delete()) {
        close()
        Abc80Log.floppy("DELETE ${diskPath()}")
        true
    } else
        false


    /**
     * @return true if the disk has a floppy inserted.
     */
    fun isValid(): Boolean {
        return (randomAccessFile != null &&
                randomAccessFile?.length()?.toInt() == size()).apply {
            Abc80Log.floppy("IsValid $this ${randomAccessFile?.length()} == ${size()}")
        }
    }

    /**
     * Save this disk to the stream given
     *
     * @param stream the stream to write this floppy to
     * @return true if we succeeded in our endeavour
     */
    fun save(stream: OutputStream) = try {
        Abc80Log.floppy("SAVE")
        randomAccessFile?.apply {
            while (read(buffer) == buffer.size) {
                stream.write(buffer)
            }
            Abc80Log.floppy("SAVE $filePointer")
            eject()
            return true
        }
        false
    } catch (e: IOException) {
        Abc80Log.floppy("SAVE $e")
        false
    }

    /**
     * @return the floppy size, in bytes
     */
    private fun unAllocatedSize(): Int {
        var size = 0
        randomAccessFile?.apply {
            format?.apply {
                for (index in format.unAllocatedIndex..format.unAllocatedIndex + 0xa0) {
                    seek(index.toLong())
                    if (read() == 0x00)
                        size += 8
                }
            }
        }
        return size
    }

    /**
     * @return the floppy size, in bytes
     */
    private fun allocatedSize(): Int {
        var size = 0
        randomAccessFile?.apply {
            format?.apply {
                for (index in format.allocatedIndex..format.allocatedIndex + 0xa0) {
                    seek(index.toLong())
                    size += 8 - read().countOneBits()
                }
            }
        }
        return size
    }

    /**
     * Create an empty disk image.
     */
    fun create(): ByteArray {
        var image = ByteArray(0) { 0x40 }
        format?.apply {
            image = ByteArray(sectorFormat.size) { 0x40 }

            image.fill(0xff.toByte(), format.unAllocatedIndex, 0x103)
            image.fill(0x00, 0x103, format.unAllocatedIndex + (sectorFormat.c * sectorFormat.s).shr(3))

            val bitmapIndex = systemSector.shl(8)
            // Create empty bitmap
            image.fill(0x00, bitmapIndex, bitmapIndex + 256)

            /* Mark nonexistent clusters busy */
            for (index in sectorFormat.sectors.shr(clusterShift)..239 * 8) {
                val i = bitmapIndex + index.shr(3)
                image[i] = image[i].or(0x80.shr(index.and(0x07)).toByte())
            }

            // 239..254 is the MFD (master file table) bitmap
            image.fill(0x01, bitmapIndex + 239, bitmapIndex + 239 + numberOfDirectorySectors)

            /* This is claimed in a message found on ABC-klubben MSG */
            image[bitmapIndex + 255] = clusterShift.toByte()

            // Mark all the system sectors used
            var startIndex = numberOfDirectorySectors + max(directorySectors.a, directorySectors.b)
            val sectorsPerCluster = 1.shl(clusterShift)

            startIndex = (startIndex + sectorsPerCluster - 1).shr(clusterShift)
            for (index in 0 until startIndex) {
                val i = bitmapIndex + index.shr(3)
                image[i] = image[i].or(0x80.shr(index.and(0x07)).toByte())
            }

            // Copy bitmap to backup empty bitmap (= bad block map)
            image.copyInto(image, bitmapIndex + 256, bitmapIndex, bitmapIndex + 256)

            // Create MFD directory sectors
            val directorySectorIndex = directorySectors.a.shl(8)
            for (index in 0..numberOfDirectorySectors) {
                val fromIndex = directorySectorIndex + index * 256
                image.fill(0x00, fromIndex, fromIndex + 16)
                image.fill(0xff.toByte(), fromIndex + 16, fromIndex + 256)
            }
            // Create backup MFD if it exists
            if (directorySectors.b != 0) {
                val toIndex = directorySectors.b.shl(8)
                val fromIndex = directorySectors.a.shl(8)
                val size = numberOfDirectorySectors.shl(8)
                image.copyInto(image, toIndex, fromIndex, fromIndex + size)
            }
            // todo We need to copy the allocated directory bitmap from 0x600 to 0xa00
            // I don't know way but the c-program (abcwrite.c) does create the map at 0x600
            // but the driver code looks at 0xa00.
            image.copyInto(image, bitmapIndex + 0x400, bitmapIndex, bitmapIndex + 256)
        }
        return image
    }

}