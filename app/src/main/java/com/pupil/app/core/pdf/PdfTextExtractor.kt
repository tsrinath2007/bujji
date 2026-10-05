package com.pupil.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class ExtractedPage(
    val pageNumber: Int,
    val text: String,
    val usedOcr: Boolean
)

@kotlinx.serialization.Serializable
data class StudySection(
    val sectionIndex: Int,
    val title: String,
    val content: String,
    val pageReference: String
)

class PdfTextExtractor(private val context: Context) {

    companion object {
        private const val TAG = "PdfTextExtractor"
        private const val MIN_TEXT_THRESHOLD = 50 // Fewer characters implies scanned/image page
    }

    init {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PDFBoxResourceLoader", e)
        }
    }

    /**
     * Extracts text page-by-page from a PDF Uri.
     * Uses PdfBox-Android for digital text.
     * Automatically falls back to rendering + ML Kit OCR for scanned/image pages.
     */
    suspend fun extractPages(pdfUri: Uri): List<ExtractedPage> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<ExtractedPage>()

        var inputStream: InputStream? = null
        var pdDocument: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalArgumentException("Cannot open PDF URI: $pdfUri")

            pdDocument = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val totalPages = pdDocument.numberOfPages

            Log.d(TAG, "Opened PDF with $totalPages pages.")

            for (pageIndex in 0 until totalPages) {
                val pageNumber = pageIndex + 1
                stripper.startPage = pageNumber
                stripper.endPage = pageNumber
                val rawText = stripper.getText(pdDocument)?.trim() ?: ""

                if (rawText.length >= MIN_TEXT_THRESHOLD) {
                    pages.add(ExtractedPage(pageNumber = pageNumber, text = rawText, usedOcr = false))
                } else {
                    // Page is image-only or scanned: render page and apply ML Kit OCR
                    Log.d(TAG, "Page $pageNumber has low digital text (${rawText.length} chars). Running ML Kit OCR...")
                    val ocrText = renderAndOcrPage(pdfUri, pageIndex)
                    pages.add(
                        ExtractedPage(
                            pageNumber = pageNumber,
                            text = if (ocrText.isNotBlank()) ocrText else rawText,
                            usedOcr = true
                        )
                    )
                }
            }
        } finally {
            try {
                pdDocument?.close()
                inputStream?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing PDF resources", e)
            }
        }

        pages
    }

    /**
     * Renders a specific page to a Bitmap using Android's native PdfRenderer
     * and performs on-device OCR using ML Kit.
     */
    private fun renderAndOcrPage(pdfUri: Uri, pageIndex: Int): String {
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        var bitmap: Bitmap? = null

        return try {
            // Copy URI content to a temporary file for ParcelFileDescriptor
            tempFile = File.createTempFile("pdf_ocr_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            if (pageIndex < renderer.pageCount) {
                page = renderer.openPage(pageIndex)
                // Render at 2x scale for crisp OCR accuracy
                val width = page.width * 2
                val height = page.height * 2
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val visionText = Tasks.await(recognizer.process(image))
                visionText.text.trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing ML Kit OCR on page $pageIndex", e)
            ""
        } finally {
            try {
                bitmap?.recycle()
                page?.close()
                renderer?.close()
                pfd?.close()
                tempFile?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Cleanup error in OCR", e)
            }
        }
    }

    /**
     * Splits extracted pages into teachable study sections (~5 pages per section).
     */
    fun splitIntoSections(pages: List<ExtractedPage>): List<StudySection> {
        if (pages.isEmpty()) return emptyList()

        val sections = mutableListOf<StudySection>()
        var currentBuffer = StringBuilder()
        var startPage = pages.first().pageNumber
        var sectionCounter = 1

        val targetPagesPerSection = 5

        for ((index, page) in pages.withIndex()) {
            if (currentBuffer.isEmpty()) {
                startPage = page.pageNumber
            }
            currentBuffer.append(page.text).append("\n\n")

            val pagesInBuffer = page.pageNumber - startPage + 1
            val isLastPage = (index == pages.size - 1)

            // Group ~5 pages per section, or if text length exceeds 7000 characters
            if (pagesInBuffer >= targetPagesPerSection || currentBuffer.length >= 7000 || isLastPage) {
                val pageRef = if (startPage == page.pageNumber) "p. $startPage" else "pp. $startPage-${page.pageNumber}"
                sections.add(
                    StudySection(
                        sectionIndex = sectionCounter,
                        title = "Section $sectionCounter ($pageRef)",
                        content = currentBuffer.toString().trim(),
                        pageReference = pageRef
                    )
                )
                sectionCounter++
                currentBuffer = StringBuilder()
            }
        }

        if (currentBuffer.isNotBlank()) {
            val endPage = pages.lastOrNull()?.pageNumber ?: startPage
            val pageRef = if (startPage == endPage) "p. $startPage" else "pp. $startPage-$endPage"
            sections.add(
                StudySection(
                    sectionIndex = sectionCounter,
                    title = "Section $sectionCounter ($pageRef)",
                    content = currentBuffer.toString().trim(),
                    pageReference = pageRef
                )
            )
        }

        return sections
    }

    /**
     * Splits pasted plain text into teachable sections.
     */
    fun splitPastedText(rawText: String): List<StudySection> {
        val paragraphs = rawText.split(Regex("\n{2,}")).filter { it.isNotBlank() }
        val sections = mutableListOf<StudySection>()
        var currentBuffer = StringBuilder()
        var sectionCounter = 1

        for (para in paragraphs) {
            currentBuffer.append(para).append("\n\n")
            if (currentBuffer.length >= 1500) {
                sections.add(
                    StudySection(
                        sectionIndex = sectionCounter,
                        title = "Part $sectionCounter",
                        content = currentBuffer.toString().trim(),
                        pageReference = "Notes Pt. $sectionCounter"
                    )
                )
                sectionCounter++
                currentBuffer = StringBuilder()
            }
        }

        if (currentBuffer.isNotBlank()) {
            sections.add(
                StudySection(
                    sectionIndex = sectionCounter,
                    title = "Part $sectionCounter",
                    content = currentBuffer.toString().trim(),
                    pageReference = "Notes Pt. $sectionCounter"
                )
            )
        }

        if (sections.isEmpty() && rawText.isNotBlank()) {
            sections.add(
                StudySection(
                    sectionIndex = 1,
                    title = "Part 1",
                    content = rawText.trim(),
                    pageReference = "Notes Pt. 1"
                )
            )
        }

        return sections
    }
}
