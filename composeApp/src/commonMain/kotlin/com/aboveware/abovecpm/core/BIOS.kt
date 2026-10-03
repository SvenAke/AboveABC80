package com.aboveware.abovecpm.core

import com.aboveware.abovecpm.Assembler
import com.aboveware.abovecpm.NativeLib
import com.aboveware.abovecpm.Z80Registers
import com.aboveware.abovecpm.ZXLog
import com.aboveware.abovecpm.printer.VirtualPrinter
import com.aboveware.abovecpm.tape.TapeController
import com.aboveware.abovecpm.terminal.TerminalManager
import com.aboveware.abovecpm.toHex

/**
 * CP/M BIOS Emulation layer.
 * This class coordinates hardware emulation for console, disk, and printer
 * as expected by the BIOS routines in cpm.z80.
 */
class BIOS {
    companion object {
        val instance = BIOS()
    }

    private var isConnected = false
    private val biosWatchers = mutableMapOf<Int, NativeLib.MemoryReadWatcher>()
    private var rebootCount = 0
    @Volatile
    var isTransientProgramRunning = false
        private set
    private var transientProgramWatcher: NativeLib.MemoryReadWatcher? = null

    /**
     * Connects all hardware components required by the CP/M BIOS.
     */
    fun connect() {
        if (isConnected) return

        // 1. Terminal / Console (ADM3A)
        TerminalManager.activeTerminal.connect()

        // 2. Intercept BIOS and BDOS calls
        setupWatchers()
        applyBIOSPatches()
        patchPageZero()
        watchBDOS()
        watchWarmBoot()
        watchTransientProgramStart()

        isConnected = true
        ZXLog.terminal("CP/M BIOS connected and high-level interception enabled.")
    }

    /**
     * Disconnects all hardware components.
     */
    fun disconnect() {
        if (!isConnected) return

        TerminalManager.activeTerminal.disconnect()

        // Remove watchers
        removeBIOSWatchers()
        removeBDOSWatcher()
        transientProgramWatcher?.let {
            NativeLib.getObject().removeMemoryReadWatcher(0x0100, it)
            transientProgramWatcher = null
        }
        isTransientProgramRunning = false

        isConnected = false
        ZXLog.terminal("CP/M BIOS disconnected.")
    }

    /**
     * Performs a cold boot of the system.
     */
    fun restart() {
        rebootCount++
        val lib = NativeLib.getObject()
        // Thaw if frozen
        lib.freeze(false)
        // Reset memory to initial assembled state
        lib.copyToMemory(0, Assembler.instance.memory)
        // Clear terminal
        TerminalManager.activeTerminal.reset()
        TerminalManager.activeTerminal.clearInputBuffer()
        // Trigger the boot logic
        performBoot(isWarm = false)
    }

    private val biosFunctions = listOf(
        "BOOT" to "boot",
        "WBOOT" to "wboot",
        "CONST" to "const",
        "CONIN" to "conin",
        "CONOUT" to "conout",
        "LIST" to "list",
        "PUNCH" to "punch",
        "READER" to "reader",
        "HOME" to "home",
        "SELDSK" to "seldsk",
        "SETTRK" to "settrk",
        "SETSEC" to "setsec",
        "SETDMA" to "setdma",
        "READ" to "read",
        "WRITE" to "write",
        "PRSTAT" to "listst",
        "SECTRN" to "sectran",
    )

    private fun setupWatchers() {
        val biosAddr = Assembler.instance.labels["BIOS"]
        if (biosAddr == 0) {
            ZXLog.wtf("BIOS label not found!")
            return
        }

        val lib = NativeLib.getObject()

        biosFunctions.forEachIndexed { index, pair ->
            val name = pair.first
            val internalName = pair.second

            val jumpTableAddr = biosAddr + index * 3
            val implementationAddr = Assembler.instance.labels[internalName]
            ZXLog.terminal(
                "BIOS mapping: $name table=${jumpTableAddr.toHex(4)} " +
                    "implementation=${implementationAddr.toHex(4)}"
            )

            // HLE implementation
            val watcher = object : NativeLib.MemoryReadWatcher {
                override fun onRead(address: Int): Boolean {
                    val cpu = cpu()
                    val caller = lib.peekw(cpu.sp)

                    if (name == "BOOT" || name == "WBOOT") {
                        ZXLog.terminal(
                            "BIOS $name invoked at ${address.toHex(4)}, " +
                                "caller=${caller.toHex(4)}, PC=${cpu.pc.toHex(4)}, " +
                                "SP=${cpu.sp.toHex(4)}"
                        )
                    }

                    when (name) {
                        "BOOT" -> {
                            val applicationBoot = caller in 0x0100 until 0xE400
                            if (applicationBoot) {
                                ZXLog.terminal("Ignoring application BOOT request from ${caller.toHex(4)}")
                            } else {
                                performBoot(isWarm = false)
                            }
                        }

                        "WBOOT" -> {
                            performBoot(isWarm = true)
                        }

                        "CONST" -> {
                            val result =
                                if (TerminalManager.activeTerminal.hasChar()) 0xFF else 0x00
                            setResult(a = result)
                        }

                        "CONIN" -> {
                            val startRebootCount = rebootCount
                            while (!TerminalManager.activeTerminal.hasChar() && startRebootCount == rebootCount) {
                                Thread.sleep(10)
                            }
                            if (startRebootCount != rebootCount) return false

                            val char = TerminalManager.activeTerminal.getChar()
                            val caller = lib.peekw(cpu.sp)
                            ZXLog.terminal(
                                "BIOS CONIN: Returned 0x${
                                    char.toString(16).uppercase()
                                } ('${char.toChar()}') to PC=${caller.toHex(4)}"
                            )
                            setResult(a = char)
                            return false
                        }

                        "CONOUT" -> {
                            val char = cpu.bc and 0xFF
                            if (char != 0) {
                                // Standard CP/M prompt detection
                                ZXLog.terminal(
                                    "BIOS CONOUT: 0x${
                                        char.toString(16).uppercase()
                                    } ('${if (char >= 32) char.toChar() else '.'}')"
                                )
                                TerminalManager.activeTerminal.putChar(char.toChar())
                            }
                        }

                        "LIST" -> {
                            val char = cpu.bc and 0xFF
                            val caller = lib.peekw(cpu.sp)
                            ZXLog.terminal(
                                "BIOS LIST: 0x${
                                    char.toString(16).uppercase()
                                } ('${if (char >= 32) char.toChar() else '.'}') from PC=${
                                    caller.toHex(
                                        4
                                    )
                                }"
                            )
                            VirtualPrinter.instance.printChar(char.toChar())
                        }

                        "PUNCH" -> {
                            val char = cpu.bc and 0xFF
                            TapeController.instance.punchChar(char.toChar())
                        }

                        "READER" -> {
                            val char = TapeController.instance.readChar()
                            setResult(a = char.code)
                        }

                        "HOME" -> {
                            DiskController.instance.currentTrack = 0
                        }

                        "SELDSK" -> {
                            val drive = cpu.bc and 0xFF
                            val caller = lib.peekw(cpu.sp)

                            val dpbaseRaw = Assembler.instance.labels["dpbase"]
                            val dpbase = if (dpbaseRaw == 0) 0 else dpbaseRaw
                            ZXLog.terminal(
                                "BIOS SELDSK: Drive $drive from PC=${caller.toHex(4)}, dpbase=0x${
                                    dpbase.toHex(
                                        4
                                    )
                                }"
                            )

                            if (drive < DiskController.MAX_DRIVES && dpbase != 0) {
                                DiskController.instance.currentDriveIndex = drive

                                val floppy = DiskController.instance.getFloppy(drive)
                                val dpb = floppy?.dpb ?: DiskFormats.IBM_3740
                                
                                // Each drive must have a unique DPB memory address in Z80 RAM 
                                // so CP/M BDOS treats their geometries independently.
                                // We use a safer area further up in high memory.
                                val uniqueDpbAddr = 0xFF00 + (drive * 16)
                                
                                // Write DPB properties to the drive's unique memory space
                                lib.patchMemory(uniqueDpbAddr + 0, dpb.spt and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 1, (dpb.spt shr 8) and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 2, dpb.bsh and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 3, dpb.blm and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 4, dpb.exm and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 5, dpb.dsm and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 6, (dpb.dsm shr 8) and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 7, dpb.drm and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 8, (dpb.drm shr 8) and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 9, dpb.al0 and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 10, dpb.al1 and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 11, dpb.cks and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 12, (dpb.cks shr 8) and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 13, dpb.off and 0xFF)
                                lib.patchMemory(uniqueDpbAddr + 14, (dpb.off shr 8) and 0xFF)

                                val dphAddr = dpbase + (drive * 16)
                                
                                // Patch DPB pointer inside DPH (bytes 10-11)
                                lib.patchMemory(dphAddr + 10, uniqueDpbAddr and 0xFF)
                                lib.patchMemory(dphAddr + 11, (uniqueDpbAddr shr 8) and 0xFF)

                                // Patch XLT translation table pointer inside DPH (bytes 0-1)
                                if (dpb.spt == 26) {
                                    val transAddr = Assembler.instance.labels["trans"]
                                    if (transAddr != 0) {
                                        lib.patchMemory(dphAddr + 0, transAddr and 0xFF)
                                        lib.patchMemory(dphAddr + 1, (transAddr shr 8) and 0xFF)
                                    }
                                } else {
                                    lib.patchMemory(dphAddr + 0, 0)
                                    lib.patchMemory(dphAddr + 1, 0)
                                }

                                val savedDpbPtr = lib.peekw(dphAddr + 10)
                                val savedOff = lib.peekw(savedDpbPtr + 13)
                                ZXLog.diskett("BIOS SELDSK: Drive $drive DPH at 0x${dphAddr.toHex(4)} points to DPB at 0x${savedDpbPtr.toHex(4)} (OFF=$savedOff, SPT=${lib.peekw(savedDpbPtr)})")

                                // HLE bypasses CP/M's normal DPH copy, so keep its
                                // cached disk parameters synchronized explicitly.
                                val sectorsAddr = Assembler.instance.labels["SECTORS"]
                                val offsetAddr = Assembler.instance.labels["OFFSET"]
                                val xlateAddr = Assembler.instance.labels["XLATE"]
                                if (sectorsAddr != 0) {
                                    lib.patchMemory(sectorsAddr, dpb.spt and 0xFF)
                                    lib.patchMemory(sectorsAddr + 1, (dpb.spt shr 8) and 0xFF)
                                }
                                if (offsetAddr != 0) {
                                    lib.patchMemory(offsetAddr, dpb.off and 0xFF)
                                    lib.patchMemory(offsetAddr + 1, (dpb.off shr 8) and 0xFF)
                                }
                                if (xlateAddr != 0) {
                                    val translation = if (dpb.spt == 26) {
                                        Assembler.instance.labels["trans"]
                                    } else {
                                        0
                                    }
                                    lib.patchMemory(xlateAddr, translation and 0xFF)
                                    lib.patchMemory(xlateAddr + 1, (translation shr 8) and 0xFF)
                                }

                                val hl = dphAddr
                                setResult(a = 0, hl = hl) // A=0 on success, HL=header address
                            } else {
                                setResult(hl = 0) // Invalid drive
                            }
                        }

                        "SETTRK" -> {
                            val requestedTrack = cpu.bc and 0xFF
                            val floppy = DiskController.instance.getFloppy(
                                DiskController.instance.currentDriveIndex
                            )
                            val maxTrack = floppy?.tracks ?: 0
                            if (floppy != null && requestedTrack >= maxTrack) {
                                ZXLog.wtf(
                                    "Ignoring invalid SETTRK: requested=$requestedTrack, " +
                                        "maxTrack=$maxTrack, watcher=${address.toHex(4)}, " +
                                        "caller=${lib.peekw(cpu.sp).toHex(4)}"
                                )
                                return false
                            }
                            val track = if (floppy?.dpb?.spt == 40) {
                                requestedTrack + 1
                            } else {
                                requestedTrack
                            }
                            val caller = lib.peekw(cpu.sp)
                            ZXLog.terminal(
                                "BIOS SETTRK: requested=$requestedTrack physical=$track " +
                                    "watcher=${address.toHex(4)} caller=${caller.toHex(4)}"
                            )
                            DiskController.instance.currentTrack = track
                        }

                        "SETSEC" -> {
                            val sector = cpu.bc and 0xFF
                            val caller = lib.peekw(cpu.sp)
                            ZXLog.terminal(
                                "BIOS SETSEC: $sector watcher=${address.toHex(4)} " +
                                    "caller=${caller.toHex(4)}"
                            )
                            DiskController.instance.currentSector = sector
                        }

                        "SETDMA" -> {
                            val dma = cpu.bc and 0xFFFF
                            val caller = lib.peekw(cpu.sp)
                            ZXLog.terminal("BIOS SETDMA: 0x${dma.toHex(4)} from PC=${caller.toHex(4)}")
                            DiskController.instance.dmaAddress = dma
                        }

                        "READ" -> {
                            val result = DiskController.instance.biosRead()
                            setResult(a = result)
                        }

                        "WRITE" -> {
                            val result = DiskController.instance.biosWrite()
                            setResult(a = result)
                        }

                        "PRSTAT" -> setResult(a = 0x01)
                        "SECTRN" -> {
                            val logical = cpu.bc and 0xFFFF
                            val table = cpu.de and 0xFFFF
                            val floppy = DiskController.instance.getFloppy(
                                DiskController.instance.currentDriveIndex
                            )
                            if (floppy?.dpb?.spt != 26 || table == 0) {
                                setResult(hl = logical)
                            } else {
                                val physical = lib.peekMemory(table + logical)
                                setResult(hl = physical)
                            }
                        }
                    }
                    return false // Execute the patched RET (or JP for BOOT/WBOOT)
                }
            }

            // Watch jump table
            biosWatchers[jumpTableAddr] = watcher
            lib.addMemoryReadWatcher(jumpTableAddr, watcher)

            // BIOS internals call most handlers directly. Keep those calls
            // intercepted, but leave BOOT/WBOOT implementation addresses native.
            if (name != "BOOT" && name != "WBOOT" &&
                implementationAddr != 0 && implementationAddr != jumpTableAddr
            ) {
                biosWatchers[implementationAddr] = watcher
                lib.addMemoryReadWatcher(implementationAddr, watcher)
            }
        }
    }

    private fun applyBIOSPatches() {
        val biosAddr = Assembler.instance.labels["BIOS"]
        if (biosAddr == 0) return

        val lib = NativeLib.getObject()
        val ccpAddrRaw = Assembler.instance.labels["CCP"]
        val ccpAddr = if (ccpAddrRaw == 0) 0xE400 else ccpAddrRaw

        biosFunctions.forEachIndexed { index, pair ->
            val name = pair.first
            val internalName = pair.second

            val jumpTableAddr = biosAddr + index * 3
            val implementationAddr = Assembler.instance.labels[internalName]

            // Patch jump table: JP ccpAddr for boots, RET for others
            if (name == "BOOT" || name == "WBOOT") {
                lib.patchMemory(jumpTableAddr, 0xC3) // JP
                lib.patchMemory(jumpTableAddr + 1, ccpAddr and 0xFF)
                lib.patchMemory(jumpTableAddr + 2, (ccpAddr shr 8) and 0xFF)
            } else {
                lib.patchMemory(jumpTableAddr, 0xC9) // RET
            }

            if (name != "BOOT" && name != "WBOOT" &&
                implementationAddr != 0 && implementationAddr != jumpTableAddr
            ) {
                lib.patchMemory(implementationAddr, 0xC9) // RET for HLE watcher
            }

        }
    }

    private fun removeBIOSWatchers() {
        val lib = NativeLib.getObject()
        biosWatchers.forEach { (addr, watcher) ->
            lib.removeMemoryReadWatcher(addr, watcher)
        }
        biosWatchers.clear()
    }

    private var bdosWatcher: NativeLib.MemoryReadWatcher? = null
    private var warmBootWatcher: NativeLib.MemoryReadWatcher? = null
    private var bdosAddr: Int = 0

    private fun watchWarmBoot() {
        val watcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                // If we hit address 0, it's a jump to WBOOT. 
                // We let it execute the JP WBOOT instruction, which will hit our BIOS watcher.
                return false
            }
        }
        warmBootWatcher = watcher
        NativeLib.getObject().addMemoryReadWatcher(0, watcher)
    }

    private fun watchTransientProgramStart() {
        if (transientProgramWatcher != null) return

        val tpaAddress = Assembler.instance.labels["TPA"].takeIf { it != 0 } ?: 0x0100
        val watcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                isTransientProgramRunning = true
                return false
            }
        }
        transientProgramWatcher = watcher
        NativeLib.getObject().addMemoryReadWatcher(tpaAddress, watcher)
    }

    private fun watchBDOS() {
        bdosAddr = Assembler.instance.labels["FBASE1"]
        if (bdosAddr == 0) return

        val bdosFunctions = mapOf(
            0 to "WBOOT",
            1 to "GETCON",
            2 to "OUTCON",
            3 to "GETRDR",
            4 to "PUNCH",
            5 to "LIST",
            6 to "DIRCIO",
            7 to "GETIOB",
            8 to "SETIOB",
            9 to "PRTSTR",
            10 to "RDBUFF",
            11 to "GETCSTS",
            12 to "GETVER",
            13 to "RSTDSK",
            14 to "SETDSK",
            15 to "OPENFIL",
            16 to "CLOSEFIL",
            17 to "GETFST",
            18 to "GETNXT",
            19 to "DELFILE",
            20 to "READSEQ",
            21 to "WRTSEQ",
            22 to "FCREATE",
            23 to "RENFILE",
            24 to "GETLOG",
            25 to "GETCRNT",
            26 to "PUTDMA",
            27 to "GETALOC",
            28 to "WRTPRTD",
            29 to "GETROV",
            30 to "SETATTR",
            31 to "GETPARM",
            32 to "GETUSER",
            33 to "RDRANDOM",
            34 to "WTRANDOM",
            35 to "FILESIZE",
            36 to "SETRAN",
            37 to "LOGOFF",
            38 to "RTN",
            39 to "RTN",
            40 to "WTSPECL"
        )

        val watcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                val cpu = cpu()
                val funcCode = cpu.bc and 0xFF

                // Force user number 0 to avoid WordStar issues
                val usernoAddrLocal = Assembler.instance.labels["USERNO"]
                if (usernoAddrLocal != 0) {
                    NativeLib.getObject().patchMemory(usernoAddrLocal, 0)
                }

                // Only log valid or likely intended BDOS calls
                if (funcCode <= 40) {
                    val funcName = bdosFunctions[funcCode] ?: "UNKNOWN"
                    val deVal = cpu.de and 0xFFFF
                    val caller = NativeLib.getObject().peekw(cpu.sp)

                    var logMsg = "BDOS Call: $funcName ($funcCode), Caller=${caller.toHex(4)}, " +
                        "AF=${(cpu.af and 0xFFFF).toHex(4)}, HL=${(cpu.hl and 0xFFFF).toHex(4)}, " +
                        "DE=${deVal.toHex(4)}"

                    // Extract info for relevant file operations
                    when (funcCode) {
                        15, 16, 17, 18, 19, 22, 23, 35 -> {
                            val filename = getFilenameFromFCB(deVal)
                            logMsg += " [$filename]"
                        }

                        6 -> { // DIRCIO
                            val eVal = cpu.de and 0xFF
                            logMsg += when (eVal) {
                                0xFF -> " IN (Check Status)"
                                0xFE -> " IN (Get Char)"
                                else -> " OUT: 0x${
                                    eVal.toString(16).uppercase()
                                } ('${if (eVal >= 32) eVal.toChar() else '.'}')"
                            }
                        }

                        9 -> { // PRTSTR
                            val str = getStringFromMemory(deVal)
                            logMsg += " \"$str\""
                        }
                    }
                    if(funcCode != 6)
                        ZXLog.terminal(logMsg)
                }
                return false
            }
        }

        bdosWatcher = watcher
        NativeLib.getObject().addMemoryReadWatcher(bdosAddr, watcher)
    }

    private fun performBoot(isWarm: Boolean) {
        val lib = NativeLib.getObject()
        val cpu = cpu()
        isTransientProgramRunning = false
        val biosAddress = Assembler.instance.labels["BIOS"]
        val ccpAddressRaw = when {
            "CCP" in Assembler.instance.labels -> Assembler.instance.labels["CCP"]
            "ccp" in Assembler.instance.labels -> Assembler.instance.labels["ccp"]
            else -> 0
        }
        val ccpAddress = if (ccpAddressRaw == 0) 0xE400 else ccpAddressRaw

        val currentDrive = DiskController.instance.currentDriveIndex

        ZXLog.terminal(
            "BIOS performBoot(isWarm=$isWarm), PC=${cpu.pc.toHex(4)}, " +
                "Drive=$currentDrive"
        )

        if (isWarm) {
            ZXLog.terminal("Warm Boot (WBOOT) initiated...")
            // Reload only the system part (CCP/BDOS/BIOS) to preserve TPA
            val systemMemory = Assembler.instance.memory.copyOfRange(ccpAddress, 0x10000)
            lib.copyToMemory(ccpAddress, systemMemory)
            
            TerminalManager.activeTerminal.clearInputBuffer()
            applyBIOSPatches()
        } else {
            // Cold Boot: Load everything
            lib.copyToMemory(0, Assembler.instance.memory)
            DiskController.instance.currentDriveIndex = 0
            applyBIOSPatches()
        }

        patchPageZero()

        // Sync drive to address 4 for reloaded system state
        lib.patchMemory(0x0004, DiskController.instance.currentDriveIndex and 0x0F)

        // Ensure User 0
        val userNoAddress = Assembler.instance.labels["USERNO"]
        if (userNoAddress != 0) {
            lib.patchMemory(userNoAddress, 0)
        }

        // Clear CCP command buffer length
        lib.patchMemory(ccpAddress + 7, 0)

        // WBOOT's JP opcode has already been fetched when this handler runs.
        // Point PC at CCP+1 so that the in-flight JP reads CCP's target operands.
        val entryPoint = if (isWarm) ccpAddress + 1 else biosAddress
        val regs = cpu.toZ80Registers().copy(
            pc = entryPoint.toShort(),
            sp = 0xFFFF.toShort(),
            af = 0,
            bc = (if (isWarm) 0 else (DiskController.instance.currentDriveIndex and 0x0F)).toShort(),
            de = 0,
            hl = 0,
        )
        lib.setZ80Registers(regs)
    }

    private fun patchPageZero() {
        val lib = NativeLib.getObject()
        val biosAddr = Assembler.instance.labels["BIOS"]

        // Setup Page Zero (JP WBOOT and JP BDOS)
        lib.patchMemory(0, 0xC3) // JP
        val wbootLabel = Assembler.instance.labels["WBOOT"]
            .takeIf { it != 0 }
            ?: Assembler.instance.labels["wboot"]
        val wbootAddr = wbootLabel.takeIf { it != 0 } ?: (biosAddr + 3)
        ZXLog.terminal("Page zero WBOOT target=${wbootAddr.toHex(4)}")
        lib.patchMemory(1, wbootAddr and 0xFF)
        lib.patchMemory(2, (wbootAddr shr 8) and 0xFF)

        lib.patchMemory(5, 0xC3) // JP
        val bdosAddr = Assembler.instance.labels["BDOS"]
        lib.patchMemory(6, bdosAddr and 0xFF)
        lib.patchMemory(7, (bdosAddr shr 8) and 0xFF)
        ZXLog.terminal("Page zero WBOOT target=${wbootAddr.toHex(4)}")
    }

    private fun getStringFromMemory(address: Int): String {
        val lib = NativeLib.getObject()
        val sb = StringBuilder()
        var addr = address
        while (true) {
            val char = lib.peekMemory(addr++) and 0x7F
            if ((char == '$'.code) || (char == 0)) break
            sb.append(char.toChar())
            if (sb.length > 255) break // Safety
        }
        return sb.toString()
    }

    private fun getFilenameFromFCB(address: Int): String {
        val lib = NativeLib.getObject()
        val driveByte = lib.peekMemory(address)
        val driveChar =
            if (driveByte == 0) ('A' + DiskController.instance.currentDriveIndex) else ('A' + (driveByte - 1))

        val name = StringBuilder()
        for (i in 1..8) {
            val charCode = lib.peekMemory(address + i) and 0x7F
            val c = charCode.toChar()
            if (c in ' '..'~') name.append(c) else name.append('?')
        }
        val ext = StringBuilder()
        for (i in 9..11) {
            val charCode = lib.peekMemory(address + i) and 0x7F
            val c = charCode.toChar()
            if (c in ' '..'~') ext.append(c) else ext.append('?')
        }
        val n = name.toString().trim()
        val e = ext.toString().trim()
        return if (e.isEmpty()) "$driveChar:$n" else "$driveChar:$n.$e"
    }

    private fun removeBDOSWatcher() {
        val watcher = bdosWatcher ?: return
        NativeLib.getObject().removeMemoryReadWatcher(bdosAddr, watcher)
        bdosWatcher = null
    }

    /**
     * Sets the result of a BIOS call in registers.
     * Many CP/M programs expect 8-bit results in BOTH A and L registers
     * for maximum compatibility with both 8080 and Z80 calling conventions.
     */
    private fun setResult(a: Int? = null, hl: Int? = null) {
        val lib = NativeLib.getObject()
        a?.let {
            lib.setRegisterA(it)
            lib.setRegisterL(it) // Double-return in A and L
        }
        hl?.let {
            lib.setRegisterHL(it)
        }
    }

    private fun CPU.toZ80Registers() = Z80Registers(
        af = af.toShort(),
        bc = bc.toShort(),
        de = de.toShort(),
        hl = hl.toShort(),
        af2 = af_.toShort(),
        bc2 = bc_.toShort(),
        de2 = de_.toShort(),
        hl2 = hl_.toShort(),
        ix = ix.toShort(),
        iy = iy.toShort(),
        sp = sp.toShort(),
        pc = pc.toShort(),
        i = i.toByte(),
        r = (rReg).toByte(),
        iff1 = iff1.toByte(),
        iff2 = iff2.toByte(),
        im = im.toByte(),
        tStates = cycle.toInt(),
        holdIntReqCycles = 0,
        flags = if (halted != 0) 1 else 0,
        memPtr = memptr.toShort(),
        totalCycles = totalCycles,
    )

    suspend fun loadDisk(driveIndex: Int, resourcePath: String) {
        DiskController.instance.loadFromResource(driveIndex, resourcePath)
    }
}
