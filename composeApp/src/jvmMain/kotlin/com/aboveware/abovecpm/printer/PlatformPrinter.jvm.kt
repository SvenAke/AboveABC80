package com.aboveware.abovecpm.printer

import java.awt.Color
import java.awt.Graphics
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterJob

actual fun printPrinterBuffer(lines: List<IntArray>) {
    if (lines.isEmpty()) return

    val job = PrinterJob.getPrinterJob()
    job.setPrintable(object : Printable {
        override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
            // Horizontal scale: from 2160 DPI to 72 DPI (1/30)
            val scale = 72.0 / VirtualPrinter.VIRTUAL_DPI
            // Vertical: one entry in 'lines' = 1 point (72 DPI)
            val virtualLinesPerPage = pageFormat.imageableHeight.toInt()
            val startLine = pageIndex * virtualLinesPerPage

            if (startLine >= lines.size) return Printable.NO_SUCH_PAGE

            val g2d = graphics as java.awt.Graphics2D
            g2d.translate(pageFormat.imageableX, pageFormat.imageableY)

            g2d.color = Color.WHITE
            g2d.fillRect(
                0,
                0,
                pageFormat.imageableWidth.toInt(),
                pageFormat.imageableHeight.toInt()
            )

            g2d.scale(scale, 1.0) // Scale only X
            g2d.translate(0.0, -startLine.toDouble())

            val blueBarColor = Color(0xF4, 0xFA, 0xF4)
            val width = VirtualPrinter.PRINTER_WIDTH_TOTAL

            val endLine = minOf(startLine + virtualLinesPerPage, lines.size)

            // Draw background bars (0.5 inch = 36 lines)
            for (barY in (startLine / 36) * 36 until endLine step 72) {
                val barTop = maxOf(startLine, barY)
                val barBottom = minOf(endLine, barY + 36)
                if (barBottom > barTop) {
                    g2d.color = blueBarColor
                    g2d.fillRect(0, barTop, width, barBottom - barTop)
                }
            }

            for (y in startLine until endLine) {
                val line = lines[y]
                line.forEachIndexed { x, pixel ->
                    if (pixel != 0) {
                        g2d.color = if (pixel == 2) Color.RED else Color.BLACK
                        // Dot size: approx 1 point (30 virtual units) wide, 1 point high
                        g2d.fillRect(x, y, 30, 1)
                    }
                }
            }
            return Printable.PAGE_EXISTS
        }
    })

    if (job.printDialog()) {
        try {
            job.print()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
