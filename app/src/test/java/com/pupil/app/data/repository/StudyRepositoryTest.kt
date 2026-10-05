package com.pupil.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyRepositoryTest {

    @Test
    fun testConceptStatsCalculation() {
        val stats = ConceptStats(
            totalConcepts = 10,
            understoodCount = 7,
            partialCount = 2,
            missedCount = 1,
            unstudiedCount = 0
        )

        assertEquals(10, stats.totalConcepts)
        assertEquals(7, stats.understoodCount)
        assertEquals(0.7f, stats.coverageFraction, 0.001f)
    }

    @Test
    fun testConceptStatsZeroConcepts() {
        val stats = ConceptStats(
            totalConcepts = 0,
            understoodCount = 0,
            partialCount = 0,
            missedCount = 0,
            unstudiedCount = 0
        )

        assertEquals(0f, stats.coverageFraction, 0.001f)
    }

    @Test
    fun testConceptStatsAllUnderstood() {
        val stats = ConceptStats(
            totalConcepts = 15,
            understoodCount = 15,
            partialCount = 0,
            missedCount = 0,
            unstudiedCount = 0
        )

        assertEquals(1.0f, stats.coverageFraction, 0.001f)
    }

    @Test
    fun testConceptStatsPartialCoverage() {
        val stats = ConceptStats(
            totalConcepts = 8,
            understoodCount = 4,
            partialCount = 2,
            missedCount = 2,
            unstudiedCount = 0
        )

        assertEquals(0.5f, stats.coverageFraction, 0.001f)
    }
}
