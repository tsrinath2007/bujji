package com.pupil.app.ui.screens.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.data.local.entity.CreatureEntity
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingResult
import com.pupil.app.data.model.InferenceMetrics
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ResultViewModel — shows summary after a teaching conversation.
 * Supports progressive grading: show summary immediately, fill in each concept result as its call finishes.
 */
class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepository(application)

    // Map of conceptId -> grading result (fills in progressively)
    private val _gradingResults = MutableStateFlow<Map<String, GradingResult>>(emptyMap())
    val gradingResults: StateFlow<Map<String, GradingResult>> = _gradingResults.asStateFlow()

    // Track which concept is currently being graded (shows spinner on its card)
    private val _gradingConceptId = MutableStateFlow<String?>(null)
    val gradingConceptId: StateFlow<String?> = _gradingConceptId.asStateFlow()

    private val _isGradingComplete = MutableStateFlow(false)
    val isGradingComplete: StateFlow<Boolean> = _isGradingComplete.asStateFlow()

    val isMockActive: Boolean get() = repository.isMockActive
    val lastMetrics: StateFlow<InferenceMetrics?> = repository.lastMetrics

    val creature: StateFlow<CreatureEntity> = repository.getCreatureFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CreatureEntity(1, 0, 1, 1, 1, System.currentTimeMillis())
        )

    /**
     * Returns live concept list for a topic (or subject if topicId is null).
     * Used to show all concept cards immediately while grading runs.
     */
    fun getConceptsForTopicFlow(topicId: String): kotlinx.coroutines.flow.Flow<List<Concept>> {
        return repository.getConceptsForTopic(topicId)
    }

    fun getConceptsForSubjectFlow(subjectId: String): kotlinx.coroutines.flow.Flow<List<Concept>> {
        return repository.getConceptsForSubject(subjectId)
    }

    /**
     * Progressive grading: grades each concept sequentially, emitting results as each finishes.
     * The UI shows cards immediately and fills in results one by one.
     * Per-call latency is logged inside StudyRepository.gradeConcept.
     */
    fun gradeConceptsProgressively(
        concepts: List<Concept>,
        fullTranscript: String,
        subjectId: String
    ) {
        viewModelScope.launch {
            _isGradingComplete.value = false
            val resultsMap = mutableMapOf<String, GradingResult>()

            for (concept in concepts) {
                _gradingConceptId.value = concept.id
                try {
                    val result = repository.gradeConcept(
                        concept = concept,
                        studentExplanation = fullTranscript,
                        subjectId = subjectId
                    )
                    resultsMap[concept.id] = result
                    _gradingResults.value = resultsMap.toMap()
                } catch (e: Exception) {
                    // On error, mark as missed with an error message
                    resultsMap[concept.id] = GradingResult(
                        status = com.pupil.app.data.model.GradingStatus.MISSED,
                        evidence = "Grading failed: ${e.message}",
                        followupQuestion = "",
                        metrics = null
                    )
                    _gradingResults.value = resultsMap.toMap()
                } finally {
                    _gradingConceptId.value = null
                }
            }
            _isGradingComplete.value = true
        }
    }

    fun regradeConcept(concept: Concept, subjectId: String) {
        viewModelScope.launch {
            _gradingConceptId.value = concept.id
            try {
                val result = repository.regradeConcept(concept, subjectId)
                _gradingResults.value = _gradingResults.value + (concept.id to result)
            } finally {
                _gradingConceptId.value = null
            }
        }
    }
}
