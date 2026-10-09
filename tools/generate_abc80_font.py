"""Generate and verify a TrueType font from the ABC80 monitor's text glyphs."""

import re
from pathlib import Path

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (
    ROOT / "composeApp" / "src" / "commonMain" / "kotlin" / "com"
    / "aboveware" / "aboveabc80" / "Abc80MonitorCharacterMap.kt"
)
OUTPUT = ROOT / "fonts" / "ABC80.ttf"
WIDTH, HEIGHT, PIXEL = 8, 14, 64
ASCENT, DESCENT = 11 * PIXEL, -3 * PIXEL
UNICODE_CODES = {
    36: 0x00A4,
    64: 0x00C9,
    91: 0x00C4,
    92: 0x00D6,
    93: 0x00C5,
    94: 0x00DC,
    96: 0x00E9,
    123: 0x00E4,
    124: 0x00F6,
    125: 0x00E5,
    126: 0x00FC,
    127: 0x2588,
}


def read_text_glyphs() -> dict[int, list[int]]:
    source = SOURCE.read_text(encoding="utf-8")
    array = source.split("private val characters = ubyteArrayOf(", 1)[1]
    values = [int(bits, 2) for bits in re.findall(r"0b([01]{8})u", array)]
    if len(values) < 96 * HEIGHT:
        raise ValueError("The monitor table must contain all 96 text glyphs")
    return {
        code: values[(code - 32) * HEIGHT:(code - 31) * HEIGHT]
        for code in range(32, 128)
    }


def pixel_rectangles(rows: list[int]) -> list[tuple[tuple[int, int], ...]]:
    return [
        (
            (column * PIXEL, ASCENT - (row + 1) * PIXEL),
            (column * PIXEL, ASCENT - row * PIXEL),
            ((column + 1) * PIXEL, ASCENT - row * PIXEL),
            ((column + 1) * PIXEL, ASCENT - (row + 1) * PIXEL),
        )
        for row, bits in enumerate(rows)
        for column in range(WIDTH)
        if bits & (0x80 >> column)
    ]


def verify_font(path: Path, rows_by_code: dict[int, list[int]]) -> None:
    with TTFont(path, checkChecksums=2) as font:
        expected_map = {
            UNICODE_CODES.get(code, code): f"uni{UNICODE_CODES.get(code, code):04X}"
            for code in rows_by_code
        }
        if font.getBestCmap() != expected_map:
            raise ValueError("The saved font's Unicode map differs from the text character set")
        if font["maxp"].numGlyphs != 97 or not font["post"].isFixedPitch:
            raise ValueError("Expected 96 text glyphs, .notdef and fixed-pitch metrics")
        for code, rows in rows_by_code.items():
            name = expected_map[UNICODE_CODES.get(code, code)]
            glyph = font["glyf"][name]
            rectangles = pixel_rectangles(rows)
            coordinates, ends, flags = glyph.getCoordinates(font["glyf"])
            expected_points = [point for rectangle in rectangles for point in rectangle]
            if list(coordinates) != expected_points:
                raise ValueError(f"Pixel outline mismatch for ABC80 code {code}")
            if list(ends) != [4 * index + 3 for index in range(len(rectangles))]:
                raise ValueError(f"Contour mismatch for ABC80 code {code}")
            if any(not flag & 1 for flag in flags):
                raise ValueError(f"Unexpected curves for ABC80 code {code}")
            expected_lsb = min((point[0] for point in expected_points), default=0)
            if font["hmtx"][name] != (WIDTH * PIXEL, expected_lsb):
                raise ValueError(f"Spacing mismatch for ABC80 code {code}")


def main() -> None:
    rows_by_code = read_text_glyphs()
    builder = FontBuilder(HEIGHT * PIXEL, isTTF=True)
    cmap = {
        UNICODE_CODES.get(code, code): f"uni{UNICODE_CODES.get(code, code):04X}"
        for code in rows_by_code
    }
    builder.setupGlyphOrder([".notdef", *cmap.values()])
    builder.setupCharacterMap(cmap)
    glyphs = {".notdef": TTGlyphPen(None).glyph()}
    metrics = {".notdef": (WIDTH * PIXEL, 0)}
    for code, rows in rows_by_code.items():
        name = cmap[UNICODE_CODES.get(code, code)]
        rectangles = pixel_rectangles(rows)
        pen = TTGlyphPen(None)
        for rectangle in rectangles:
            pen.moveTo(rectangle[0])
            for point in rectangle[1:]:
                pen.lineTo(point)
            pen.closePath()
        glyphs[name] = pen.glyph()
        metrics[name] = (
            WIDTH * PIXEL,
            min((point[0] for rectangle in rectangles for point in rectangle), default=0),
        )
    builder.setupGlyf(glyphs)
    builder.setupHorizontalMetrics(metrics)
    builder.setupHorizontalHeader(ascent=ASCENT, descent=DESCENT, lineGap=0)
    builder.setupNameTable({
        "familyName": "ABC80",
        "styleName": "Regular",
        "uniqueFontIdentifier": "AboveABC80:ABC80:Regular:1.000",
        "fullName": "ABC80 Regular",
        "psName": "ABC80-Regular",
        "version": "Version 1.000",
        "description": "ABC80 8x14 monitor text glyphs with Unicode Swedish letters.",
    })
    builder.setupOS2(
        sTypoAscender=ASCENT, sTypoDescender=DESCENT, sTypoLineGap=0,
        usWinAscent=ASCENT, usWinDescent=-DESCENT, fsType=0,
        sxHeight=7 * PIXEL, sCapHeight=9 * PIXEL,
    )
    builder.setupPost(isFixedPitch=1)
    builder.setupMaxp()
    builder.setupHead(created=2082844800, modified=2082844800)
    builder.font.recalcTimestamp = False
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    builder.save(OUTPUT)
    verify_font(OUTPUT, rows_by_code)
    print(f"Created and verified {OUTPUT}: 96 text glyphs, no graphics glyphs.")


if __name__ == "__main__":
    main()
