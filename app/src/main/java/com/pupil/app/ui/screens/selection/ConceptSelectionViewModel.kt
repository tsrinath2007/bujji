package com.pupil.app.ui.screens.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.data.local.entity.SourceEntity
import com.pupil.app.data.local.entity.SubjectEntity
import com.pupil.app.data.local.entity.TopicEntity
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.InferenceMetrics
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * TopicSelectionViewModel / SubjectViewModel —
 * Manages Subject information, Sources list, Topics list, and Brain Map navigation.
 */
class TopicSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepository(application)

    val isMockActive: Boolean get() = repository.isMockActive
    val lastMetrics: StateFlow<InferenceMetrics?> = repository.lastMetrics

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun getSubject(subjectId: String): kotlinx.coroutines.flow.Flow<SubjectEntity?> {
        return repository.getSubjectFlow(subjectId)
    }

    fun getSourcesFlow(subjectId: String): kotlinx.coroutines.flow.Flow<List<SourceEntity>> {
        return repository.getSourcesForSubject(subjectId)
    }

    /** Topics for the current subject */
    fun getTopicsFlow(subjectId: String): kotlinx.coroutines.flow.Flow<List<TopicEntity>> {
        return repository.getTopicsForSubject(subjectId)
    }

    /** Concepts for a specific topic */
    fun getConceptsForTopic(topicId: String): kotlinx.coroutines.flow.Flow<List<Concept>> {
        return repository.getConceptsForTopic(topicId)
    }

    /** All concepts for subject (for "Teach all" mode) */
    fun getAllConceptsForSubject(subjectId: String): kotlinx.coroutines.flow.Flow<List<Concept>> {
        return repository.getConceptsForSubject(subjectId)
    }

    /** Gap concepts (missed, misconception, partial) for subject */
    fun loadGapConcepts(subjectId: String, onResult: (List<Concept>) -> Unit) {
        viewModelScope.launch {
            val gaps = repository.getGapConcepts(subjectId)
            onResult(gaps)
        }
    }

    /** Create a new text or study source */
    fun createSource(subjectId: String, name: String, textContent: String = "") {
        viewModelScope.launch {
            val source = repository.addSource(
                subjectId = subjectId,
                type = "TEXT",
                displayName = name,
                filePath = null
            )
            if (textContent.isNotBlank()) {
                val extractor = com.pupil.app.core.pdf.PdfTextExtractor(getApplication())
                val sections = extractor.splitPastedText(textContent)
                repository.extractAndSaveConceptsForSource(
                    subjectId = subjectId,
                    sourceId = source.id,
                    sections = sections,
                    onProgress = { _, _, _ -> }
                )
            }
            repository.markSourceDone(source.id, 1)
        }
    }

    /** Delete a source and associated concepts */
    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            repository.deleteSource(sourceId)
        }
    }
}

// Keep old name as a typealias so all references compile seamlessly
typealias ConceptSelectionViewModel = TopicSelectionViewModel
