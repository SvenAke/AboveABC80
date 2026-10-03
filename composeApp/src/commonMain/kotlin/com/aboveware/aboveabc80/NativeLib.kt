/**
 * NativeLib: Interface to the native C++ emulator core.
 *
 * Recent changes:
 * - Removed legacy bitmap rendering functions (renderToBitmapNative, etc.) 
 *   as terminal rendering is now handled by TerminalView.
 * - Cleaned up platform-specific implementations and unused video memory flags.
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
                if (labelCount != 0) ZXLog.trace("... $lastLabel($labelCount)")
                lastLabel = label
                labelCount = 0
                val cpu = cpu()
                var message = "Trace: $label ${cpu.cycle} ${address.toHex()} $cpu"
                val retAddr = (peek(cpu.sp + 1) shl 8) or peek(cpu.sp)
                val retAddr2 = (peek(cpu.sp + 3) shl 8) or peek(cpu.sp + 2)
                message += " RET -> ${retAddr.toHex()}, ${retAddr2.toHex()}"
                ZXLog.trace(message)
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
    fun portRead(port: Int): Int = try { bus.read(port) } catch (e: Throwable) { 0xFF }
    fun portWrite(port: Int, data: Int) { try { bus.write(port, data) } catch (_: Throwable) {} }

    fun sendKey(code: Int) = sendKeyNative(code)
    private external fun sendKeyNative(code: Int)

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
        ZXLog.terminal("Extracted ${files.size} files from LBR archive")
        return files
    }

    private external fun extractArkArchiveNative(arkData: ByteArray): ByteArray
    private external fun decompressSqueezedNative(data: ByteArray): ByteArray?
}

expect fun loadNativeLibrary()
// cat TZX01.
// Welcome to The Z-Letter!  This is the first issue of a monthly newsletter forthe 8-bit or CP/M-compatible community.  Everything in this issue is copyright(c) 1988 Alpha Systems Corporation, 711 Chatsworth Place, San Jose, CalifThe purpose of this magazine is to spread the news about new developments inthe community, and to help newcomers get the most out of their machines.  Tothat end, every issue will contain a news column, one or more articles, and aletter column.  The news column will spread news not only of new products fromus (that is, Alpha Systems Corporation), but all news of new programs andmachines that comes to our attention.  The articles, we hope, will be writtenby many people.  So send us the news about your new software or hardware, youropinion of someone else's product, Alpha Systems Corporation, since 1979, has been dedicated to development ofmicroprocessor-based eight-bit computerSince 1984, we have concentrated entirely on the software side of eight-bitsystem development.  Alpha Systems Corporation developed the first Z-Systemoffered by a computer manufacturer, the Ampro Little Board.  In 1985, theMicroMint SB180 system software was developed here.  The ON! System software islargely a product of Alpha Systems CorpoThe Z-System has its roots in CP/M 2.2 and 3.0.  CP/M was the first reallyuseful operating system for eight-bit micros.  Until the advent of Intel'ssixteen-bit 8086 and its IBM following, all serious small computers ran undereigWe do not take particular issue with the argument that ATs offer more memory,more speed, more of whatever.  IBM anWe are of the mind that more is not necessarily better.  A sailor may love thesea, but a drowning man would certainly appreciate less water around him.  Asixteen-cylinder Ferrari with a flat tire is not as attractive as a Chevroletwith eight cylinders and no flats, if you need to go somewhere.  Given themyriad things small computers can do and our own particular needs andpreferences, 'bigger', 'newer' or even 'faster' don't mean much.  'Better' iswhat The Z-System is a full, rich and growing operating system environment foreight-bit systems based on the Zilog Z80 (or compatible) microprocessor.  Proudof our heritage, the Z-System supports virtually all CP/M programs. FurthermDue to the demonstrated popularity of the Z-System, industry leader MicroProreleased WordStar 4.0 last year, with Z-System support.  They reportedly soldover ten thousand copies of it in two months.  To paraphrase Mark Twain,"ReJoseph W. Wright (Joe to his friends) is the founder and president of AlphaSystems Corporation, as well as the publisher of The Z-Letter.  Joe is an open,friendly man who puts together new computers the way most people screw in lightbulbs, and designs system software with less trouble than most people havebalancing their checkbooks.  As far as his accomplishments go, read thedescription of Alpha Systems Corporation above.  Alpha is Joe's company, anditDavid A.J. McGlone earns his living at Tandem Computers, Incorporated, inCupertino, where he is a Software Technician, a title invented to describe whathe does for the software development group for which he works.  David comesfrom a much later generation of computer users than Joe; his first homecomputer was an Eagle IIII purchased in 1981.  Besides being the editor of TheZ-Letter, David programs in BASIC, COBOL, and Pascal.  He has been the editorof The deadline for submission of material is one full week before the end of themonth.  We cannot pay for articles, but for every article we publish, theauthor will receive that issue of The Z-Letter free.  If the author has asubsMaterial may be submitted printed or typewritten on clean white unlined paper. However, this is not the way I prefer to receive them, because then I have totype them into my computer.  Submissions will be favored that are received ondiskette in any 48-tpi or 96-tpi format known to Uniform, such as Kaypro,Ampro, Micromint, Epson QX-10, Eagle II, or Eagle IIII.  Please label thediskette carefully with your name, address, phone number, and the format it'sDiskette files can be in WordStar format.  However, standard ASCII files, withno control codes, and carriage returns only at the ends of paragraphs ormarking blank lines, are preferred.  Most word processors can save files inthiMail letters, articles, and news for The Z-Letter to:  David A.J. McGlone, 720South Second Street, San Jose, CalifSubscriptions will be accepted for 12 or 24 months.  A subscription starts withthe first issue after the subscription payment is received.  The cost is $24for 1 year, or $48 for two, for subscriptions mailed to U.S., FPO, or APOaddresses.  Subscriptions mailed to addresses outside the U.S., includingCanada and Mexico, cost $32 for 1 year, $64 for two.  Back issues cost 1/12 ofan annual subscription; $2 in the U.S., $2.67 foreign.  There will be an indeSubscriptions should be paid by check or international money order in U.S.dollars, mailed to: Alpha Systems CorporEchelon, Inc., for many years the owner of the trademarks and rights to theparts of the Z-System that were not public domain, has folded.  With it goes|The Z-News|, the biweekly magazine that Echelon published (Z-News 809, 14MarEchelon has signed its rights and trademarks over to Alpha Systems Corporation,which will continue Echelon's work of serving as a central resource for theeight-bit community.  In addition to developing software, which it has beendoing all along, Alpha has started its own newsletter, |The Z-Letter|.  Ordersand subscriptions sent to Echelon are now being forwarded to Alpha, which willfill them as quickly as possible.  Z-Node Central is being moved to AlpAlpha Systems Corporation has been filling orders for Z-Com since June with NewZ-Com version 2.0 (NZ-COM), a virtual revolution in operating system design. Like Z-Com, NZ-COM installs itself on all CP/M 2.2 computers in seconds, givingthe user a full Z-System without any modification or assembly.  New featuresinclude dynamic reconfiguration of the system on the fly, including the abilityto drop back to CP/M for a memory-hogging application program and thenreturning automatically to the Z-System after the program is done.  For $69.95plus $4 shipping and handling plus sales tax (for California residents only),the NZ-COM package includes ZCPR 3.4, ZRDOS 1.9, a very clear and welt of LUS is like NZ-COM, but specifically designed for CP/M Plus (3.0) computers. It installs on all CP/M NKs systems in seconds.  This makes it possible, forthe first time, to migrate from a hlus to the Z-System, a feat that was saidto be imposser wle.  Between NZ-COM and 9.US, the Z-System is now available toalmost anyone with a Z80 (compatible511omputer system.  ribS comes with ZCPR3.4, but NOT ZRDOS 1.9; due to the differences between a CP/M (2.2)  wit is priced the same as NZ-COM, at $69.95 plus $4 shipping and handling,plus sales tax is you live in CaliforniIn connection with writing ourc Joe Wright has updated -tpk T, originallywritten by Dennis Wright (no relation).  This latest public ZRk T featuresimproved character I/O and disk-error handling,84 well as support forNK*Perfect SPerhaps the intot iontrtant change, in the long run, is the elimination of twoof CP/M's critical limits.  Under CP/M 2.2, no file could behe drger than 8megabytes, nor could any disk drive be largomaan 8 megicroPtes.  Wiscrik T1.9, a file can be as , dus i32 megicroP's 6, and a disk|Theve canedits largeas a gigthoe (1024 megthe Es).  While The latest incarnation of the n, ha command processor pelot "just anupgrade" of ZCPR3.  rriR34 is the heart of the new n, ha and was rewrittenspecis oflly for I hand  wit.  AR|h, it representMatny advances andsome new approaches in computer operation.  With newe 3..  Nwe get "Type 4"utilities.  These are difthemnt from epope 1 un moities, which always load at0100 hex, a ae 2 uti, weies, which had the environment descriptor built intothem, or TIse 3 athitifavhiche ofbe assembled to lE! at s Ea address otherthan ly t00 (these werd rtroduced with log 3.3).  TMI 4 uhavsome loady- CP/M.fixed address.  Instead, they examine the system when invoked envpartdthemselgu just below the cMr.  of trbeen Gupta, Technical Marketing Manager of the Z280 group at Zilog,recently ansts wd qu=R288ions we had.  Yield on the Z280 is still pretty low;this is reflected in the unit price of the current version s, ihip. Revicompan H of the Zilog 280 16-bit cesso-compatible MPU se Lfor around $20 to$25 apieceL, long as inter=R288 in the chip remains high in Europe and Japan,development will continue (interest -Syse U.S. is low, due to the stiflingef-Com StaBig Grey on tecATs wr innovation).  Reroupion J94ue any time now,will correcMcfunbugs in earlier releases, a.  U. ne ipta (pronounced "Hoopta", by the way) also explained what a Z180 isd thiThis chip is a joint projecStaHitachi somilog, which fi gr-bit s timingproblems of the HD64180, and some other modifications.  The revised H41495ssold by adem0i as the Hs M180Z; Zilprocmarkets itus ithe Z180. to yese chips, an andproved very cof the CPU in Mic ion of the Print Spooler (B/cornter# tevrS) is in the workded hepghtremains $39.95 afullyce, or $89s wilfor all tThpyw IrioR is made actessary by NZ-COM.  to wCORD.COM, which turns on and offthe recording of characters sent to the CRT, prriter, or both, sn mol ds,under I f  But you be ft create an IOP first, using RIPTORDER.ple and loadthaRDe ofdo its and tb.  It is RE! SDER that does not work(der I fs abd eNuKER wrongly assumes, when making theo myly o that the deveironment We cptor fa Lon a page boundary.  Under ourc 'tain'tactUBMrily so.  The new ZC $y-tpL is  ion 2.03 of BA (Build Alias),a program that lets you use your editor, or uesediSTE  write a APses. Unand 8e  TOAS and ial LI-ba, whi to dave their own hinnds, or SrowS, which usesthe WordStar see prwith BALI-ba you use your customary word procais.  You makean ASCII text file of the k ld script; the up, $ed or ofcombininte with ainlskeleton a APs to make the alias you $2.n.  Ambiguous script names are accepted,allowing you to ers a several for eses cle..eetting up terminal function keysputer lecting pri 'is rtions, andt=R288ing the hardware.  Controlot t support fSORTMKsee8086 orders small -bat nI textot ts onus imanyus inine s, and ckeys in, thicending or descending seter thence.  The files can be aersrge as 20K94ep in Ungon the TPA.  Snew pMK is fast; enti0 lines of tr Mi are sores, a NSURV.  IB keeps a data base of whatfor CnamRSodhat diskette, as do othercLETlogfor onrograms.  o puU ABO.  IB al ps theCRSanand size of the RIG.  FileMaty be searched for by disk, name, go ,prececSTEy, or pose.  'b memfrtracks down RIG that have not been updated, orbroke byEntries may be deleted by   or disk identis oftion.UBLonoluot tsuFrwordly appere b.  F.  A CPRc) Z-System, CP/M's Z28man w    ATThartaxOSanexplains that "control file dat" means that OM SCRdesignatge,perha Seither from f msual parameter line ca Lor machm a harLfi he hat contains the t peeters.  This f!  Till tlows automating opere b(iter theturnshe r $ctory, and to document what parameters were used for aiTo ordomaks  products  inquire about the policy for multiple purchases, orto in-5e about updates, es|genSopennkt-eek ramting Ceissue es, 198 inKohlerDrive, Boulder, Colorado 803HER|-5240.  Shippi dad handling for a singlepurc$3 Alphat'tems Corporation has =R288ablishei fZ-PLAN for user's groupsl cosplan benefits both Alpha Systems Corp. and r ger les, pet participha Sinie prby encouragfor oneop he o and t-Syse userptionroups, encourmyng people to29ye o       Alpha0 180eeping Alpha le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII He details are still in fluas a c the basis!idea is that Alpha will purchase amembership in every p~=R2pablig les..b for Alpha.  Since the o oefits of the THE  onlphoxtendto atsbernd N the e,y-tendees who are not 7 ers will be andour dged toidentn the gresouter fue;Alpha's e .s will be an abCOM)e through local: Aerds o, people will be more Ss inter=R288ed in this idea shouldad ite to Alpha Systems Corporation, 711Chatsworth Place, San Jose, CaliforniaThe letters that follow were not written for |The Z-Letter|.  I culle-bit sm outof correspondeue;on)to Alphat'Marr o Echelon and forwarded by FrankGaude to: A.  I hopecrew) mt issueFuefee us ts written owevressly fa Syass Z-LettDG dence to:  David A.J. McGlone, 7enti South SecondStreee prSan Jose, rfornia 95112.s r ediSTE and thT Tblisher reserve theright to edit icu received to conform to standardOFtaste94ecency, andlanguage.  We will CP/M.T disSTEt tDo Tor eg of ues ed when ithed, please say  or 2m d letter.  All other, Ias iill beassumed to be for publication aItem one on my list is to convey a big pat on the back to you for your    Nenuinely uplifed.  spirit of userort.orNeABOUI had better tell you what I've got and why I amckaed. , later Ils. of Tllin with whatOrm doing and who I am  9ave a very busy and tb, and E P Z-tifunhas been nearly nil for about a year now. My subscription evenhe dpsed (oba the dusome of System AdministraSTE for a pair of Unix machinees fSiliconGraphics)crewwe use for TV an andation confuences.  Gettherip to spege,n allof this kept me from blikeL|g in n, ha, but also made appreciate it more (IlNow I woeigh like to make ctivfore80 really hum while I wait for who knows what toc Eaner 2pike.  Irgug  get the 9le), -Ntar 4, andfoon (he rest I woeigh l U. your adissue e.  Having been out of touch with thedevels tognts, and wanti Z-Co gt  in.k on track quickly, Iludeld Plarec fol yourinput, s WRIhat I can do this all n anue;and get on arop r owoUT T8to renew ctivnewsletter subscription, and the 8all the issuewithoOrve missedsince ctiv(sigh) owevirnMy applications: writing icu, programming (Pascal and shifed.  to) C),ownfunsemi-homebrew projectsr ostay awaypaymBA She way, this us t was written and prrited from ctivvintage Radio ShackModel-100 laptop (it lineseditter't sixticas old now).s rre se better toolpl a quick us t.  Of xt.rse you  insefee 60*10 screen software(Ultrascreen) doining on it.  So far it's the only lao Ep chece usedcrewdidn'tmake funwish for a desk to Usit on.  IMoead of cannng at ctivdesk, checntaeensitting out on the front porch of ctivbig old issue STEf ie, 7use, smelling then dofusion oWernill feelcrewAlpha is carrying on elons work in such a waycrewwewill ear 2safunpraise.  ct,ur subscription renewal, and nrthers Echelonsent us, will be dealt with as subscriptions to The Z-Letter, since ZATESews isno moreL, for advicreirguafraid anyone you ask will efee difthemntrecommening ftions.  Joe uses -Nfor Z-4.094 of tse II, and their in assemblylanguager oprefer Spellbinder e+3 and SeON RSTE V, and ue wEag of E Pn dogramming right now in TURBO Pascis bend Nevaing f COBOL, not efeing had the ph LCOickto the 8into (Zembler.    9opecrewhelThe d    -- DAJMI understand are sis going th$2. sofunchanges.e auefee read yourATESEWELLndNEWS RIG and only want to say erso carrere09 existbetween you and Frank, may they not slowner 2excied.  ideane eou and othersare promoed. !eyf popan be of any (Ze Tancd r protling E Wts f, letonakn l4ey am andlosing the afty ist issue of the identnt 51,tter vingresouWe are the outfi"Rt pu col the first (anmeoal?) Z F=R288ivalEavd r the in.s, bf ctivmind a roupit by Joe Wright anAne emaycan see, Lee, the chaiten 'e even biggomaan ch yight hav8 auessed. are sis gone ve tothe scene completely, with Alpha care nong r Nere udileft offL, for helpfor onromote our ts f, sicromly spreadinthe me wordments are sm~=Rit computing set dead ev the afticromorted iter 1ng you can do o. his stration   "Thingn 'e happening erso wanm this space!" There is a definit.  Suole planned for user cy gresous; see above, in the newscolumn.e aulook forwar RITn mentionge,n Jay Sage's RAS at $69'swcertainly andour dge M fo consider some kind of 51,tter ve toyource aftny.  Thesoftwas no ter thestion buems C Z-News, -5ky and erratis!as it was,provided a irsOFuniting tpha'windling ei He s Ea of     secrovunicatsibten through PSanPursuit, that meomber is a small minority of yourpot20al cust Ear bahimey join with manyalahers in Seattle, most of whom I knEMbeing "foailaed" onAing fmZ3-iBBS (f mnofficial Seattl all the luck (le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIWat ath enoun pts f-cyke I hand  wit to ers ams Co easieon (o switch to the Z-System,our hh a large  maiun psubscriber base andwith r ger ds o and bulletin boards spreUBSng the word, the "potentialcust Ealifoase" maphoxpand to iThe 4th of July in 198he fl, yr Joe:ishehat you efee down here in tee3Piego is E Wter uic computer ignorabe fsurrounh Sby a doo, cn (give or take) Kaypro-10s turned into K-44s, K-88s, andsuch; two S-100 systems with 50 shorptef ous iyet uninventoried (sendedl71OR er andental ptef o); a PC/AT-286 with 126 MB on e wa fi grd disks and 3074K RAM(540K plus 2 l b aboveeee- a bunch of modems, e wa being USRobotics CourM- HST96dones; a half-doo, cn ithers (one isnomer,  monally) r, aprietary for ure fairly blanketing CP/M 1..  N2.2,3.0, and 86 erso and MSrksC-DOS, naturally erso double-backed-up on meombered,-hoged flopfullyout a and microcomputer hahat eof ond for u|Wd systems owevertsof national r maiwn r lne or two gainfullphomployed, rumor has itewhho visit nowand the 2way people g4to zoos (And more .... bhellks, manuals, and turnal colON Rtions preing fed.  electrictMIwriters, AND PAPnt(!!) efeing s Eathing or , gther to ue wn anne ph  toownme to Ue wistN| believeus ian acSta ..........th but woeighn't be col ha doMS  computers or computerd nrsons or persons who met aou'ter t Con atleas cole 1han theof ore -Ntars with MailMer4.0ce I d aa rethemue;le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII Mibook publisher who knew his29siness welld thiHelprkiuild several good companies and h hem for cashmoneOUT  Tfter.  Z-nc., Scott, Foreman, and even the American BrE!.9,ed.  Comp, incare iwice erso satBd Shen I started laying awake at nights, staL|g at fuzzy dimnesms iThe czingar kent.out a fhellt belEMWReiling in ctivbedrhellm this i the lios.d aoffwonds|ng over and over again, "What the hell thaha inclay Ito theou'terrrrNevolution? HEMcome alaftnone of the more  of o 6done or 7done sci20sts,e plsearchers, academicf is I knEMpretty wellEors of s Eadamay Iicromorted it stuff still CHiling cabinets and ds patient ing ftaS Hclin wr trials to their col ities, hospitals, and reseDITh iMoitutes have inisocomputers inthem s Eawhere erso and modemSCRmewEDelse erso and AL sib theirmodemn 'e thaconactted to a telephone croout one end and a clloter outthe meaner erso a fuissues Ea m <................................................................................................................The idea is s foereed for ure.  Worthwhile their.  Pragmatism.  Quality thatspeaks for itselfvirtuicue tround $2 into $4 inmax.  Sofunfree29t to wALLYn dotected.  Effecti of darketing that doesn, ucost more  of o 15% of net sales,budgeted realistically.  De Tribution e cods,crewlet middlemen make a bor Ed thiRo64 lsome.  A reputation created by ailatopsos and autre T, not by aBASce-n desident fhor ublis!ght-btionswsrovon Sense.  Tstring fys and ThursinDroponaone 8085ph  or we canms.sage ar k on Z-Node Central 8085 days andone or more of The Cagu of CP/M on tpirs's~=, the cro insbe drawn 8085re wie.  Winews ony luck, the actktietMIn 'l4going meotse no Z-Co gt Big Grey's macigh o say that these things shoUT Tbecul,s|o, cdot- whoption or i Z-C he work?  S Eaone has to sickup thesystemgethme to tMI in all the dLET0 180eep typing it in as new ing fve Sis geggiated,co, wigenmistakes, fix burop  in is $ogr ities, and resefrie institu's 6ank  all theycan o the 8the money for the people and 80, uipment to accomplish ble sn d andary mission (as we IT Mpareay s orArE P).  W g4you can, uthe 8doctors Z80X-ray mase nes, you don, uwor, Caabout whether your patient records sit in acul,er or afor Crminee prbelie of de.dSerede,nother reaustrcrewthe fils, bf them ase plplacing paper, mues  1arkshells, b 2computer moof os a tool for makmune .ion of the chi'er easM-...nmple of this. Hande you the 8computer-e ored reports filling file rm, a vs, all secof o aretransferred by dsing $2.th, a fullyce of paper cane, 7ld  or 2much more information  of o astandard CRT screenin lt I adert knEMalWR.,es, for usoftwas getting hard to lolars40 maycanimagine ctivand ty this i Ie fird s oruncne '88 issusib _ProRIG_crewyou efee atter.ON Rtion of sace,ulare.  Pl formo gntssoenputer o hie E W-hog'swefe thedifrieed throun pmany of WRurrent user ly saBOLes without doiningacross your name env leastus ied oource of CP/M their.  WEDue wyouadvertise?  I've looked in _ramter Sho inclr_, _Inputterand MAsYSear. ZCe'nyother  7The Z-Lel riterest user publiralns popoMaybe re son, uadvertisntaecause helon, ufeel therede, mal wlessr CP/MAmpell, think againr oORATchased my Kandio1   $10000MC,k T machine this i so many per-Com tly adequha Sfunctional and ineOR ensive CP/M(its are at.IBe either:ly ma closeouts.  Let: A knowLCOou'-bit 8 necand we s orsecondar(Wl wt can still be counted on to buy Che.  After all, look clethe mone(Wde by manfinan Jrers whoort.ly partspl old, outing fted,ts onUDars.  uncst because it ain, The proatibMS  comparisons between cars and computers, Ciags, is that 0,efe.  Sueally changed very-cy!|d rPTic performaue;uesph  recently.s rrrrNange of perne nce and featurue tmong aft0, and t$2.cks is ce aftrable tothe raite in PCes fthat is, BigtchePOMatprefeue tnd other0 (Ntseunders'ce afttible clones); ily boeserret aratingoach the ran ......................he TRSmal r, etcr oagreecrewnot staddrof-tdresunedoes not voldead.  On thepesher hand, the n, ha IS state of the art.  Beli, COme, the-bit 8e f!  Tess orn, ha only RITn ed hainframeou'ters.  Sofunof them, such as thedynamic atsory mapping occorthherinder I hhhh.3SUBS, are t. raroo seen71ven on the new=R288 mainframes.ishee aren, uadvertising anyment o yet. to yis will c.6e w g4we efe.noun pmoneyle IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIter to invg orin it.  Anywayin lt's up to JW ll ds out -hogs aSCRon as thePOre ready.  Right now, we have thsot.  Moment gh the stuff in elons cLETlog and RITuKe Liourc Z3-Dot-Com, ZIALlus, Z-Com, ZCPR,-tpk T, Z-Letter, Z-Node, Z-System, AlphaDSutre T; d of tse II, Ashton-Tate;Unias a c AT&T Bell Labout a TURBO Pascalbmorland Inpatl; MagicFont, CES; CP/M, CP/MNKs, DRI; Z-News, Echel..nmpmif the kind of article we hopeand Bickfor 1 n 198us t, and sinht-brgu to Alified to write on word processingNote that this _doeserret_ mean that no one else canckate abod Shis field. Articles on any e'of wubWS|ts ny,8 aratEors(ormatters(ontmanagers, and officNanagement  Thi29 11 or 2uest Cr No uses theownftware.  All that the term teld."you is tho. he  Che involgu puted. words on py rr, le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIat it isof puAsprimarily_ a ing fve Ster uOLnager, spreadsheet, or programming ce. Toration e au  J pr andarvelo, because 8085 word processorscan ue wa fair  ret of making ing fta- ell you owevlicitlyalI feel onsib them has outlivMinbe ssefulness.  If you notntexonausing onei9 rway) ioesin twfit ctivted onition, please on'funkn l4  Of     se, youaof olso free to disn IeeMS  my ted o  Dons.  No one aFrarit..nmples of  washeoretical schemeof cl(Zif 130. you are freeand Bresou and separate WRategoriage a-bit smemberOFeaEor| is the sicromlest kit gof   Bacommurocurtng ots  By all Cfi or.  Z-centan news athat allowne emayto Us  Bas iEor has a crovle IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII send tr Miturnshe ts tr, but-cy!|e or no abilityto ers a the ithout look-cyke anything.  If erned it a paragraph indented, youput spaces before the teABOUs orfile; if ymay805t a-cyne centered, you xt.ntthe characElig sh.c you the fimine cro is o specifer.  number of croat aph ; you can, umove up and dow 2screen, and can onlyEor supplies, upr Nich itFuue w8085is theg t WRIhe linene eouefee speM per. , show them roou, and issue antpir pro scravedown, move the curs is o'Bor cith nCsorag[eysLic.  Ily sine-Nfor Z-without uesdot rass a 11of And with no ab teOUT  format outputto is $riter.  That woUT Tbe a scre12 o     sigishehat ue wI volby |format obel ut|?  FoTINt is antpir much-IT Mou'ter   BaAmpe speak of the formaStaa diskette(ost oed. yliiskette, and now formated. obel ut.  W g4I   Jcrewa anu, alter, stformats obel ut.  Z- mean that a lareff the W.uff ymayIT M to efee to do on aITIpeA |formatter|, then, is Duhat understands commands that and can placeint EDot t.  These  in 1 control the placement of tr Mi on copinted page,WELdgmay. Mcank  to Usin a loStacar67ge returns, crofeeds, andspa.2to e Wf Ar Mi placeEing capabilities of these on p.......................aissare ihere's that on pagain!) va, Cawideews or 2ue wble s ford thaing abilities.  AllIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIAll Cabir and a f, alter ue wnot efee to be combins Zi a single fullyce ofots  Ipats per-Com tly possible, and in Eing offeab teies cleallreetmaytype "corntmyindex o8"(ofor mMoance, to send a file nao n MYINDEX to E Wld.  PRINTaWet 8 spacever,ore maileline MI andulaed.  a left margin of letlor GAEven alE Won p.......................ais comb s rre xt.ld beany meomber of reaustrs to ue wsofornithe aft8ly is tho. he f, alter hasownfunab te, 'o. he f, alter built iyu your on p.......................ais does not.  If',ur on p.......................ais is nd not handle proportional spacing well, or hao Uo2 fto switch fonts on a laser ldy ycriptiill probably switchgethoneror re pr,  usSce.ted, Bowi or Medium or Light THurM-ITIpetsee 408s Helvetian typetir408s Old English  for sace versus zillions of tpir, Portrait or Landscy rorientation o 2itoe, a particular point sio, c (height),  ...........................rticve. pbigh(width), fi grd or p79rtional slorne t a partic SUBsymbol see pretc.  Fthem71xore sle, thpelfigetter is ts td using a font rtmh is in he pndtfpeos hintation (sideways on the itoe), Roman letyOEl am (never minor chat thatl Zns'fixed sp trang, 10 characterscommuer-inch pinm, 12 point height, upright new ot italic), o nium weiA If nt FOger #s what popall Duhat allall a4 print tr Mi in avas hity of SCIts.  TIs wrly this irsne emayefee ahe dserS .................................... ', and the fon.  Aanilit allowne emayto  andbeUDrovandwitho sst hh betwNucts ve toSoftCraft let you ithr, tall kindOFSCIts onEpson FX-80s and compatible arte (udi al or 2efen-he pserJiche pog'SCIts may be |residenith i, in which .9,e they are built ihmcopi 'd thiNon-8 Aent SCIts tten  be on cartride. The tpir kit gof non-here nt laser SCIt is the |soft font, strtmh here s inthe user as a file, and is coper. tuual5, iths the file, it must eitheredit ford thaer, or it  insworkS H a filethat has been formatted on diskgethcreElig is urces, artllall a4 eIf n  a font file andccles the shape or the detailOFcharacters ubmonth Since SCIPslesare not  mony artext, this re-5es special software.  A ghelld debugger 8ZPATCHe ofbe IT M to ers a spot chaitetion,..b called TORicFontwX-retules  wasr Mifor Cint ED Hewlett-Pts Ill,soft font.  Fancy Font ds,with bit maps, 24ofthEor rtmh will Used oinglecharact,"rom one of their SCIts                        an e, phfile, and issue n, bersa. .avit ith Icall a on p.......................ais |offw)riptioement, stnews aalit combines , Alnly71abir and formatAlpha29Mcfunof the funcphsof a dLETPTe, spread ss G,r, agramminProuanr moge.  WordStar set officOLnagement  Thi29t if',ucloine its Bes e   ne t for-wail and macro functions with tpirtheir such as MailMerge, DataStar, etc., you begin thsickthere. Spellbinder Word Procais, from L-Tek.  Z-nc., doue t mace,better  retus i thancemanageCP/M fpe  It has a core fetn adogramminProu--uage built in, and71xceeds the offeab teies of g)itsib software just mentioneavWelal mo of oks for bearing with mede Cr Mi issue lon l disailas the TORicCeiesS HC., f Edia ae Systems.    erso3P(Tons smarks contimeoed from itoe 24) FX-80, Qh.10, Epson; he pzeet, HP; Hs M180,Hs M180ZHELitachi; AT.  Z-BM; Spellthler, L-Tek Inc.; SELEharOR, Micro Ap; fore80,SB180FX7-icromint; -Ntar7-ailMeindu, DataStar, wneo Pro; ON!, 4.0Tsurve LiSopennkCreek ramed.  Serissue es; yearsnc.bone prSoft#tafemo Z80, Z180, Z280, Zilog erso andplease coryou␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦␦svenake@SonicBook:/mnt/c/Users/svena/Downloads$ cat TZL01.\ \ \
//
// Min preview
/*
Welcome to The Z-Letter!  This is the first issue of a monthly newsletter for the 8-bit or CP/M-compatible community.  Everything in this issue is copyright (c) 1988 Alpha Systems Corporation, 711 Chatsworth Place, San Jose, California 95128, phone (408) 297-5594.

The purpose of this magazine is to spread the news about new developments in the community, and to help newcomers get the most out of their machines.  To that end, every issue will contain a news column, one or more articles, and a letter column.  The news column will spread news not only of new products from us (that is, Alpha Systems Corporation), but all news of new programs and machines that comes to our attention.  The articles, we hope, will be written by many people.  So send us the news about your new software or hardware, your opinion of someone else's product, that article you've been meaning to write, your praise, gripes, or just plain questions!  This is the place.

ALPHA SYSTEMS CORPORATION

Alpha Systems Corporation, since 1979, has been dedicated to development of microprocessor-based eight-bit computer systems including Intel's 8080, 8085, 8048 and 8051, Zilog's Z80, Z8 and Hitachi's 64180.

Since 1984, we have concentrated entirely on the software side of eight-bit system development.  Alpha Systems Corporation developed the first Z-System offered by a computer manufacturer, the Ampro Little Board.  In 1985, the MicroMint SB180 system software was developed here.  The ON! System software is largely a product of Alpha Systems Corporation.  Z3-Dot-Com, Z-Com and NuKey were written here.  So much for our credentials.

The Z-System has its roots in CP/M 2.2 and 3.0.  CP/M was the first really useful operating system for eight-bit micros.  Until the advent of Intel's sixteen-bit 8086 and its IBM following, all serious small computers ran under eight-bit CP/M.

We do not take particular issue with the argument that ATs offer more memory, more speed, more of whatever.  IBM and the clone makers are spending hundreds of millions of dollars to present 'more' to the marketplace.  So be it.

We are of the mind that more is not necessarily better.  A sailor may love the sea, but a drowning man would certainly appreciate less water around him.  A sixteen-cylinder Ferrari with a flat tire is not as attractive as a Chevrolet with eight cylinders and no flats, if you need to go somewhere.  Given the myriad things small computers can do and our own particular needs and preferences, 'bigger', 'newer' or even 'faster' don't mean much.  'Better' is what we are after.  More useful, easier, more fun, better for us in our own personal context.

It is in this vein that Alpha Systems Corporation offers the Z-System to you.

The Z-System is a full, rich and growing operating system environment for eight-bit systems based on the Zilog Z80 (or compatible) microprocessor.  Proud of our heritage, the Z-System supports virtually all CP/M programs.  Furthermore, practically all active CP/M public domain authors provide support for Z-System features in their programs.

Due to the demonstrated popularity of the Z-System, industry leader MicroPro released WordStar 4.0 last year, with Z-System support.  They reportedly sold over ten thousand copies of it in two months.  To paraphrase Mark Twain, "Reports of our death are somewhat exag[-gerated."    -- Joseph W. Wright

ABOUT THE PUBLISHER

Joseph W. Wright (Joe to his friends) is the founder and president of Alpha Systems Corporation, as well as the publisher of The Z-Letter.  Joe is an open, friendly man who puts together new computers the way most people screw in light bulbs, and designs system software with less trouble than most people have balancing their checkbooks.  As far as his accomplishments go, read the description of Alpha Systems Corporation above.  Alpha is Joe's company, and its accomplishments are his.

ABOUT THE EDITOR

David A.J. McGlone earns his living at Tandem Computers, Incorporated, in Cupertino, where he is a Software Technician, a title invented to describe what he does for the software development group for which he works.  David comes from a much later generation of computer users than Joe; his first home computer was an Eagle IIII purchased in 1981.  Besides being the editor of The Z-Letter, David programs in BASIC, COBOL, and Pascal.  He has been the editor of the Eagle Computer Users Group newsletter for over a year.

HOW TO SUBMIT MATERIAL

The deadline for submission of material is one full week before the end of the month.  We cannot pay for articles, but for every article we publish, the author will receive that issue of The Z-Letter free.  If the author has a subscription (see below), the subscription will be extended for one issue.

Material may be submitted printed or typewritten on clean white unlined paper.  However, this is not the way I prefer to receive them, because then I have to type them into my computer.  Submissions will be favored that are received on diskette in any 48-tpi or 96-tpi format known to Uniform, such as Kaypro, Ampro, Micromint, Epson QX-10, Eagle II, or Eagle IIII.  Please label the diskette carefully with your name, address, phone number, and the format it's written in.  Sorry, diskettes cannot be returned.

Diskette files can be in WordStar format.  However, standard ASCII files, with no control codes, and carriage returns only at the ends of paragraphs or marking blank lines, are preferred.  Most word processors can save files in this format.  If you have Spellbinder, this is the normal file format.

Mail letters, articles, and news for The Z-Letter to:  David A.J. McGlone, 720 South Second Street, San Jose, California 95112.

SUBSCRIPTION RATES

Subscriptions will be accepted for 12 or 24 months.  A subscription starts with the first issue after the subscription payment is received.  The cost is $24 for 1 year, or $48 for two, for subscriptions mailed to U.S., FPO, or APO addresses.  Subscriptions mailed to addresses outside the U.S., including Canada and Mexico, cost $32 for 1 year, $64 for two.  Back issues cost 1/12 of an annual subscription; $2 in the U.S., $2.67 foreign.  There will be an index to The Z-Letter, updated every issue; its availability and price will be announced after each year of publication.

Subscriptions should be paid by check or international money order in U.S. dollars, mailed to: Alpha Systems Corporation, 711 Chatsworth Place, San Jose, California 95128.

NEWS

FRANK GAUDE FOLDS HIS HAND

Echelon, Inc., for many years the owner of the trademarks and rights to the parts of the Z-System that were not public domain, has folded.  With it goes |The Z-News|, the biweekly magazine that Echelon published (Z-News 809, 14 March 1988, was the last issue).  Frank Gaude, the president and founder of Echelon, has retired.

Echelon has signed its rights and trademarks over to Alpha Systems Corporation, which will continue Echelon's work of serving as a central resource for the eight-bit community.  In addition to developing software, which it has been doing all along, Alpha has started its own newsletter, |The Z-Letter|.  Orders and subscriptions sent to Echelon are now being forwarded to Alpha, which will fill them as quickly as possible.  Z-Node Central is being moved to Alpha's premises, a data base of resources is being organized, and a catalog of available software is being prepared.

JOE WRIGHT FINISHES NZ-COM

Alpha Systems Corporation has been filling orders for Z-Com since June with New Z-Com version 2.0 (NZ-COM), a virtual revolution in operating system design.  Like Z-Com, NZ-COM installs itself on all CP/M 2.2 computers in seconds, giving the user a full Z-System without any modification or assembly.  New features include dynamic reconfiguration of the system on the fly, including the ability to drop back to CP/M for a memory-hogging application program and then returning automatically to the Z-System after the program is done.  For $69.95 plus $4 shipping and handling plus sales tax (for California residents only), the NZ-COM package includes ZCPR 3.4, ZRDOS 1.9, a very clear and well-written manual by Jay Sage and Bridger Mitchell, and a large assortment of utilities.
BRIDGER MITCHELL FINISHES Z3PLUS

t of LUS is like NZ-COM, but specifically designed for CP/M Plus (3.0) computers.  It installs on all CP/M NKs systems in seconds.  This makes it possible, for the first time, to migrate from a hlus to the Z-System, a feat that was said to be imposser wle.  Between NZ-COM and 9.US, the Z-System is now available to almost anyone with a Z80 (compatible511omputer system.  ribS comes with ZCPR 3.4, but NOT ZRDOS 1.9; due to the differences between a CP/M (2.2) BDOS and a .  Therus hes OS, Z3SUBS must retain the rars 

AMoead of replacing it with Zuth.

 wit is priced the same as NZ-COM, at $69.95 plus $4 shipping and handling, plus sales tax is you live in California.

NEW ZRk T 1.9 READY

In connection with writing ourc Joe Wright has updated -tpk T, originally written by Dennis Wright (no relation).  This latest public ZRk T features improved character I/O and disk-error handling,84 well as support for NK*Perfect Systems' BackGrounder ii.

Perhaps the intot iontrtant change, in the long run, is the elimination of two of CP/M's critical limits.  Under CP/M 2.2, no file could behe drger than 8 megabytes, nor could any disk drive be largomaan 8 megicroPtes.  Wiscrik T 1.9, a file can be as , dus i32 megicroP's 6, and a disk|Theve canedits large as a gigthoe (1024 megthe Es).  While no current BIOS supportppoch , d drives, litt least such a ndsOS is possible.

JAY SAGE FINISHES ZCPR 3.4

The latest incarnation of the n, ha command processor pelot "just an upgrade" of ZCPR3.  rriR34 is the heart of the new n, ha and was rewritten specis oflly for I hand  wit.  AR|h, it representMatny advances and some new approaches in computer operation.  With newe 3..  Nwe get "Type 4" utilities.  These are difthe mnt from epope 1 un moities, which always load at 0100 hex, a ae 2 uti, weies, which had the environment descriptor built into them, or TIse 3 athitifavhiche ofbe assembled to lE! at s Ea address other than ly t00 (these werd rtroduced with log 3.3).  TMI 4 uhavsome loady- CP/M. fixed address.  Instead, they examine the system when invoked envpartd themselgu just below the commr, acessor!ment ic
PROGto wSS IS MADE, S
 AS ZIL in t

Mr.  of trbeen Gupta, Technical Marketing Manager of the Z280 group at Zilog, recently ansts wd qu=R288ions we had.  Yield on the Z280 is still pretty low; this is reflected in the unit price of the current version s, ihip.  Revicompan H of the Zilog 280 16-bit cesso-compatible MPU se  Lfor around $20 to $25 apieceL, long as inter=R288 in the chip remains high in Europe and Japan, development will continue (interest -Syse U.S. is low, due to the stifling ef-Com StaBig Grey on tecATs wr innovation).  Reroupion J94ue any time now, will correcMcfunbugs in earlier releases, and a price reduction is possiata
.  U. ne ipta (pronounced "Hoopta", by the way) also explained what a Z180 isd thiThis chip is a joint projecStaHitachi somilog, which fi gr-bit s timing problems of the HD64180, and some other modifications.  The revised H41495s sold by adem0 i as the Hs M180Z; Zilprocmarkets itus ithe Z180. to yese chips, an  and proved very cof the CPU in Micromint's SB180 and sav180FX, sells for  in or 6 dollars.

y JEW IOPS AVAg aABLE

New 

 ions of the Inputrioutput Recorder (e, aOR[Stm# ) and NUKEY are at.able.  A new 

 ion of the Print Spooler (B/cornter# tevrS) is in the workded he pght remains $39.95 afullyce, or $89s wilfor all three, sameus iin Echelon's catalog.

Version 1.4 of y wever, frfi grs s Ea29rop  found in the ion pier 

 ion.

Thpyw IrioR is made actessary by NZ-COM.  to wCORD.COM, which turns on and off the recording of characters sent to the CRT, prriter, or both, sn mol ds, under I f  But you be ft create an IOP first, using RIPTORDER.ple and load that IOP, before #
RDe ofdo its and tb.  It is RE! SDER that does not work (der I fs abd eNuKER wrongly assumes, when making theo myly o that the  deveironment We cptor fa  Lon a page boundary.  Under ourc 'tain't actUBMrily so.  The new ZC $ y-tpL is a prefabricated IOP readA So be  IIded b.jZ-COM, eliminating the need to run RullRDn tat all.

Brksrinter suffers from the same proatib, only worse; a similar fix is in the ds,.

y JEW BALIA.  'ND OTHER NEAT STUFF

Skunk Creek Computing Services has on)Alpha 

 ion 2.03 of BA (Build Alias), a program that lets you use your editor, or uesediSTE   write a APses.  Unand 8e  TOAS and ial LI-ba, whi to dave their own hinnds, or SrowS, which uses the WordStar see prwith BALI-ba you use your customary word procais.  You make an ASCII text file of the k ld script; the up, $ed or ofcombininte with ainlskeleton a APs to make the alias you $2.n.  Ambiguous script names are accepted, allowing you to ers a several for eses cleonce.  The script file is available for documentnia rs r pricRS$12.95.

Other Sopennk Creek products include (accht (ng to the file PRODUCTS.DOC that cafunwith the new BA):

PERSET# To#  crea's 6 free-standing programs to send any control seter thence to any e, aO port(o

SORTMKsee8086 orders small -bat nI textot ts onus imanyus inine s, and ckeys in, thicending or descending seter thence.  The files can be aers rge as 20K94ep in Ung on the TPA.  Snew pMK is fast; enti0 lines of tr Mi are sores, a n enti seconds (6MHz Z80 bla).  el'n Josl file-CO.  Cfor poratated operations.  For EL, Z-System, CP/M's Z19n o
NSURV.  IB keeps a data base of whatfor CnamRSodhat diskette, as do other cLETlogfor onrograms.  o puU ABO.  IB al or 2-hogs all diyou.ory areas, and 6
ps the CRSanand size of the RIG.  FileMaty be searched for by disk, name, go , prececSTEy, or pose.  'b mem frtracks down RIG that have not been updated, or broke byEntries may be deleted by    or disk identis oftion.UBLonoluot t suFrart39
wordly appere b.  F.  A CPRc) Z-System, CP/M's Z28man w    ATThartaxOSanexplains that "control file dat" means that OM SCR designatge,perha Seither from f msual parameter line ca  Lor machm a harL fi he hat contains the t peeters.  This f!  Till tlows automating opere b (iter theturnshe r $ctory, and to document what parameters were used for ainlPlalication.

To ordomaks  products   inquire about the policy for multiple purchases, or to in-5e about updates, es |genSopennkt-eek ramting Ceissue es, 198 in Kohler Drive, Boulder, Colorado 803HER|-5240.  Shippi dad handling for a single purc$3 is $4wsoncraue wresidents pay tax.

ALPHA ISSUess INVITATION TO USn t, the pOUPS

Alphat'tems Corporation has =R288ablishei fZ-PLAN for user's groupsl cos plan benefits both Alpha Systems Corp. and r ger les, pet participha Sin ie prby encouragfor oneop he o and t-Syse userptionroups, encourmyng people to29y e orts 
       Alpha0 180eeping Alpha le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIe userea,ouan wup to dha Son each other's activisome.

W
 He details are still in fluas a c the basis!idea is that Alpha will purchase a membership in every p~=R2pablig les,, and maileparticipatin(cresou will becofuna disxt.nt dea

Gr
 Ss inter=R288ed in this idea shouldad ite to Alpha Systems Corporation, 711 Chatsworth Place, San Jose, California 95128, or downBOU the file StrLNTS.DOC from a ZATESode and fill od She  his  13ion therein.

Starting neABOUissue, a column in The Z-Lettermat. publish the information about aMatny ds o
W respondus ispace allows.

LETTERS

The letters that follow were not written for |The Z-Letter|.  I culle-bit sm out of correspondeue;on)to Alphat'Marrporation, usually acce aftnirkiy 25
s for ourc and co, wiBLI domce 8
 o Echelon and forwarded by Frank Gaude to: A.  I hopecrew) mt issueFuefee us ts written owevressly fa Syass Z-Letter|.  Send co.

DG dence to:  David A.J. McGlone, 7enti South Second Streee prSan Jose,  rfornia 95112.s r ediSTE and thT Tblisher reserve the right to edit icu received to conform to standardOFtaste94ecency, and language.  We will CP/M.T disSTEt the irsing of uesus t; we'll simply notn dint it first.  If you torot willing to have any us t you send fun|

Do Tor e	 ed when ith ed, please say  or 2m d letter.  All other, Ias iill be assumed to be for publication and become the property of Alpha Systems Corporation uG  receipt.

-----------

Dear Z-Newsonians,

Item one on my list is to convey a big pat on the back to you for your    Nenuinely uplifed.  spirit of userort.ort; ra of ond commendIBe.  I nuute you.

NeABOUI had better tell you what I've got and why I amckaed. , later Ils. of Tll in with whatOrm doing and who I am.ey'm: Aing an SB180 (no FX suffix), rtmh I ORATchaseor cith the fre,tanstemownftware packager oefee release 4.

  9ave a very busy and tb, and E P Z-tifunhas been nearly nil for about a year now.  My subscription evenhe dpsed (more on that re pr).  In that sme, S.,ve added to ms.
o ba the dusome of System AdministraSTE for a pair of Unix machinees fSilicon Graphics)crewwe use for TV an and ation confuences.  Gettherip to spege,n all of this kept me from blikeL|g in n, ha, but also made appreciate it more (I love[-ixot- Z is the small-is-beautiful OS).

Now I woeigh like to make ctivfore80 really hum while I wait for who knows what to c Eaner  2pike.  Irgugo   get the 9 7
z speed1


































































































le), -Ntar 4, and foon (he rest I woeigh l U. your adissue e.  Having been out of touch with the devels tognts, and wanti Z-Co gt  in.k on track quickly, Iludeld Plarec fol your input, s WRIhat I can do this all n anue;and get on and neight
rop r owoUT T 8 to renew ctivnewsletter subscription, and the 8all the issuewithoOrve missed since ctiv(sigh) owevirnia r

My applications: writing icu, programming (Pascal and shifed.  to ) C),ownfunsemi-homebrew projectsr ostay awaypaymm spreadsheets and dLET bases.

BA She way, this us t was written and prrited from ctivvintage Radio Shack Model-100 laptop (it lineseditter't sixticas old now).s rre  se better tool pl a quick us t.  Of xt.rse you  insefee 60*10 screen software (Ultrascreen)  doining on it.  So far it's the only lao Ep chece usedcrewdidn't make funwish for a desk to Usit on.  IMoead of cannng at ctivdesk, checntaeen sitting out on the front porch of ctivbig old issue STEf ie, 7use, smelling then dofusion of old rosescrewgrow on the raili dad s oryard.  Surely a bouter thet that rivad."that of the finest Zinfandels.

Thanks,
Stephen G. Beuret
4831 Cedar Avenue
P
 Hadelphia, PA 19143

- mon

Well, Stephen.  Z-rgusure Echelon aratingec foli fpat o  2 in.k Eope
W ernill feelcrewAlpha is carrying on elons work in such a waycrewwe will ear  2safunpraise.  ct,ur subscription renewal, and n rthers Echelon sent us, will be dealt with as subscriptions to The Z-Letter, since ZATESews is no moreL, for advicr eirguafraid anyone you ask will efee difthe mnt recommening ftions.  Joe uses -Nfor Z-4.094 of tse II, and their in assembly languager oprefer Spellbinder e+3 and SeON RSTE V, and ue wEag of E Pn dogramming right now in TURBO Pascis bend Nevaing f COBOL, not efeing had the ph LCOickto the 8into (Zembler.    9opecrewhelThe d    -- DAJMment ic
-boo..r
17 June 1988

Dear Joe .....................
I lishxaresidenStaa les, called CCP/M in el'actticut ( PUecticut CP/M Users' Gr
 S).e auand many of the 7 ers of ctivarcPlarec fol the work you and others are phrng for the 8-bit enthusiast.

I understand are sis going th$2. sofunchanges.e auefee read yourATESEWELLnd NEWS RIG and only want to say erso carry on!  Whatever d



ere09 exist between you and Frank, may they not slowner  2excied.  ideane eou and others are promoed. !eyf popan be of any (Ze Tancd r protling E Wts f, letonakn l4ey am and losing the afty ist issue of the identnt 51,tter vingresou and another CP/M arcpublish.

We are the outfi"Rt pu col the first (anmeoal?) Z F=R288ival Eavd r the  in.s, bf ctivmind a roupit by Joe Wright and Bridger Mitchell and Jay Sage for 1 tter.cond Z-Fthe cs tr!

Sincerely,at it
Lee  y Bradley
24 E. Cedar St.
Newington, har 061111

-, Ea.c-

Ane emaycan see, Lee, the chaiten 'e even biggomaan ch yight hav8 auessed.  are sis gone ve tothe scene completely, with Alpha care nong r Nere udi left offL, for helpfor onromote our ts f, sicromly spreadinthe me wordments are smay b8 aur 29t .

~=Rit computing  set dead ev the afticromorted iter 1ng you can do o. his stration   "Thingn 'e happening erso wanm this space!"  There is a definit.  Suole planned for user cy gresous; see above, in the news column.e aulook forward to meeting you ifcrewsens sZ-FFFFFFFFFFFFFFFFFFal ker off.     erso DA, 8ment iat wboo
Joe Wright, e 11 .....................
Am enclosing $7 in as payment and hanle ig for 1  newATESZCOM/Z3v3rtmh   9av9
 RITn mentionge,n Jay Sage's RAS at $69's wcertainly and our dge M fo consider some kind of 51,tter ve toyour ce aftny.  Thesoftwas no ter thestion buems C Z-News, -5ky and erratis!as it was, provided a irsOFuniting tpha'windling eiveuser' o
 S.  W
 He s Ea of     se crovunicatsibten through PSanPursuit, that meomber is a small minority of your pot20al cust Ear bahimey join with manyalahers in Seattle, most of whom I knEMbeing "foailaed" on Aing fmZ3-iBBS (f mnofficial Seattle ZNode) at (enti6) 882-368.  Noredishing you much luck in E Wendeavor.

Sinceroo yours .....................                         Michael Bglchat
                           26that EE. Aloha
                         ms attle WA 98112

-boo
Thch w, movael, weof I need it!  Ceiousewsthis  ret we've taken s vSCRve th, it scares me a little.  W  3
 5
 all the luck (le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIe help) we cano gt.  On anmean
    Bubjece prhere's the first issusib our 51,tter; how ue wyou  8 it?  Joe and LLre hopeful
Wat ath enoun pts f-cyke I hand  wit to ers ams Co easieon (o switch to the Z-System,our hh a large  maiun psubscriber base and with r ger ds o and bulletin boards spreUBSng the word, the "potential cust Ealifoase" maphoxpand to include
 CP/M users, plus n'n pnew users to  issueorporaho he ight
(c or ing.    erso DAnews nJosepboo
The 4th of July in 198he fl, yr Joe:ishehat you efee down here in tee3Piego is E Wter uic computer ignorabe f surrounh Sby a doo, cn (give or take) Kaypro-10s turned into K-44s, K-88s, and such; two S-100 systems with 50 shorptef ous iyet uninventoried (sendedl71OR er and ental ptef o); a PC/AT-286 with 126 MB on e wa fi grd disks and 3074K RAM (540K plus 2 l b aboveeee- a bunch of modems, e wa being USRobotics CourM- HST 96dones; a half-doo, cn ith ers (one in usepraa collection of Nodublic domain" )
 isnomer,  monally) r, aprietary for ure fairly blanketing CP/M 1..  N2.2, 3.0, and 86 erso and MSrksC-DOS, naturally erso double-backed-up on meombered, -hoged flopfullyout a and microcomputer hahat e of ond for u|Wd systems oweverts of national r maiwn r lne or two gainfullphomployed, rumor has itewhho visit now and the  2way people g4to zoos (habit, une .ive, s Eaph s abe fing).

And more .... bhellks, manuals, and turnal colON Rtions preing fed.  electric tMIwriters, AND PAPn t(!!) efeing s Eathing or , gther to ue wn anne ph  toownme to Ue wistN| believeus ian acSta ..........th but woeighn't be col hav   doMS  computers or computerd nrsons or persons who met aou'ter t Con at leas cole 1han the of ore -Ntars with MailMergpyver used more than three t and es.

4.0ce I d aa rethe mue;le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII Mibook publisher who knew his29siness welld thiHelprkiuild several good companies and helped their owners se3
 hem for cash moneOUT  Tfter.  Z-nc., Scott, Foreman, and even the American BrE!.9,ed.  Comp, incare iwice erso sate ie aftny erso dDistoryid p
Bd Shen I started laying awake at nights, staL|g at fuzzy dimnesms iThe czing ar kent.out a fhellt belEMWReiling in ctivbedrhellm this i the lios.d aoff wonds|ng over and over again, "What the hell  thaha inclay Ito theou'terrrrNevolution? HEMcome alaftnone of the more  of o 6done or 7done sci20sts,e plsearchers, academicf is I knEMpretty well as authors and 
Eors of s Ea damay Iicromorted it stuff still CHiling cabinets and ds  patient ing ftaS H clin wr trials to their colleagues by mail or bicycle messeiter even thment gh their uni

 ities, hospitals, and reseDITh iMoitutes have inisocomputers in them s Eawhere erso and modemSCRmewEDelse erso and how  1
 AL sib their modemn 'e  thaconactted to a telephone croout one end and a clloter out the meaner erso a fuissues Ea most of them say, hu8) when I say modem, huh?"

 <...................................................................................................................y HOW COME!??

That d a4 years ago.

The idea is s foereed for ure.  Worthwhile their.  Pragmatism.  Quality that speaks for itselfvirtuicue tround $2 in to $4 in max.  Sofunfree29t to wALLYn dotected.  Effecti of darketing that doesn, ucost more  of o 15% of net sales, budgeted realistically.  De Tribution e cods,crewlet middlemen make a bor Ed thiRo64 lsome.  A reputation created by ailatopsos and autre T, not by aBASce-n desident fhor ublis!ght-btionswsrovon Sense.  Tstring fys and Thursing fys, anyway.  A hell of a lot of fun maileing fy.  Feeling |

.aviter't phrng |

.d.  corded thiWith a smile.

And if someone really 805ts to wear a necktie[ ....

Naah40 a gotta draw the crosomewhere.

Droponaone 8085ph  or we canms.sage ar k on Z-Node Central 8085 days and one or more of The Cagu of CP/M on tpirs's wd aborn3Pecember 2v31937, and figureOrve fitted0
 'Bd long n'gh, so S.,m retina do this any  G  Honest.ment ic+
Ted Logan PublishSystem tonsultants
4560 Muir  ovenue
(JSYSego, CA 92107iptio, Ea.c-

LLgree, 
~=, the cro insbe drawn 8085re wie.  Winews ony luck, the actktie tMIn 'l4going meot se no Z-Co gt Big Grey's macight
es to work rightd thiMean..c
Be, people-cyke you and funare geted.  thingha i to Uith the Z-System.

In answer to E Wter theryventut modems, re7 er how understaffeavind overworked most places are.  It's all very we3
 o say that these things shoUT Tbe cul,s|o, cdot- whoption or i Z-C  he work?  S Eaone has to sickup the systemgethmeone l io designion.LET base, a whole loStatpi
 to tMI in all the dLET0 180eep typing it in as new ing fve Sis geggiated, co, wigenmistakes, fix burop  in is $ograms, repair m0 inery, and the  is w goe  Hn and o byMost hospita  2 uni

 ities, and resefrie institu's 6ank  all they can  o the 8the money for the people and 80, uipment to accomplish ble sn d and ary mission (as we IT Mpareay s orArE P).  W g4you can, uthe 8doctors Z80X-ray mase nes, you don, uwor, Caabout whether your patient records sit in a cul,er or afor Cr minee prbelie of de.dSerede,nother reaustrcrewthe files are  Suile cabinets rather  of o in cD
erti  W
 He s Ead nople2 cerested in computers tendturnsight
s, bf them ase plplacing paper, mues  1arkshells, b  2computer mo of os a tool for makmune .ion of the chi'er easM-.  The use of a c., f to ayserha Sthis newsletter, rtmh iMatiled on or.r, not on a is nskettem, san 
 $2.th, a fullyce of paper cane, 7ld  or 2much more information  of o a standard CRT screenin lt I adert knEMa lWR., fFu tha_replace_ py rr.    erso3PAnews nJosepboo
, yr Sirsions m
LLm the f$2.strated owner of two CP/M computers, a KaIsro4 and a Kandio1.  Th9
 frustration is due toion,acems C, whivid e c., fs themsgeraue tre71xcellent, re APble macight
es, for usoftwas getting hard to lolars40 maycan imagine ctivand ty this i Ie fird s oruncne '88 issusib _ProRIG_crewyou efee atter.ON Rtion of sace,ulare.  Pl formo gntssoenputer o hie E W-hog's wefe the difrieed throun pmany of WRurrent user ly saBOLes without  doining across your name env leastus ied oource of CP/M their.  WEDue wyou advertise?  I've looked in _ramter Sho inclr_, _Inputterand MAsYSear. ZCe'ny other  7The Z-Lel riterest user publiralns popould find.

Maybe re son, uadvertisntaecause helon, ufeel therede, mal wlessr CP/MAmpell, think againr oORATchased my Kandio1 just last Novembelifoecause it was being dumprki, ' NTEMfacturer cheap.  So ts of us out kb, the t ne0
  $10000 MC,k T machine this i so many per-Com tly adequha Sfunctional and ineOR ensive CP/M (its are at.IBe either: Age,r asephufer
ly ma closeouts.  Let: A knowLCOou'-bit 8 necand we s orsecondar(Wl wt can still be counted on to buy  Che.  After all, look clethe mone(Wde by manfinan Jrers whoort.ly parts pl old, outing fted,ts onUDars.  uncst because it ain, ustate-of-the-art doerret irs it's dead.

ct,itac T$2.ly,at it
C.  Moe Topher A. MeiAU
P.O. Box 66
Colon, NE 68018 iptio, Easix-

The proatibMS  comparisons between cars and computers, Ciags, is that 0, efe.  Sueally changed very-cy!|d rPTic performaue;uesph  recently.s rrrrNange of perne nce and featurue tmong aft0, and t$2.cks is ce aftrable to the raite in PCes fthat is, BigtchePO Matprefeue tnd other0 (Ntseunders' ce afttible clones); ily boeserret aratingoach the ranwo,f the ion piest C,100 ma'nF  WRIoing fhate S-1are rmacight
es, the ssueF he CP/M    .-System macight
es, the Apples II and Macintoshes, Crov
Nore's macight
es, Atari's macight

W  ll ds  out -hogs aSCRon as thePO re ready.  Right now, we have thso t.  Moment gh the stuff in elons cLETlog and RIT what                                   ave, what we, 71't, what wee ofgetLic.
erso3PAnews n..c
---------------------
Trademarks: BrksL|ter.  Z-rioR|

TuKe Liourc Z3-Dot-Com, ZIALlus, Z-Com, ZCPR, -tpk T, Z-Letter, Z-Node, Z-System, AlphaDSutre T; d of tse II, Ashton-Tate; Unias a c AT&T Bell Labout a TURBO Pascalbmorland Inpatl; MagicFont, CES; CP/M, CP/M NKs, DRI; Z-News, Echelon; Nevafor a COBOL, Ellis ramed. ; (continuge,n page 31)
~=R2880 18 for tsix+
WRITING ABOUT WRITING

Defining the Terms

Since I bovavin 
p Nost city an CHor hote i., fs is w.4,processinggeth a regular column on the sub|

Jgenis aratingopri.

Maor this newx tter.

Note that this _doeserret_ mean that no one else canckate abod Shis field.  Articles on any e'of wubWS|ts ny,8 arateN| accepted.  See page 5, "How to Submit Mate67l".

The term "worwn ncessor" is alafttoo va Toe to be useful.  It includes nots smly more specific tMIOF Che, sace,as 
Eors(ormatters(ont managers, and offic Nanagement  Thi29 11 or 2uest Cr No uses theownftware.  All that the term teld."you is tho. he  Che involgu puted.  words on py rr, le IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIat it isof puAsprimarily_ a ing fve Ster uOLnager, spread sheet, or programming ce. Toration e au  J pr and arvelo, because 8085 word processors can ue wa fair  ret of making ing fta-baseephagerstworead s .ts, and OM ming lanr moges unactUBMry!

Let me, then, define asir hrm, eaOM
of I be using them ch ashe ph  in this cLPmn.  These ICinitions are not fi grd in stur 29t S.,3
 ell you owevlicitly a lI feel onsib them has outlivMinbe ssefulness.  If you notntexonausing one i9 rway) ioesin twfit ctivted onition, please on'funkn l4  Of     se, you a of olso free to disn IeeMS  my ted o  Dons.  No one aFrarited funthe last word on woll,.......................essingL,r, tall er
ual 

An |
Eor| is the sicromlest kit gof   Bacommurocurtng ots  By all C	 or.  Z-centan news athat allowne emayto Us  Bas in a file.  Ps|
Nvirtuinting is purely optional, and may be supported in only  5udfterntary fashionde Cormally, an 
Eor has a crovle IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII send tr Miturnshe ts tr, but-cy!|e or no ability to ers a the ith out look-cyke anything.  If erned it a paragraph indented, you put spaces before the teABOUs orfile; if ymay805t a-cyne centered, you xt.nt the characters s orcroand put n'n pspacever,ore the cro or 2tter,homill ith  centered; and  or 2forthl conk of ~

A |cro
Elig sh.c you the fi mine cro is o specifer.  number of croat a ph ; you can, umove up and dow  2screen, and can only give k lds o. he prompt the 
Eor supplies, upr Nich itFuue w8085is theg t WRIhe linene eou efee speM per. , show them r oou, and issue antpir prompt.  SYSgital Resefrie's CP/M 
Eor ED.COM is a |

.ce fxamplsib a cro
Eor.

A |zinen 
Elig shall aM fhe file a r fn at a tiiviDSllall aM fo scroll1












































 scravedown, move the curs is o'Bor cith n Csorag[eysLic.  Ily sine -Nfor Z-without uesdot rass a 11of And with no ab teOUT  format output to is $riter.  That woUT Tbe a scre12 o	  sigishehat ue wI volby |format obel ut|?  FoTINt is antpir much-IT Mou'ter   BaAmpe speak of the formaStaa diskette(ost oed. yliiskette, and now formated.  obel ut.  W g4I   Jcrewa anu, alter, stformats obel ut.  Z- mean that a lareff the W.uff ymayIT M to efee to do on aITIpeer a r  sew donnta, 'e software.  SuppoS
hat M fMIion,ollowing crowith E W  Ba processorions m
 <sixifor to Raise Gerbid."for Fun and YSit

Noll n if issuing the command P for ith  produced this cleE Wld  .....................
1981to shedise the Urbils for vvn and ers tt

then E Won p.......................ais is , Alnly an eabir (you Usthe teABOUint EDot t), bu
 Nformatter; it obeyeavin iMo$2.ctis vmbedded i  2file ere. d She texts sm the or.for m9 rplace not r $ctly co.

DG ding to its place ubm2


A |formatter|, then, is Duhat understands commands that and can place int EDot t.  These  in 1 control the placement of tr Mi on copinted page, WELdgmay. Mcank  to Usin a loStacar67ge returns, crofeeds, and spa.2to e Wf Ar Mi placed just so.  Most is, eabirs are also plmatters.  iforever, y, t as the 
Eing capabilities of these on p.......................aiss are ihere's that on pagain!) va, Cawideews or 2ue wble s ford thaing abilities.  AllIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIItters are _not_ created 80, ual!

All Cabir and a f, alter ue wnot efee to be combins Zi a single fullyce of ots  Ipats per-Com tly possible, and in rer deveironments is the normaln der
ntex   e	  a file, exit ve tothe 
Eor, and then formaty nand ds  it to copi ' by  doining 

Est oter s vt h The Z-System athity PRINT is an indepe domlessst ogoeith no 
Eing offeab teies cleallreetmaytype "cornt myindex o8"(ofor mMoance, to send a file nao n MYINDEX to E Wld .  PRINT automat wrt taL|t she file..c
|e ani fpage meomber on top of maileito9
 unlesne emaychhellse ras-crooptionscrewinst$2.genit not to do so.s r option lcsen here, "o8", teld."it .

Wet 8 spacever,ore maileline MI and ulaed.  a left margin of let lor GAEven a lE Won p.......................ais combines 
E is ond formatter, ch yay wish to use',ur 
Eor because you are IT M to it, puted.  in 2 fthat volnotight
g to ro  and then using a seystem se f, alter to p of cion,2
 s rre xt.ld be any meomber of reaustrs to ue wsofornithe aft 8ly is tho. he f, alter hasownfunab te, 'o. he f, alter built iy u your on p.......................ais does not.  If',ur on p.......................ais is nd not handle proportional spacing well, or hao Uo 2 fto switch fonts on a laser ld y ycriptiill probably switchgethoner or re pr, to 

Est oter that does.  If the formatter cane fird fear. pro theid by E Wold, familiar 
Eor, why learn a new seSta
Eaperovandsre n
Tnnuefine 

Ent manager, I  insfirst ted one pplt h A |SCIt, stis a collec25~
.of characters that efee the safun ce of characts|stic, eaace,as Uprios.

 us Sce.ted, Bowi or Medium or Light THurM-ITIpetsee 408s Helvetian typetir 408s Old English  for sace versus zillions of tpir, Portrait or Landscy r orientation o  2itoe, a particular point sio, c (height),  ...........................rticve. pbigh (width), fi grd or p79rtional slorne t a partic SUBsymbol see pretc.  Fthem71xore sle, thpelfigetter is ts td using a font rtmh is in he pndt fpe os hintation (sideways on the itoe), Roman let yOEl am (never minor chat thatl Zns'fixed sp trang, 10 characterscommuer-inch pinm, 12 point height, upright  new ot italic), o nium weight, and CourM-ITIpe face.     F, Cacharacter -Sysis font shares all ish,e characts|sti, or .

A If nt FOger #s what popall Duhat allall a4  print tr Mi in a vas hity of SCIts.  TIs wrly this irsne emayefee ahe dserS .................................... ', and the fon.  A anilit allowne emayto  and beUDrovandwitho sst hh between difthe mnt SCIts an abCOM)e o  2laser.  Bd SEDare rerSCIt riptioas ihich t5
 advantage of the graphics capab teies of Pro -matrix ld s.  Fofor mMoance, Fancy Font Z80tpiTO 
Nucts ve toSoftCraft let you ith r, tall kindOFSCIts on Epson FX-80s and compatible arte (udi al or 2efen-he pserJick

 ionid p
he pog'SCIts may be |residenith i, in which .9,e they are built ihmcopi 'd thiNon-8 Aent SCIts tten  be on cartridledors h are stor E2 co she ms on the laser; asie agen 8 resident arteus ilong as the card is in the macight
e.  The tpir kit gof non-here nt laser SCIt is the |soft font, strtmh here s in the user as a file, and is coper. turnshe ld  when bovded.

If it sounds ly ppplt riptioer must al or 2be a ford thaer.  Soru're t. nearlyyyyNight.  They  monally are  4
e 1he font manilit is the softwarecrewer
ual5, ith s the file, it must eitheredit ford thaer, or it  insworkS H a file that has been formatted on diskgethcrewona vhe font manilit's crovandn '9
 leTwubmile, and the teABOUis tpirHAN .xer
ly newsleit ny,e p of cge,n each itoe.

uncter so 'Bd od She pian Jre, theBR or 2exe T font 
Eors and SCIt compileerti  A If nt 
Elig is urces, artllall a4  e	  a font file and ccles the shape or the detailOFcharacters ubmont h Since SCIPsles are not  mony artext, this re-5es special software.  A ghelld debugger  8 ZPATCHe ofbe IT M to ers a spot chaitetion, bina, CaSCIPsle, if ymayknEMthe st$2.an Jre of the file preciseewsR 3.this is thell errorcommurone for , d ccless.

A |SCIt retu
Eor, and ght-b's 6 a binary fonPsle ve toit.  Computer Edia ae Systems, for 
Eor rtmh will Used oingle charact, "rom one of their SCIts                        an e, phfile, and issue n, bersa.  They alredlumof A compiler rtmh transre ps their  Chaaphas bts miseHP soTwSCIts.

S Ea woll,.......................aiss t, Cato do everyis theg, and rerare pretty |

.avit it h I call a on p.......................ais |offw)riptioement, stnews aa lit combines , Alnly71abir and formatAlpha 29Mcfunof the funcphsof a dLETPTe, spread ss G, r, agramminProuanr moge.  WordStar  set officOLnagement  Thi29t if',ucloine its Bes e	 ne t for-wail and macro functions with tpir their such as MailMerge, DataStar, etc., you begin thsickthere.  Spellbinder Word Procais, from L-Tek.  Z-nc., doue t mace,better  retus i thance manageCP/M f pe  It has a core fetn adogramminProu--uage built in, and71xceeds the offeab teies of g)itsib software just mentioneavill by itself, using macgl.

Welal mo of oks for bearing with mede Cr Mi issue lon l disailas the TORicCeiesS H C., f Edia ae Systems.    erso3Pavid A.J. McGlonement iat wwwwwwwwwwwwwwwwwwwww

(Tons smarks contimeoed from itoe 24) FX-80, Qh.10, Epson; he pzeet, HP; Hs M180, Hs M180ZHELitachi; AT.  Z-BM; Spellthler, L-Tek Inc.; SELEharOR, Micro Ap; fore80, SB180FX7-icromint; -Ntar7-ailMeindu, DataStar, wneo Pro; ON!, 4.0eac;  of tckGr ker RID, NK*Per-Com t Systems; BA APs, Perset, Sortmk|

Tsurve LiSopennk Creek ramed.  Serissue es; yearsnc.bone prSoft#tafemo Z80, Z180, Z280, Zilog erso and please coryou.onaa lI got any of them wrong!
 */