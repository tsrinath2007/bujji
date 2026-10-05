package com.pupil.app.ui.screens.import_material

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.pdf.PdfTextExtractor
import com.pupil.app.data.model.InferenceMetrics
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportUiState {
    object Idle : ImportUiState()
    data class Processing(val progress: String) : ImportUiState()
    /** subjectId (String UUID) is ready — navigate to that subject's topic picker */
    data class Success(val subjectId: String) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepository(application)
    private val pdfExtractor: PdfTextExtractor = PdfTextExtractor(application)
    private val ocrManager: com.pupil.app.core.ocr.OcrManager = com.pupil.app.core.ocr.OcrManager(application)

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    val isMockActive: Boolean get() = repository.isMockActive
    val lastMetrics: StateFlow<InferenceMetrics?> = repository.lastMetrics

    init {
        viewModelScope.launch {
            repository.setupEngine()
        }
    }

    fun recognizePhotos(uris: List<Uri>, onCompleted: (text: String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Processing("Running on-device OCR on photos...")
            val result = ocrManager.recognizeTextFromUris(uris) { current, total ->
                _uiState.value = ImportUiState.Processing("Recognizing text from photo $current of $total...")
            }
            result.onSuccess { recognizedText ->
                _uiState.value = ImportUiState.Idle
                onCompleted(recognizedText)
            }.onFailure { error ->
                _uiState.value = ImportUiState.Error(error.localizedMessage ?: "OCR failed to read notes.")
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val context = getApplication<Application>()
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ImportViewModel", "Could not query display name from uri", e)
            }
        }
        if (result.isNullOrBlank()) result = uri.lastPathSegment
        return result?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "Study Document"
    }

    fun processPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Processing("Extracting text from PDF...")
            try {
                val pages = pdfExtractor.extractPages(uri)
                if (pages.isEmpty() || pages.all { it.text.isBlank() }) {
                    _uiState.value = ImportUiState.Error("No readable text found in this PDF file.")
                    return@launch
                }

                val displayName = getFileNameFromUri(uri)
                val sections = pdfExtractor.splitIntoSections(pages)

                val subject = repository.createSubject(
                    name = displayName,
                    emoji = "📄",
                    colourHex = "#5B4DFF"
                )

                val source = repository.addSource(
                    subjectId = subject.id,
                    type = "PDF",
                    displayName = displayName,
                    filePath = uri.toString()
                )

                _uiState.value = ImportUiState.Processing("Extracting concepts from $displayName...")
                repository.extractAndSaveConceptsForSource(
                    subjectId = subject.id,
                    sourceId = source.id,
                    sections = sections,
                    onProgress = { current, total, status ->
                        _uiState.value = ImportUiState.Processing("$status ($current/$total)")
                    }
                )

                repository.markSourceDone(source.id, pages.size)
                _uiState.value = ImportUiState.Success(subject.id)
            } catch (e: Exception) {
                Log.e("ImportViewModel", "processPdf failed", e)
                _uiState.value = ImportUiState.Error("Failed to extract PDF: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun processPastedText(text: String, title: String = "Pasted Notes") {
        if (text.isBlank()) {
            _uiState.value = ImportUiState.Error("Please enter some text to study.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ImportUiState.Processing("Chunking text into study sections...")
            try {
                val sections = pdfExtractor.splitPastedText(text)

                val subject = repository.createSubject(
                    name = title,
                    emoji = "📝",
                    colourHex = "#FF9F1C"
                )

                val source = repository.addSource(
                    subjectId = subject.id,
                    type = "TEXT",
                    displayName = title,
                    filePath = null
                )

                val count = repository.extractAndSaveConceptsForSource(
                    subjectId = subject.id,
                    sourceId = source.id,
                    sections = sections,
                    onProgress = { current, total, status ->
                        _uiState.value = ImportUiState.Processing("$status ($current/$total)")
                    }
                )

                repository.markSourceDone(source.id, 1)
                _uiState.value = ImportUiState.Success(subject.id)
            } catch (e: Exception) {
                Log.e("ImportViewModel", "processPastedText failed", e)
                _uiState.value = ImportUiState.Error("Failed to extract concepts: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun resetState() {
        _uiState.value = ImportUiState.Idle
    }
}
