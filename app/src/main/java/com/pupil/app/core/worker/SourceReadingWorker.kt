package com.pupil.app.core.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pupil.app.core.ocr.OcrManager
import com.pupil.app.core.pdf.PdfTextExtractor
import com.pupil.app.data.local.PupilDatabase
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SourceReadingWorker — WorkManager background pipeline for reading study sources.
 *
 * Processes PDF documents and photo notes off the UI thread:
 * 1. Updates source processing state to READING.
 * 2. Extracts digital text or runs on-device OCR page-by-page.
 * 3. Reports fine-grained page-level progress to WorkManager and Room.
 * 4. Triggers grounded concept extraction into the subject's knowledge graph.
 * 5. Marks source as DONE or FAILED on error.
 */
class SourceReadingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SourceReadingWorker"
        const val KEY_SOURCE_ID = "key_source_id"
        const val KEY_SUBJECT_ID = "key_subject_id"
        const val KEY_SOURCE_URI = "key_source_uri"
        const val KEY_SOURCE_TYPE = "key_source_type" // "PDF", "PHOTO", "TEXT"
        const val KEY_DISPLAY_NAME = "key_display_name"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sourceId = inputData.getString(KEY_SOURCE_ID) ?: return@withContext Result.failure()
        val subjectId = inputData.getString(KEY_SUBJECT_ID) ?: return@withContext Result.failure()
        val sourceUriString = inputData.getString(KEY_SOURCE_URI)
        val sourceType = inputData.getString(KEY_SOURCE_TYPE) ?: "PDF"
        val displayName = inputData.getString(KEY_DISPLAY_NAME) ?: "Document"

        val db = PupilDatabase.getInstance(applicationContext)
        val repository = StudyRepository(applicationContext)

        try {
            Log.i(TAG, "Starting background reading for source: $sourceId ($sourceType: $displayName)")
            db.sourceDao().updateProgress(sourceId, "READING", 0.05f)
            setProgress(workDataOf("progress" to 0.05f, "status" to "Starting extraction..."))

            val extractedText: String
            var totalPages = 1

            when (sourceType.uppercase()) {
                "PDF" -> {
                    if (sourceUriString.isNullOrBlank()) {
                        throw IllegalArgumentException("PDF URI is null or blank")
                    }
                    val pdfUri = Uri.parse(sourceUriString)
                    val pdfExtractor = PdfTextExtractor(applicationContext)
                    val pages = pdfExtractor.extractPages(pdfUri)
                    totalPages = pages.size

                    db.sourceDao().updatePageCount(sourceId, totalPages)

                    val textBuilder = StringBuilder()
                    for ((index, page) in pages.withIndex()) {
                        val progress = (index + 1).toFloat() / totalPages
                        db.sourceDao().updateProgress(sourceId, "READING", progress * 0.7f) // 70% for reading
                        setProgress(
                            workDataOf(
                                "progress" to progress * 0.7f,
                                "page" to (index + 1),
                                "total" to totalPages,
                                "status" to "Reading page ${index + 1} of $totalPages..."
                            )
                        )
                        textBuilder.append("--- Page ${page.pageNumber} ---\n")
                        textBuilder.append(page.text).append("\n\n")
                    }
                    extractedText = textBuilder.toString()
                }

                "PHOTO" -> {
                    if (sourceUriString.isNullOrBlank()) {
                        throw IllegalArgumentException("Photo URI is null or blank")
                    }
                    val uri = Uri.parse(sourceUriString)
                    val ocrManager = OcrManager(applicationContext)
                    setProgress(workDataOf("progress" to 0.3f, "status" to "Running on-device OCR..."))

                    val ocrResult = ocrManager.recognizeTextFromUris(listOf(uri)) { curr, total ->
                        // Synchronous progress reporting
                        Log.d(TAG, "OCR progress: $curr of $total")
                    }

                    if (ocrResult.isFailure) {
                        throw ocrResult.exceptionOrNull() ?: Exception("OCR failed")
                    }
                    extractedText = ocrResult.getOrThrow()
                    totalPages = 1
                    db.sourceDao().updatePageCount(sourceId, 1)
                }

                else -> {
                    // Raw text or fallback
                    extractedText = inputData.getString("raw_text") ?: ""
                }
            }

            if (extractedText.isBlank()) {
                throw IllegalStateException("No text could be extracted from source: $displayName")
            }

            // Concept extraction phase (remaining 30% progress)
            db.sourceDao().updateProgress(sourceId, "READING", 0.75f)
            setProgress(workDataOf("progress" to 0.75f, "status" to "Extracting concepts..."))

            repository.extractAndSaveConceptsFromText(
                subjectId = subjectId,
                sourceId = sourceId,
                sourceName = displayName,
                text = extractedText
            )

            // Done!
            db.sourceDao().markDone(sourceId, totalPages)
            setProgress(workDataOf("progress" to 1.0f, "status" to "Complete"))

            Log.i(TAG, "Completed background reading for source: $sourceId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading source $sourceId: ${e.localizedMessage}", e)
            try {
                db.sourceDao().updateFailed(sourceId, error = e.localizedMessage ?: "Unknown error")
            } catch (dbEx: Exception) {
                Log.w(TAG, "Failed to update source state to FAILED", dbEx)
            }
            Result.failure(workDataOf("error" to (e.localizedMessage ?: "Unknown error")))
        }
    }
}
