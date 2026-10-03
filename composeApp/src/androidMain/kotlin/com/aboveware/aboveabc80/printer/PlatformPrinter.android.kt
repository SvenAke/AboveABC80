package com.aboveware.aboveabc80.printer

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.graphics.withScale
import com.aboveware.aboveabc80.androidContext
import java.io.FileOutputStream
import java.io.IOException

actual fun printPrinterBuffer(lines: List<IntArray>) {
    val context = androidContext ?: return
    if (lines.isEmpty()) return

    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    printManager?.let {
        val jobName = "AboveCPM Printer Output"
        it.print(jobName, ZXPrintAdapter(lines), PrintAttributes.Builder().build())
    }
}

class ZXPrintAdapter(private val lines: List<IntArray>) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        // 72 points per inch. Virtual DPI is 2160.
        val scale = 72f / VirtualPrinter.VIRTUAL_DPI
        val totalHeightPoints = (lines.size * scale).toInt()
        val pointsPerPage = 792 // 11 inches
        val pages = (totalHeightPoints + pointsPerPage - 1) / pointsPerPage

        val info = PrintDocumentInfo.Builder("printer_output.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(maxOf(1, pages))
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val pdfDocument = PdfDocument()
        val scale = 72f / VirtualPrinter.VIRTUAL_DPI
        val pointsPerPage = 792 // 11 inches at 72 DPI

        // Since each entry in 'lines' is essentially 1 point high (72 DPI vertical),
        // we don't need to scale Y, and one virtual line = one point.
        val virtualLinesPerPage = pointsPerPage

        val totalPages = (lines.size + virtualLinesPerPage - 1) / virtualLinesPerPage

        val blackPaint = Paint().apply { color = Color.BLACK }
        val redPaint = Paint().apply { color = Color.RED }
        val barPaint = Paint().apply { color = Color.argb(15, 0, 100, 0) }

        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(612, pointsPerPage, p + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            canvas.withScale(scale, 1f) {
                // Scale X to fit 2160 DPI into 72 DPI width, keep Y at 1:1 (one line = one point)
                translate(0f, -(p * virtualLinesPerPage).toFloat())

                val startY = p * virtualLinesPerPage
                val endY = minOf(startY + virtualLinesPerPage, lines.size)

                // Draw background bars (0.5 inch = 36 lines at 72 DPI)
                for (barY in (startY / 36) * 36 until endY step 72) {
                    val barTop = maxOf(startY, barY).toFloat()
                    val barBottom = minOf(endY, barY + 36).toFloat()
                    if (barBottom > barTop) {
                        drawRect(
                            0f,
                            barTop,
                            VirtualPrinter.PRINTER_WIDTH_TOTAL.toFloat(),
                            barBottom,
                            barPaint
                        )
                    }
                }

                for (y in startY until endY) {
                    val line = lines[y]
                    for (x in line.indices) {
                        val pixel = line[x]
                        if (pixel != 0) {
                            // Draw a dot. Horizontal size is 30 virtual units (1 point).
                            // Vertical size is 1.0 (1 point).
                            drawRect(
                                x.toFloat(),
                                y.toFloat(),
                                x.toFloat() + 30f,
                                y.toFloat() + 1f,
                                if (pixel == 2) redPaint else blackPaint
                            )
                        }
                    }
                }
            }
            pdfDocument.finishPage(page)
        }

        try {
            val fd = destination?.fileDescriptor
            if (fd != null) {
                pdfDocument.writeTo(FileOutputStream(fd))
            } else {
                callback.onWriteFailed("File descriptor is null")
                return
            }
        } catch (e: IOException) {
            callback.onWriteFailed(e.toString())
            return
        } finally {
            pdfDocument.close()
        }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }
}
