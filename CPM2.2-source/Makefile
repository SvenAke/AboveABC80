#
# Unix Makefile for CP/M 2.2
#
OBJS=mcd80.obj mcd80p.obj ed.obj pip.obj submit.obj
#
HEXS=as0com.hex as1io.hex as2scan.hex as3sym.hex as4sear.hex as5oper.hex \
	as6main.hex ddt0mov.hex ddt1asm.hex ddt2mon.hex submit.hex dump.hex \
	help.hex

BLKS=ed load pip stat submit

MCOMS = asm.com dump.com ddt.com ed.com load.com pip.com stat.com submit.com \
	xsub.com help.com #movcpm.com sysgen.com

BDOS = os2ccp.spr os3bdos.spr os2ccp.prl os3bdos.prl

ZXCC = zxcc
THAMES = ./runthames

MAC=mac.com
RMAC=rmac.com
LINK=drlink.com

BINARIES= asm.com ddt.com dump.com ed.com load.com pip.com stat.com submit.com \
	  xsub.com $(BDOS) help.com help.hlp README

HELPFILES= asm.help commands.help ctlchars.help ddt.help dir.help dump.help \
	ed.help era.help fspec.help help.help load.help pip.help ren.help \
	save.help stat.help submit.help type.help user.help

SOURCES= runthames \
 as0com.asm   as6main.asm  ddt2mon.asm  Makefile     sysgen.asm \
 as1io.asm    bios.asm	  deblock.asm  mcd80.asm    os3bdos.asm   xsub0.asm \
 as2scan.asm  cbios.asm	  dump.asm     os4bios.asm   xsub1.asm \
 as3sym.asm   cpmove.asm   ed20pat.asm  mcd80p.asm   pip.plm making.txt \
 as4sear.asm  ddt0mov.asm  ed.plm       os1boot.asm  stat.plm \
 as5oper.asm  ddt1asm.asm  load.plm     os2ccp.asm   submit.plm \
 hexcom.c hexpat.c drlink.com mac.com rmac.com help.plm $(HELPFILES)

all:	$(MCOMS) $(BDOS) help.hlp

zip:	cpm22src_unix.zip cpm22bin_unix.zip

cpm22src_unix.zip:	$(SOURCES)
	zip $@ $(SOURCES)

cpm22bin_unix.zip:	$(BINARIES)
	zip $@ $(BINARIES)

############################################################################
#
# Build tools
#
hexcom:		hexcom.c
	${CC} -o hexcom hexcom.c

hexpat:		hexpat.c
	${CC} -o hexpat hexpat.c


##############################################################################
#
# Specific build rules
#
#
# ed:
# * Builds with a stack
# * Applies an assembled patch (ed20pat) to improve performance
#
ed.mod:	ed.obj mcd80p.obj
	$(THAMES) :F3:link mcd80p.obj,$<,:F1:plm80.lib to $@

ed.tra:	ed.mod
	$(THAMES) :F3:locate $< code\(0100h\) stacksize\(80\) map print\($@\)

ed.com:	ed.hex ed20pat.hex hexcom hexpat
	./hexcom ed.bin <ed.hex
	./hexpat ed.bin ed.com < ed20pat.hex
#
# PIP uses a slightly different BDOS binding (mcd80p rather than mcd80)
#
pip.mod:	pip.obj mcd80p.obj
	$(THAMES) :F3:link $<,mcd80p.obj,:F1:plm80.lib to $@

submit.com:	submit.hex hexcom
	./hexcom $@ <$<

submit.mod: 	submit.obj mcd80.obj
	$(THAMES) :F3:link $<,mcd80.obj,:F1:plm80.lib to $@

mcd80.obj:	mcd80.asm
	$(THAMES) :F2:asm80 $<

mcd80p.obj:	mcd80p.asm
	$(THAMES) :F2:asm80 $<

##############################
#
# BDOS and CCP
#

os2ccp.spr:	os2ccp.rel
	$(ZXCC) $(LINK) os2ccp.spr +-= os2ccp +-[OS]

os2ccp.prl:	os2ccp.rel
	$(ZXCC) $(LINK) os2ccp.prl +-= os2ccp +-[OP]

os3bdos.spr:	os3bdos.rel
	$(ZXCC) $(LINK) os3bdos.spr +-= os3bdos +-[OS]

os3bdos.prl:	os3bdos.rel
	$(ZXCC) $(LINK) os3bdos.prl +-= os3bdos +-[OP]

##############################
#
# ASM
#
ASMFILES = as0com.hex as1io.hex as2scan.hex as3sym.hex as4sear.hex as5oper.hex \
	   as6main.hex

%.hex:	%.asm
	$(ZXCC) mac `basename $< .asm`

asm.com:	$(ASMFILES) hexcom hexpat
	./hexcom as0.com < as0com.hex
	./hexpat as0.com as1.com <as1io.hex
	./hexpat as1.com as2.com <as2scan.hex
	./hexpat as2.com as3.com <as3sym.hex
	./hexpat as3.com as4.com <as4sear.hex
	./hexpat as4.com as5.com <as5oper.hex
	./hexpat as5.com asm.com <as6main.hex
	rm as0.com as1.com as2.com as3.com as4.com as5.com

##############################
#
# DDT 
#
ddt.spr:	ddt1asm.rel ddt2mon.rel
	$(ZXCC) $(LINK) ddt.spr +-= +ddt1asm +-, +ddt2mon +-[OS]

ddt0mov.hex:	ddt0mov.asm
	$(ZXCC) $(MAC) ddt0mov

ddt.com:	hexpat ddt.spr ddt0mov.hex
	./hexpat ddt.spr $@ <ddt0mov.hex
#
##############################################################################

xsub1.spr:	xsub1.rel
	$(ZXCC) $(LINK) "-xsub1.spr=xsub1[OS]"
#	$(ZXCC) $(LINK) xsub1.spr += +xsub1 +-[OS]
	

xsub0.hex:	xsub0.asm
	$(ZXCC) $(MAC) xsub0

xsub.com:	xsub1.spr xsub0.hex hexpat
	./hexpat xsub1.spr $@ < xsub0.hex

##############################################################################

dump.hex:	dump.asm
	zxcc mac `basename $< .asm`

##############################################################################
#
# Generic build rules
#

%.obj:	%.asm
	$(THAMES) :F2:asm80 $< debug

%.rel:	%.asm
	$(ZXCC) $(RMAC) `basename $< .asm`


###########################################################################
#
# COM files from hex files
#
%.com:	%.hex hexcom
	./hexcom $@ < $<

###########################################################################
#
# HEX files from asm source
#
%.hex: %.asm 
	$(ZXCC) $(MAC) `basename $< .asm`

###########################################################################
#
# HEX files from PL/M source
#
%.hex: %.tra
	$(THAMES) :F3:objhex `basename $< .tra` to $@

# The "%.tra" rule also builds "%", which is what objhex actually uses, but
# I couldn't get a bare % rule to work.
%.tra:	%.mod
	$(THAMES) :F3:locate $< code\(0100h\) map print\($@\)

%.mod:	%.obj mcd80.obj
	$(THAMES) :F3:link $<,mcd80.obj,:F1:plm80.lib to $@

%.obj:	%.plm
	$(THAMES) :F1:plm80 $< optimize debug	


help.hlp:	help.dat help.com
	zxcc help.com -\[C\]

help.dat:	$(HELPFILES)
	cat $(HELPFILES) > $@

#
# 
#
clean:
	rm -f $(MCOMS) $(HEXS) $(BLKS) *.lst *.rel *.sym *.tra *.spr \
	*.mod *.obj
