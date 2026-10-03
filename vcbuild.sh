#!/usr/bin/env bash
set -euo pipefail

# ============================================================
#  VisiCalc Diskettgenerator – CP/M ASCII Edition
#  Bygger 8 datadisketter (.DSK + .IMD) för Kaypro & Generic
# ============================================================

# --- Kontrollera verktyg ------------------------------------
need() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "ERROR: $1 saknas. Installera cpmtools + imd." >&2
        exit 1
    }
}
need mkfs.cpm
need cpmcp
need imd

# --- Katalogstruktur ----------------------------------------
OUTDIR="visicalc_disks"
mkdir -p "$OUTDIR"

# --- Innehållsfiler -----------------------------------------
# Lägg dina faktiska filer här:
VC_KAYPRO="VC_KAYPRO.COM"
VC_GENERIC="VC_GENERIC.COM"
DEMO="DEMO.VC"
README="README.TXT"

for f in "$VC_KAYPRO" "$VC_GENERIC" "$DEMO" "$README"; do
    [[ -f "$f" ]] || { echo "ERROR: Fil saknas: $f"; exit 1; }
done

# --- Funktion: skapa CP/M-diskett ---------------------------
make_disk() {
    local name="$1"
    local layout="$2"
    local vcbin="$3"

    local dsk="$OUTDIR/${name}.DSK"
    local imdfile="$OUTDIR/${name}.IMD"

    echo "Bygger $name..."

    # Skapa DSK
    mkfs.cpm -f "$layout" "$dsk"

    # Kopiera filer
    cpmcp -f "$layout" "$dsk" "$vcbin" 0:VC.COM
    cpmcp -f "$layout" "$dsk" "$DEMO" 0:DEMO.VC
    cpmcp -f "$layout" "$dsk" "$README" 0:README.TXT

    # Konvertera till IMD
    imd -c "$dsk" "$imdfile"
}

# ============================================================
#  Kaypro-disketter (1:1 och 2:1)
# ============================================================

# Kaypro layout i cpmtools:
# kp2 – Kaypro II (512 bytes, 10 sektorer, 40 spår)
# Interleave hanteras via IMD-konverteringen

make_disk "VC_KP11" "kp2" "$VC_KAYPRO"
make_disk "VC_KP21" "kp2" "$VC_KAYPRO"

# ============================================================
#  Generic CP/M-2.2-disketter (1:1 och 2:1)
# ============================================================

# Generic CP/M layout:
# std – standard CP/M-2.2 (128 bytes, 26 sektorer)

make_disk "VC_GN11" "std" "$VC_GENERIC"
make_disk "VC_GN21" "std" "$VC_GENERIC"

echo "Alla disketter klara i: $OUTDIR/"
