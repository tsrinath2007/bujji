package com.pupil.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.data.local.entity.CreatureEntity
import com.pupil.app.data.local.entity.SubjectEntity
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.InferenceMetrics
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyRepository(application)

    val creature: StateFlow<CreatureEntity> = repository.getCreatureFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CreatureEntity(1, 0, 1, 1, 1, System.currentTimeMillis())
        )

    val latestSubject: StateFlow<SubjectEntity?> = repository.getLatestSubjectFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSubjects: StateFlow<List<SubjectEntity>> = repository.getAllSubjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Concepts for the active (most recently studied) subject */
    val subjectConcepts: StateFlow<List<Concept>> =
        latestSubject
            .flatMapLatest { subject ->
                if (subject != null) repository.getConceptsForSubject(subject.id)
                else flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConcepts: StateFlow<List<Concept>> = subjectConcepts

    val dueConcepts: StateFlow<List<Concept>> =
        subjectConcepts.map { concepts ->
            val now = System.currentTimeMillis()
            concepts.filter {
                it.status == com.pupil.app.data.model.GradingStatus.UNDERSTOOD &&
                it.nextDue > 0 &&
                it.nextDue <= now
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastMetrics: StateFlow<InferenceMetrics?> = repository.lastMetrics

    val isMockActive: Boolean get() = repository.isMockActive
    val modelCheckedPaths: List<String> get() = repository.getModelFileInfo().checkedPaths

    val simulatedDays: StateFlow<Int> = com.pupil.app.core.revision.SpacedRevisionManager.simulatedDaysOffset

    fun simulateDaysLater(days: Int) {
        com.pupil.app.core.revision.SpacedRevisionManager.addSimulatedDays(days)
    }

    fun resetSimulation() {
        com.pupil.app.core.revision.SpacedRevisionManager.resetSimulation()
    }

    init {
        viewModelScope.launch {
            repository.getCreature()
        }
    }
}
