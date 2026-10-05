package com.pupil.app.core.concept

import com.pupil.app.data.model.Concept
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConceptProcessorTest {

    @Test
    fun testEightCopiesCollapseToOne() = runBlocking {
        val copies = (1..8).map { i ->
            Concept(
                id = "c_$i",
                name = "Algorithmic Time Complexity",
                meaning = "Runtime growth rate as input scales.",
                keyIdeas = listOf("Big O $i"),
                source = "Page $i"
            )
        }

        val deduplicated = ConceptProcessor.deduplicateConcepts(copies)

        assertEquals("8 copies of the same name must collapse to exactly 1", 1, deduplicated.size)
        assertEquals("Algorithmic Time Complexity", deduplicated.first().name)
        assertEquals("All sources should be combined", 8, deduplicated.first().sources.size)
        assertEquals("All key ideas should be collected", 8, deduplicated.first().keyIdeas.size)
    }

    @Test
    fun testManchesterEncodingMergesWithManchesterBiphaseEncoding() = runBlocking {
        val c1 = Concept(
            id = "c1",
            name = "Manchester Encoding",
            meaning = "Digital encoding with mid-bit transition.",
            keyIdeas = listOf("Mid-bit transition", "Self-clocking"),
            source = "Slide 14"
        )
        val c2 = Concept(
            id = "c2",
            name = "Manchester (Biphase) Encoding",
            meaning = "Biphase signaling where each bit interval has a transition in the middle.",
            keyIdeas = listOf("Biphase signaling", "Clock recovery"),
            source = "Slide 18"
        )

        val sim = ConceptProcessor.calculateSimilarity(c1.name, c2.name)
        assertTrue("Similarity between Manchester Encoding and Manchester (Biphase) Encoding should be >= 0.85 (was $sim)", sim >= 0.85)

        val merged = ConceptProcessor.deduplicateConcepts(listOf(c1, c2))
        assertEquals("Manchester Encoding and Manchester (Biphase) Encoding must merge into 1", 1, merged.size)
        assertTrue("Key ideas must be combined", merged.first().keyIdeas.contains("Mid-bit transition"))
        assertTrue("Key ideas must be combined", merged.first().keyIdeas.contains("Clock recovery"))
        assertTrue("Sources must be combined", merged.first().sources.contains("Slide 14"))
        assertTrue("Sources must be combined", merged.first().sources.contains("Slide 18"))
    }

    @Test
    fun testGenericEndingsAreRejected() {
        assertFalse("Dynamics ending must be rejected", ConceptProcessor.isValidConceptName("Component Interaction Dynamics"))
        assertFalse("Interaction ending must be rejected", ConceptProcessor.isValidConceptName("Cellular Signal Interaction"))
        assertFalse("Fundamentals ending must be rejected", ConceptProcessor.isValidConceptName("Network Routing Fundamentals"))
        assertFalse("Principles ending must be rejected", ConceptProcessor.isValidConceptName("Foundational System Principles"))
        assertFalse("Overview ending must be rejected", ConceptProcessor.isValidConceptName("Protocol Stack Overview"))
        assertFalse("Concepts ending must be rejected", ConceptProcessor.isValidConceptName("Core Operating Concepts"))

        assertTrue("Valid domain term should pass", ConceptProcessor.isValidConceptName("Fourier Frequency Analysis"))
        assertTrue("Valid domain term should pass", ConceptProcessor.isValidConceptName("Differential Manchester Encoding"))
        assertTrue("Valid domain term should pass", ConceptProcessor.isValidConceptName("Peak Signal Amplitude"))
    }

    @Test
    fun testNameValidationRules() {
        assertFalse("Too short (< 2 words) must be rejected", ConceptProcessor.isValidConceptName("Simplex"))
        assertFalse("Too long (> 6 words) must be rejected", ConceptProcessor.isValidConceptName("This Is A Very Long Name That Exceeds Six Words"))
        assertFalse("Starting lowercase must be rejected", ConceptProcessor.isValidConceptName("frequency modulation"))
        assertFalse("Starting with invalid conjunction must be rejected", ConceptProcessor.isValidConceptName("Because Waveform Attenuation Occurs"))
        assertFalse("Ending with period must be rejected", ConceptProcessor.isValidConceptName("Bit Rate Interval."))

        // Strip leading bullets before validation
        val sanitized = ConceptProcessor.sanitizeConceptName("• Synchronous Time Division Multiplexing")
        assertEquals("Synchronous Time Division Multiplexing", sanitized)
        assertTrue(ConceptProcessor.isValidConceptName(sanitized))
    }

    @Test
    fun testGroundednessCheck() {
        val slideText = """
            Data Communication and Computer Networks.
            In simplex mode, communication is unidirectional.
            Half-duplex allows transmission in both directions, but not simultaneously.
            Full-duplex allows simultaneous bidirectional transmission.
        """.trimIndent()

        assertTrue(ConceptProcessor.isGroundedInSource("Simplex Communication Mode", slideText))
        assertTrue(ConceptProcessor.isGroundedInSource("Full Duplex Transmission", slideText))
        assertFalse("Ungrounded algorithmic concept must be dropped", ConceptProcessor.isGroundedInSource("Algorithmic Time Complexity", slideText))
        assertFalse("Ungrounded memory concept must be dropped", ConceptProcessor.isGroundedInSource("Dynamic Memory Allocation", slideText))
    }

    @Test
    fun testTopicClusteringSplitsWhenOverEight() {
        val concepts = (1..15).map {
            Concept(id = "c$it", name = "Concept Number $it", meaning = "Meaning $it")
        }

        val topics = ConceptProcessor.clusterIntoTopics(concepts, "Signal Transmission")
        assertTrue("More than 8 concepts should be split into multiple parts", topics.size > 1)
        for ((name, topicConcepts) in topics) {
            assertTrue("Topic $name must have at most 8 concepts (has ${topicConcepts.size})", topicConcepts.size <= 8)
            assertTrue("Topic $name must have at least 1 concept", topicConcepts.isNotEmpty())
        }
    }
}
