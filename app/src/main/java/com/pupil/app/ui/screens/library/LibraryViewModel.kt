package com.pupil.app.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.data.local.entity.SubjectEntity
import com.pupil.app.data.local.entity.SourceEntity
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingStatus
import com.pupil.app.data.model.InferenceMetrics
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectCoverageItem(
    val subject: SubjectEntity,
    val totalConcepts: Int,
    val understoodConcepts: Int,
    val coveragePercent: Int,
    val sources: List<SourceEntity> = emptyList(),
    val dueCount: Int = 0
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyRepository(application)

    val allSubjects: StateFlow<List<SubjectEntity>> = repository.getAllSubjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isMockActive: Boolean get() = repository.isMockActive
    val lastMetrics: StateFlow<InferenceMetrics?> = repository.lastMetrics

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun openCreateSubjectDialog() { _showCreateDialog.value = true }
    fun closeCreateSubjectDialog() { _showCreateDialog.value = false }

    fun createSubject(name: String, emoji: String, colourHex: String) {
        viewModelScope.launch {
            repository.createSubject(name, emoji, colourHex)
            _showCreateDialog.value = false
            _actionMessage.value = "\"$name\" added!"
        }
    }

    fun deleteSubject(subjectId: String, name: String = "") {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
            _actionMessage.value = if (name.isNotBlank()) "\"$name\" deleted." else "Subject deleted."
        }
    }

    fun renameSubject(subjectId: String, name: String, emoji: String, colourHex: String) {
        viewModelScope.launch {
            repository.renameSubject(subjectId, name, emoji, colourHex)
        }
    }

    fun clearActionMessage() { _actionMessage.value = null }

    /** Returns live concept list for the given subject — used to compute coverage rings */
    fun getConceptsFlow(subjectId: String): kotlinx.coroutines.flow.Flow<List<Concept>> {
        return repository.getConceptsForSubject(subjectId)
    }

    /** Returns live sources list for the given subject */
    fun getSourcesFlow(subjectId: String): kotlinx.coroutines.flow.Flow<List<SourceEntity>> {
        return repository.getSourcesForSubject(subjectId)
    }
}
