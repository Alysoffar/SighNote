package com.notes.ink.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.notes.ink.mapping.InkStrokeMapper
import com.notes.ink.persistence.InkRepository
import com.notes.ink.persistence.PageEntity
import com.notes.model.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfManager {

    // Cache rendered PDF page bitmaps: key = "$pdfPath-$pageIndex-$scale"
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Imports a PDF from a content Uri, copies it into internal app storage under the notebook dir,
     * and creates a PageEntity for each page in the PDF.
     */
    suspend fun importPdf(
        context: Context,
        pdfUri: Uri,
        notebookId: String,
        repository: InkRepository
    ): List<PageEntity> = withContext(Dispatchers.IO) {
        val notebookDir = File(context.filesDir, "notebooks/$notebookId").apply { mkdirs() }
        val destFile = File(notebookDir, "imported_${System.currentTimeMillis()}.pdf")

        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Could not read URI $pdfUri")

        val pageCount = getPdfPageCount(destFile)
        val createdPages = mutableListOf<PageEntity>()

        for (i in 0 until pageCount) {
            val page = repository.addPage(
                notebookId = notebookId,
                pdfPath = destFile.absolutePath,
                pdfPageIndex = i
            )
            createdPages.add(page)
        }

        createdPages
    }

    fun getPdfPageCount(file: File): Int {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val count = renderer.pageCount
        renderer.close()
        pfd.close()
        return count
    }

    /**
     * Renders a PDF page to a Bitmap matching requested dimensions (or native page size * scale factor).
     */
    fun renderPageBitmap(
        pdfPath: String,
        pageIndex: Int,
        targetWidth: Int = 1200,
        targetHeight: Int = 1600
    ): Bitmap? {
        val file = File(pdfPath)
        if (!file.exists()) return null

        val cacheKey = "$pdfPath-$pageIndex-$targetWidth-$targetHeight"
        bitmapCache.get(cacheKey)?.let { return it }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

            page = renderer.openPage(pageIndex)
            val w = if (targetWidth > 0) targetWidth else page.width
            val h = if (targetHeight > 0) targetHeight else page.height

            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            bitmapCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * Exports a notebook (with its background/PDF and ink strokes) into a flattened PDF document.
     */
    suspend fun exportNotebookToPdf(
        context: Context,
        notebookId: String,
        repository: InkRepository
    ): File = withContext(Dispatchers.IO) {
        val notebook = repository.getNotebook(notebookId)
        val pages = repository.getPagesListForNotebook(notebookId)
        val title = notebook?.title ?: "Notebook"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "${title.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf")

        val pdfDoc = PdfDocument()
        val renderer = CanvasStrokeRenderer.create()

        val defaultPageWidth = 1200
        val defaultPageHeight = 1600

        for ((idx, pageEntity) in pages.withIndex()) {
            val strokes = repository.getStrokesList(pageEntity.id)
            val pageInfo = PdfDocument.PageInfo.Builder(defaultPageWidth, defaultPageHeight, idx + 1).create()
            val docPage = pdfDoc.startPage(pageInfo)
            val canvas = docPage.canvas

            // 1. Draw PDF background if present, else plain white background with ruled lines
            if (pageEntity.pdfPath != null) {
                val bgBitmap = renderPageBitmap(pageEntity.pdfPath, pageEntity.pdfPageIndex, defaultPageWidth, defaultPageHeight)
                if (bgBitmap != null) {
                    canvas.drawBitmap(bgBitmap, 0f, 0f, null)
                } else {
                    canvas.drawColor(Color.WHITE)
                }
            } else {
                canvas.drawColor(Color.WHITE)
                val linePaint = Paint().apply {
                    color = 0x1F000000
                    strokeWidth = 1f
                }
                for (y in 80 until defaultPageHeight step 50) {
                    canvas.drawLine(0f, y.toFloat(), defaultPageWidth.toFloat(), y.toFloat(), linePaint)
                }
            }

            // 2. Draw Ink Strokes (in page space)
            val identityMatrix = Matrix()
            for (stroke in strokes) {
                val brush = resolveBrushForExport(stroke.brush_id)
                val inkStroke = InkStrokeMapper.toInkStroke(stroke, brush)
                renderer.draw(canvas, inkStroke, identityMatrix)
            }

            pdfDoc.finishPage(docPage)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        outputFile
    }

    private fun resolveBrushForExport(brushId: String): Brush {
        val color = when {
            brushId.contains("blue") -> androidx.compose.ui.graphics.Color(0xFF1E88E5)
            brushId.contains("red") -> androidx.compose.ui.graphics.Color(0xFFE53935)
            brushId.contains("highlighter_yellow") -> androidx.compose.ui.graphics.Color(0x66FFEB3B)
            brushId.contains("highlighter_blue") -> androidx.compose.ui.graphics.Color(0x6680D8FF)
            brushId.contains("highlighter_green") -> androidx.compose.ui.graphics.Color(0x66CCFF90)
            else -> androidx.compose.ui.graphics.Color.Black
        }
        val size = when {
            brushId.contains("highlighter") -> 24f
            brushId.contains("size_4") || brushId.contains("size_3") -> 4f
            brushId.contains("size_16") || brushId.contains("size_12") -> 16f
            else -> 8f
        }
        return Brush.createWithComposeColor(
            family = StockBrushes.pressurePen(),
            color = color,
            size = size,
            epsilon = 0.1f
        )
    }
}
