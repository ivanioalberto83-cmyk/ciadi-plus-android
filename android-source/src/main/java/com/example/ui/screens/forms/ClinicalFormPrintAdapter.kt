package com.example.ui.screens.forms

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.FileOutputStream

class ClinicalFormPrintAdapter(
    private val title: String,
    private val body: String
) : PrintDocumentAdapter() {

    private var pageWidth = 595
    private var pageHeight = 842

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        pageWidth = newAttributes.mediaSize?.widthMils?.times(72)?.div(1000) ?: 595
        pageHeight = newAttributes.mediaSize?.heightMils?.times(72)?.div(1000) ?: 842
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("$title.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PdfDocument.PageInfo.UNDEFINED)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out android.print.PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val pdf = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
        }
        val lines = ("CIADI+\n$title\n\n$body").lines()
        val lineHeight = 16f
        var pageNumber = 1
        var lineIndex = 0

        try {
            while (lineIndex < lines.size) {
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled()
                    pdf.close()
                    return
                }
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdf.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                var y = 40f
                while (lineIndex < lines.size && y < pageHeight - 40f) {
                    canvas.drawText(lines[lineIndex].take(105), 36f, y, paint)
                    y += lineHeight
                    lineIndex++
                }
                pdf.finishPage(page)
                pageNumber++
            }
            FileOutputStream(destination.fileDescriptor).use { output ->
                pdf.writeTo(output)
            }
            callback.onWriteFinished(arrayOf(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create().let { PageRange.ALL_PAGES }))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        } finally {
            pdf.close()
        }
    }
}
