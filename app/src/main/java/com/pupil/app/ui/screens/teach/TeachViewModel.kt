package com.pupil.app.ui.screens.teach

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.speech.SpeechState
import com.pupil.app.core.speech.SpeechToTextManager
import com.pupil.app.data.local.entity.CreatureEntity
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingStatus
import com.pupil.app.data.model.InferenceMetrics
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TeachUiState {
    object Idle : TeachUiState()
    data class Grading(val currentConceptIndex: Int, val totalConcepts: Int, val conceptName: String) : TeachUiState()
    data class Completed(val subjectId: String, val topicId: String?) : TeachUiState()
    data class Error(val message: String) : TeachUiState()
}

class TeachViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepository(application)

    private val speechManager = SpeechToTextManager(application)
    val speechState: StateFlow<SpeechState> = speechManager.state

    private val _uiState = MutableStateFlow<TeachUiState>(TeachUiState.Idle)
    val uiState: StateFlow<TeachUiState> = _uiState.asStateFlow()

    private val _explanationText = MutableStateFlow("")
    val explanationText: StateFlow<String> = _explanationText.asStateFlow()

    val isMockActive: Boolean get() = repository.isMockActive
    val lastMetrics: StateFlow<InferenceMetrics?> = repository.lastMetrics

    val creature: StateFlow<CreatureEntity> = repository.getCreatureFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CreatureEntity(1, 0, 1, 1, 1, System.currentTimeMillis())
        )

    init {
        viewModelScope.launch {
            speechManager.state.collect { state ->
                when (state) {
                    is SpeechState.PartialResult -> {
                        if (state.text.isNotBlank()) {
                            _explanationText.value = state.text
                        }
                    }
                    is SpeechState.FinalResult -> {
                        if (state.text.isNotBlank()) {
                            _explanationText.value = state.text
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /** Get concepts for topic, or gap concepts for subject if topicId is null / gaps-only mode (capped at 8 concepts) */
    fun getConceptsFlow(topicId: String?, subjectId: String, onlyGaps: Boolean = false): kotlinx.coroutines.flow.Flow<List<Concept>> {
        val rawFlow = if (onlyGaps) {
            kotlinx.coroutines.flow.flow {
                val gaps = repository.getGapConcepts(subjectId)
                emit(gaps)
            }
        } else if (topicId != null) {
            repository.getConceptsForTopic(topicId)
        } else {
            repository.getConceptsForSubject(subjectId)
        }

        return rawFlow.map { it.take(8) }
    }

    fun updateExplanationText(text: String) {
        _explanationText.value = text
    }

    fun startListening() {
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun submitExplanation(
        topicId: String?,
        subjectId: String,
        concepts: List<Concept>
    ) {
        val explanation = _explanationText.value.trim()
        if (explanation.isBlank()) {
            _uiState.value = TeachUiState.Error("Please speak or type your explanation before submitting.")
            return
        }

        val conceptsToGrade = concepts.take(8)
        if (conceptsToGrade.isEmpty()) {
            _uiState.value = TeachUiState.Error("No concepts to grade. Add a source first.")
            return
        }

        viewModelScope.launch {
            try {
                for ((index, concept) in conceptsToGrade.withIndex()) {
                    _uiState.value = TeachUiState.Grading(
                        currentConceptIndex = index + 1,
                        totalConcepts = conceptsToGrade.size,
                        conceptName = concept.name
                    )

                    repository.gradeConcept(
                        concept = concept,
                        studentExplanation = explanation,
                        subjectId = subjectId
                    )
                }

                _uiState.value = TeachUiState.Completed(subjectId = subjectId, topicId = topicId)
            } catch (e: Exception) {
                _uiState.value = TeachUiState.Error("Grading encountered an error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun resetState() {
        _uiState.value = TeachUiState.Idle
    }

    fun destroySpeech() {
        speechManager.destroy()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
