package com.pupil.app.core.revision

import com.pupil.app.core.config.AppConfig
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SpacedRevisionManager {

    // Debug-only simulated days offset for instant demoing
    private val _simulatedDaysOffset = MutableStateFlow(0)
    val simulatedDaysOffset: StateFlow<Int> = _simulatedDaysOffset.asStateFlow()

    fun setSimulatedDays(days: Int) {
        _simulatedDaysOffset.value = days
    }

    fun addSimulatedDays(days: Int) {
        _simulatedDaysOffset.value += days
    }

    fun resetSimulation() {
        _simulatedDaysOffset.value = 0
    }

    /**
     * Checks if an understood concept is due for spaced revision.
     */
    fun isConceptDue(concept: Concept, simulatedDays: Int = _simulatedDaysOffset.value): Boolean {
        if (concept.status != GradingStatus.UNDERSTOOD || concept.lastTaught <= 0L) {
            return false
        }

        val intervalDays = AppConfig.REVISION_INTERVALS_DAYS.getOrElse(concept.revisionIntervalIndex) {
            AppConfig.REVISION_INTERVALS_DAYS.last()
        }

        val intervalMillis = (intervalDays - simulatedDays) * 24L * 60L * 60L * 1000L
        val elapsed = System.currentTimeMillis() - concept.lastTaught
        return elapsed >= intervalMillis
    }

    /**
     * Returns the next interval index when a concept is successfully re-confirmed.
     */
    fun getNextIntervalIndex(currentIndex: Int): Int {
        return (currentIndex + 1).coerceAtMost(AppConfig.REVISION_INTERVALS_DAYS.size - 1)
    }

    /**
     * KPN (Knowledge Process Network) Stages for Concepts.
     * Concepts flow through the network like tokens:
     * UNSTUDIED (Queue 0) -> IN_PROGRESS (Queue 1) -> UNDERSTOOD (Queue 2) -> DUE_REVISION (Queue 3) -> MASTERED (Queue 4)
     */
    enum class KpnStage(val displayName: String, val description: String) {
        UNSTUDIED("Unstudied", "Waiting in input buffer to be taught"),
        IN_PROGRESS("In Progress", "Currently actively being worked on with gaps"),
        UNDERSTOOD("Understood", "Grasped! Awaiting scheduled retention interval"),
        DUE_REVISION("Due for Review", "Interval elapsed, ready for spaced reinforcement"),
        MASTERED("Mastered", "Fully reinforced across 4+ revision cycles")
    }

    /**
     * Determines current KPN Stage of a concept in the network.
     */
    fun getKpnStage(concept: Concept, simulatedDays: Int = _simulatedDaysOffset.value): KpnStage {
        return when {
            concept.status == GradingStatus.UNSTUDIED -> KpnStage.UNSTUDIED
            concept.status != GradingStatus.UNDERSTOOD -> KpnStage.IN_PROGRESS
            concept.revisionIntervalIndex >= 4 -> KpnStage.MASTERED
            isConceptDue(concept, simulatedDays) -> KpnStage.DUE_REVISION
            else -> KpnStage.UNDERSTOOD
        }
    }

    /**
     * Reduced XP awarded for confirming understanding during spaced revision.
     */
    const val REDUCED_REVISION_XP = 5
}
