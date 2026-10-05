package com.pupil.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConceptModelTest {

    @Test
    fun testNormalizeConceptName() {
        assertEquals("photosynthesis", Concept.normalizeConceptName("  Photosynthesis  "))
        assertEquals("calvin cycle", Concept.normalizeConceptName("1. Calvin Cycle"))
        assertEquals("glycolysis", Concept.normalizeConceptName("• Glycolysis"))
        assertEquals("atp synthase", Concept.normalizeConceptName("- ATP Synthase!"))
        assertEquals("electron transport chain", Concept.normalizeConceptName("Electron   Transport   Chain"))
    }

    @Test
    fun testGradingStatusParsing() {
        assertEquals(GradingStatus.UNDERSTOOD, GradingStatus.fromString("understood"))
        assertEquals(GradingStatus.UNDERSTOOD, GradingStatus.fromString("UNDERSTOOD"))
        assertEquals(GradingStatus.PARTIAL, GradingStatus.fromString("partial"))
        assertEquals(GradingStatus.MISCONCEPTION, GradingStatus.fromString("misconception"))
        assertEquals(GradingStatus.UNSTUDIED, GradingStatus.fromString("unstudied"))
        assertEquals(GradingStatus.MISSED, GradingStatus.fromString("unknown_status"))
        assertEquals(GradingStatus.MISSED, GradingStatus.fromString(""))
    }
}
