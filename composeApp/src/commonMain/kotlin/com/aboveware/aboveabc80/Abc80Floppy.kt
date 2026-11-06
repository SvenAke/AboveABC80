package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.terminal.TerminalManager
import java.io.File
@ExperimentalUnsignedTypes
@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
class Abc80Floppy : Abc80Bus.BusInterface {
    /**
     * ABC80/800 floppy disks come in a few formats; the "standard" ones are
     * SSSD (80K), SSDD (160K), and DSQD (640K), with some third party drives
     * being DSDD (320K).  The basic formatting parameters are
     *
     * Format	Encoding	Sector size	Sectors/track	Tracks
     * SSSD		FM(?)		256 bytes	 8		        40
     * SSDD		MFM		    256 bytes	16		        40
     * DSDD		MFM		    256 bytes	16		        40 x2
     * DSQD		MFM		    256 bytes	16		        80 x2
     * 8"		?		    ?		     ?		        77 x?
     *
     * Värt att påpeka att detta är parametrar för ABC830/FD2D/DataDisc 82,d.v.s. SSDD.
     * SSSD (FD2/DataDisc 80):
     *      setfdprm /dev/fdX sd sect=8 ssize=256 head=1 cyl=40
     * DSDD (FD4?/DataDisc 84):
     *      setfdprm /dev/fdX sd sect=8 ssize=256 head=2 cyl=40
     * DSQD (ABC832/DataDisc 56):
     *      setfdprm /dev/fdX qd sect=16 ssize=256 head=2 cyl=80
     */

    data class SectorFormat(
        val c: Int = 0,
        val h: Int = 0,
        val s: Int = 0
    ) {
        val sectors = c * h * s                /* Total sectors (max 1911 clusters) */
        val size = sectors.shl(8)
    }

    data class DirectorySectors(
        val a: Int = 0,
        val b: Int = 0
    )

    data class DiskFormat(
        val sectorFormat: SectorFormat = SectorFormat(0, 0, 0),
        val clusterShift: Int = 0,               /* Size of a cluster, log2 */
        val systemSector: Int = 0,               /* First system sector (bitmap) */
        val directorySectors: DirectorySectors = DirectorySectors(0, 0),  /* First directory sector (main, backup) */
        val numberOfDirectorySectors: Int = 0,                        /* Number of directory sectors */
        val usedDirectorySectorsIndex: Int = 0x0AEF,
        val unAllocatedIndex: Int = 0x100,
        val allocatedIndex: Int = 0xA00
    )

/*
 * 8" floppies have a different logical sector/track
 * mapping than the physical one (which is 77/2/26), and
 * then the last virtual track is only partially filled.
    diskfmt( "abc838", 126, 2, 16, 4004, 2, 6, { 8, 16 }, 8 },
    diskfmt( "abc838-ufd", 126, 2, 16, 4004, 2, 14, { 16, 0 }, 16 },
    diskfmt( "sf", 126, 2, 16, 4004, 2, 14, { 16, 0 }, 16 },
    diskfmt( "hd", F(238, 8, 32), 5, 14, { 16, 0 }, 16 },
 */

    private var progressOperation = ""
    private var progressDrive = ""
    private var progressDialog: Boolean = false
    private var progressFilename = ""
    private val progressText
        get() = "$progressOperation $progressDrive$progressFilename..."

    /***
     * ABC 6-1X/ABC830 as determined by page 72 in bit-för-bit
     */
    internal var drive: UByte = 0b00000000u   // 0x00000ddd drive #
/*
    private val mo = DiskLayout(1, 40 * 1 * 16, "mo", abc80)
    private val mf = DiskLayout(4, 80 * 2 * 16, "mf", abc80)
    private val sf = DiskLayout(4, (77 * 2 - 1) * 26, "sf", abc80)
    private val hd = DiskLayout(32, 238 * 8 * 32, "hd", abc80)
*/

    class DiskDrive {
        operator fun get(drive: Int): Abc80FloppyLayout {
            if (!disks.containsKey(drive))
                disks[drive] = Abc80FloppyLayout("abc830", drive).open()  // todo check size first!
            return disks[drive]!!
        }

        operator fun set(drive: Int, iDrive: Abc80FloppyLayout) {
            disks[drive] = iDrive
        }

        fun close() {
            disks.forEach { (_, layout) -> layout.close() }
        }

        fun closeAndClear() {
            close()
            disks.clear()
        }

        private val disks = mutableMapOf<Int, Abc80FloppyLayout>()
    }

    val disks = DiskDrive()
    private val mountedNames = mutableMapOf<Int, String>()
    private val addressMarks = mutableMapOf<Int, TeleDiskImage>()
    private val addressRotations = IntArray(8)

    private fun addressMarkFile(drive: Int) = File(getComputerModelStorageDir("dr"), "$drive.td0")

    private val disk
        get() = disks[drive.toInt()]

    private class StateMachine {
        fun next() {
            state = when (state) {
                State.UNINITIALIZED -> State.SELECTED
                State.SELECTED -> State.SELECTED
                else -> state
            }
        }

        override fun toString(): String {
            return "$state"
        }

        enum class State {
            UNINITIALIZED,
            SELECTED,
            STARTED,
            READING,
            WRITING
        }

        var state = State.UNINITIALIZED
    }

    init {
        for (index in 0..7) disks[index]
    }

    /** Inserts a raw or TD0 disk image, preserving TD0 address marks alongside the writable drive image. */
    @Synchronized
    fun mount(drive: Int, image: ByteArray, name: String = "", restoreAddressMarks: Boolean = false): Boolean {
        val metadataFile = addressMarkFile(drive)
        val metadata = try {
            when {
                TeleDiskImage.hasSignature(image) -> TeleDiskImage.decode(image)
                restoreAddressMarks && metadataFile.isFile -> TeleDiskImage.decode(metadataFile.readBytes())
                else -> null
            }
        } catch (e: IllegalArgumentException) {
            Abc80Log.wtf("Cannot mount $name: ${e.message}")
            return false
        }
        val raw = if (TeleDiskImage.hasSignature(image)) requireNotNull(metadata).rawData else image
        if (metadata != null && metadata.rawData.size != raw.size) {
            Abc80Log.wtf("Cannot mount $name: saved TD0 geometry does not match drive image")
            return false
        }
        val format = Abc80FloppyLayout.detectFormatName(raw)
            ?: run {
                Abc80Log.wtf(
                    "${TerminalManager.computerModel.displayName} DR$drive: cannot mount $name (${image.size} bytes). " +
                        "Supported raw image sizes: 81920, 163840, 327680, 655360 bytes."
                )
                return false
            }
        disks[drive].close()
        val layout = Abc80FloppyLayout(format, drive)
        disks[drive] = layout
        val loaded = layout.load(java.io.ByteArrayInputStream(raw), name)
        cancelTransfer(drive)
        if (loaded) {
            if (metadata != null) {
                if (!restoreAddressMarks || TeleDiskImage.hasSignature(image)) metadataFile.writeBytes(image)
                addressMarks[drive] = metadata
            } else {
                check(!metadataFile.exists() || metadataFile.delete()) { "Could not remove stale TD0 metadata for DR$drive" }
                addressMarks.remove(drive)
            }
            addressRotations[drive] = 0
            mountedNames[drive] = name
            Abc80Log.diskette("${TerminalManager.computerModel.displayName} DR$drive: mounted $name, format=$format, size=${image.size}")
        } else {
            Abc80Log.wtf("${TerminalManager.computerModel.displayName} DR$drive: failed to mount $name")
        }
        return loaded
    }

    /** Removes the disk from the given drive. */
    @Synchronized
    fun unmount(drive: Int) {
        addressMarks.remove(drive)
        val metadataFile = addressMarkFile(drive)
        check(!metadataFile.exists() || metadataFile.delete()) { "Could not remove TD0 metadata for DR$drive" }
        disks[drive].eject()
        disks[drive] = Abc80FloppyLayout("abc830", drive).open()
        cancelTransfer(drive)
        mountedNames.remove(drive)
    }

    @Synchronized
    fun closeForComputerModelChange() {
        disks.closeAndClear()
        mountedNames.clear()
        addressMarks.clear()
        addressRotations.fill(0)
        sectorBuffers.forEach { it.fill(0) }
        transferPosition = 256
        commandError = false
        drive = 0u
        stateMachine.state = StateMachine.State.UNINITIALIZED
        route.clear()
    }

    private fun cancelTransfer(drive: Int) {
        if (drive == this.drive.toInt()) {
            stateMachine.state = StateMachine.State.SELECTED
            route.clear()
        }
    }

    private fun selectDrive(value: Int) {
        drive = (value and 7).toUByte()
        DiskController.instance.recordAccess(drive.toInt())
        if (drive.toInt() < DiskController.instance.maxDrives) {
            DiskController.instance.currentDriveIndex = drive.toInt()
        }
    }

    @Synchronized
    fun updateImage(drive: Int, name: String, update: (ByteArray) -> ByteArray) {
        check(mountedNames[drive] == name) { "The floppy disk in DR$drive: has been changed. Try again." }
        require(disks[drive].isValid()) { "The floppy disk in DR$drive: is not readable" }
        val image = update(disks[drive].readImage())
        val hasAddressMarks = addressMarks.containsKey(drive)
        if (!hasAddressMarks) check(saveLocalDisk(name, image)) { "Could not save the floppy disk $name" }
        check(mount(drive, image, name, restoreAddressMarks = hasAddressMarks)) {
            "Could not mount the updated floppy disk $name"
        }
    }

    @Synchronized
    fun snapshot(drive: Int): ByteArray = disks[drive].readImage()

    fun run() {}

    private val stateMachine = StateMachine()

    //                          7 : Not ready (door is open)
    //                          6 : Write protected disc
    //                          5 :
    //                          4 : Not found (disc error)
    //                          3 : CRC error
    //                          2 :
    //                          1 : Command error
    //                          0 : Busy

    private var status: UByte = 0b11001001u
    private var queriedDrive: Int = 0
    private var dos21CallBuffer: Int = 0
    private val sectorBuffers = Array(4) { ByteArray(256) }
    private var transferBuffer = 0
    private var transferPosition = 256
    private var commandError = false

    private fun readAddressMark(file: Int, sector: Int) {
        val physical = disk.physicalSectorForAddress(file, sector)
        val format = requireNotNull(disk.format).sectorFormat
        if (!disk.isValid() || physical !in 0 until format.sectors) {
            commandError = true
            Abc80Log.wtf("DR$drive: cannot read address mark outside a mounted disk")
            return
        }
        val rotation = addressRotations[drive.toInt()]++
        val doubleDensity = format.s == 16
        val mark = addressMarks[drive.toInt()]?.addressMark(physical, rotation)
            ?: TeleDiskImage.withAddressCrc(byteArrayOf(
                (physical / format.s % format.c).toByte(),
                (physical / (format.s * format.c)).toByte(),
                (rotation.mod(16) + 1).toByte(),
                if (doubleDensity) 1 else 0
            ), doubleDensity)
        mark.copyInto(sectorBuffers[transferBuffer])
    }

    private val route = mutableListOf<Int>()

    @Synchronized
    override fun onWrite(port: UByte, data: UByte, status: UByte) {
        when (port.toInt()) {
            0x00 -> {
                when (stateMachine.state) {
                    StateMachine.State.STARTED -> {
                        route.add(data.toInt())
                        if (route.size == 4) {
                            Abc80Log.floppy("ROUTE $route")
                            val dos21SystemBuffer = dos21CallBuffer == 0x01
                            dos21CallBuffer = 0
                            transferBuffer = route[1] ushr 6
                            transferPosition = 256
                            commandError = false
                            when {
                                route[0] == 0x10 && route[1].and(0x38) == 0x10 -> {
                                    selectDrive(route[1])
                                    readAddressMark(route[2], route[3])
                                    stateMachine.state = StateMachine.State.SELECTED
                                }
                                route[0].and(0x03) != 0 -> {
                                    selectDrive(route[1])
                                    if (progressOperation.isNotEmpty()) {
                                        progressDrive = "DR$drive: "
                                        progress(text = progressText)
                                    }
                                    if (route[0].and(0x01) != 0) {
                                        disk.open(route[2], route[3], dos21SystemBuffer)
                                        repeat(256) { sectorBuffers[transferBuffer][it] = disk.read() }
                                    }
                                    transferPosition = if (route[0].and(0x02) != 0) 0 else 256
                                    stateMachine.state = if (transferPosition == 0) {
                                        StateMachine.State.READING
                                    } else {
                                        StateMachine.State.SELECTED
                                    }
                                }
                                route[0].and(0x0C) == 0x0C -> {
                                    stateMachine.state = StateMachine.State.WRITING
                                    transferPosition = 0
                                    selectDrive(route[1])
                                    disk.open(route[2], route[3], dos21SystemBuffer)
                                }
                            }
                            route.clear()
                        }
                    }
                    StateMachine.State.WRITING -> {
                        if (transferPosition < 256) sectorBuffers[transferBuffer][transferPosition++] = data.toByte()
                        disk.write(data)
                        this.status = 0b11001001u
                    }
                    else -> {
                    }
                }
            }
            0x01 -> {
                stateMachine.next()
            }
            0x02 -> {
                stateMachine.state = StateMachine.State.STARTED
                route.clear()
            }
            0x03 -> {
                dos21CallBuffer = data.toInt() and 0xFF
            }
            0x04 -> {
                reset()
            }
            0x06 -> {
                queriedDrive = data.toInt() and 0x03
            }
            else -> {
            }
        }
    }

    internal fun reset() {
        // todo Don not call disks.close()
    }

    @Synchronized
    override fun onRead(port: UByte): UByte {
        return when (port.toInt()) {
            // Read status
            0x00 -> {
                when (stateMachine.state) {
                    StateMachine.State.UNINITIALIZED -> 0xffu
                    StateMachine.State.SELECTED -> if (commandError) 0x80u else drive
                    StateMachine.State.STARTED -> 0xffu
                    StateMachine.State.READING -> {
                        if (transferPosition < 256) {
                            sectorBuffers[transferBuffer][transferPosition++].toUByte()
                        } else {
                            stateMachine.state = StateMachine.State.SELECTED
                            if (disk.isValid()) 0x00u else 0x80u
                        }
                    }
                    StateMachine.State.WRITING -> {
                        stateMachine.state = StateMachine.State.SELECTED
                        if (disk.isValid()) 0x00u else 0x80u
                    }
                }
            }
            0x01 -> {
                when (stateMachine.state) {
                    StateMachine.State.READING -> {
                        if (transferPosition < 256) 0b01001001u else 0b11001001u
                    }
                    StateMachine.State.WRITING -> {
                        if (disk.inTransfer()) 0b01001001u else 0b11001001u
                    }
                    else -> status
                }
            }
            0x06 -> {
                val targetDisk = disks[queriedDrive and 0x03]
                if (targetDisk.format?.clusterShift == 2) 0x08u else 0x0Cu
            }
            else -> {
                0xffu
            }
        }
    }
}
