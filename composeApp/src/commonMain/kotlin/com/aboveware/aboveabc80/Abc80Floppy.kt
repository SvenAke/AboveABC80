package com.aboveware.aboveabc80

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

        private val disks = mutableMapOf<Int, Abc80FloppyLayout>()
    }

    val disks = DiskDrive()

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

    private val route = mutableListOf<Int>()

    override fun onWrite(port: UByte, data: UByte, status: UByte) {
        when (port.toInt()) {
            0x00 -> {
                when (stateMachine.state) {
                    StateMachine.State.STARTED -> {
                        if (route.size == 4) {
                            if (route[0].and(0x0C) == 0x0C) {
                                stateMachine.state = StateMachine.State.WRITING
                                drive = route[1].and(0x07).toUByte()
                                disk.open(route[2], route[3])
                                disk.write(data)
                            }
                            route.clear()
                        }
                        route.add(data.toInt())
                    }
                    StateMachine.State.WRITING -> {
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
            }
            0x04 -> {
                reset()
            }
            else -> {
            }
        }
    }

    internal fun reset() {
        // todo Don not call disks.close()
    }

    override fun onRead(port: UByte): UByte {
        return when (port.toInt()) {
            // Read status
            0x00 -> {
                when (stateMachine.state) {
                    StateMachine.State.UNINITIALIZED -> 0xffu
                    StateMachine.State.SELECTED -> drive
                    StateMachine.State.STARTED -> {
                        if (route.size == 4) {
                            if (route[0].and(0x01) == 0x01) {
                                stateMachine.state = StateMachine.State.READING
                                drive = route[1].and(0x07).toUByte()
                                if (progressOperation.isNotEmpty()) {
                                    progressDrive = "DR$drive: "
                                    progress(text = progressText)
                                }
                                disk.open(route[2], route[3])
                                return disk.read().toUByte()
                            }
                            route.clear()
                        }
                        0Xffu
                    }
                    StateMachine.State.READING -> {
                        disk.read().toUByte()
                    }
                    StateMachine.State.WRITING -> {
                        0x00u
                    }
                }
            }
            0x01 -> {
                when (stateMachine.state) {
                    StateMachine.State.READING -> {
                        disk.status
                    }
                    else -> status
                }
            }
            else -> {
                0xffu
            }
        }
    }
}
