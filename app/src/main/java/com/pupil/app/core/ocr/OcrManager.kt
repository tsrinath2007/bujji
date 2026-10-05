package com.pupil.app.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OcrManager(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognizes text from a list of photo Uris off the main thread,
     * downscaling large camera images to prevent OOM errors.
     */
    suspend fun recognizeTextFromUris(
        uris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val total = uris.size
            if (total == 0) {
                return@withContext Result.failure(IllegalArgumentException("No photos selected for OCR."))
            }

            val textBuilder = StringBuilder()

            for ((index, uri) in uris.withIndex()) {
                onProgress(index + 1, total)

                val bitmap = decodeSampledBitmapFromUri(context, uri, 1600, 1600)
                    ?: continue

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = Tasks.await(recognizer.process(inputImage))
                bitmap.recycle() // Free native memory immediately

                val pageText = visionText.text.trim()
                if (pageText.isNotBlank()) {
                    if (textBuilder.isNotEmpty()) {
                        textBuilder.append("\n\n--- Photo ${index + 1} ---\n\n")
                    } else if (total > 1) {
                        textBuilder.append("--- Photo 1 ---\n\n")
                    }
                    textBuilder.append(pageText)
                }
            }

            val resultString = textBuilder.toString().trim()
            if (resultString.isBlank()) {
                Result.failure(Exception("No legible text could be recognized from the photos. Please ensure good lighting and clear text."))
            } else {
                Result.success(resultString)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height: Int = options.outHeight
        val width: Int = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
