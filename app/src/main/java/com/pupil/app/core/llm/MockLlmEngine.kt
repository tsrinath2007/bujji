package com.pupil.app.core.llm

import android.content.Context
import com.pupil.app.data.model.InferenceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MockLlmEngine : LlmEngine {

    override val isMock: Boolean = true
    override val engineName: String = "Mock Fallback LLM (Simulated)"
    override val modelPath: String = "none (in-memory)"

    override fun isModelPresent(context: Context): Boolean = true

    override fun getModelFileInfo(context: Context): ModelFileInfo = ModelFileInfo(
        fileName = "Simulated / In-Memory",
        resolvedPath = "none (in-memory mock)",
        fileSizeBytes = 0L,
        isPresent = true
    )

    override suspend fun initialize(context: Context): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun generate(prompt: String, callType: String): Pair<String, InferenceMetrics> = withContext(Dispatchers.Default) {
        delay(150) // Small delay to simulate local processing

        val rawResponse = when {
            prompt.contains("same concept? yes or no", ignoreCase = true) -> generateMockComparison(prompt)
            prompt.contains("Extract only terms", ignoreCase = true) || prompt.contains("Extract 3 to 6", ignoreCase = true) || prompt.contains("STUDY TEXT:") -> generateGroundedMockExtraction(prompt)
            prompt.contains("The student disputed their grade") -> generateMockReGrade(prompt)
            prompt.contains("evaluating a student's conceptual grasp") -> generateMockGrading(prompt)
            else -> generateGenericFallback()
        }

        val metrics = InferenceMetrics(
            latencyMs = 0L,
            memoryUsageMb = 0.0,
            isMock = true,
            callType = callType
        )
        Pair(rawResponse, metrics)
    }

    /**
     * Grounded extraction for Mock mode:
     * NEVER invents concepts. Extracts ONLY terms that appear verbatim in the source text.
     * Every extracted concept is labelled as demo data.
     * Returns {"concepts":[]} if the text defines or explains nothing.
     */
    private fun generateGroundedMockExtraction(prompt: String): String {
        val studyText = prompt.substringAfter("STUDY TEXT:").trim()
        if (studyText.isBlank()) {
            return """{"concepts":[]}"""
        }

        // Check if slide/chunk is an exercise or non-conceptual content
        val lowerText = studyText.lowercase()
        val isExerciseOrHomework = lowerText.contains("draw the") ||
                lowerText.contains("find the number of") ||
                lowerText.contains("homework") ||
                lowerText.contains("exercise") ||
                lowerText.contains("tutorial question") ||
                lowerText.contains("practice problem")
        if (isExerciseOrHomework && !lowerText.contains("is defined as") && !lowerText.contains("refers to")) {
            return """{"concepts":[]}"""
        }

        val lines = studyText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val foundConcepts = mutableListOf<Triple<String, String, List<String>>>()

        val genericEndings = setOf("dynamics", "interaction", "fundamentals", "principles", "overview", "concepts")
        val invalidFirstWords = setOf(
            "and", "or", "but", "because", "so", "that", "which", "where", "when",
            "is", "are", "was", "were", "to", "in", "on", "at", "for", "with", "as", "by", "of", "the", "a", "an"
        )

        // Search for definition patterns or bullet terms in text
        val sentences = studyText.split(Regex("[.!?\n]+")).map { it.trim() }.filter { it.length > 5 }

        for (sentence in sentences) {
            if (foundConcepts.size >= 6) break

            // Pattern: "<Term> is / refers to / represents / means <definition>"
            val definitionRegex = Regex("""(?i)\b([A-Z][a-zA-Z0-9\s\-/()]{2,40}?)\s+(?:is\s+(?:a|an|the|defined\s+as)?|refers\s+to|means|represents)\s+([^,.;]{10,120})""")
            val match = definitionRegex.find(sentence)
            if (match != null) {
                var candidateName = match.groupValues[1].trim()
                candidateName = candidateName.replace(Regex("^[•*\\-–—#0-9.\\s]+"), "").trim()
                val candidateDef = match.groupValues[2].trim()

                if (isValidVerbatimName(candidateName, studyText, genericEndings, invalidFirstWords)) {
                    val keyIdeas = extractKeyIdeas(candidateDef)
                    foundConcepts.add(Triple(candidateName, "$candidateName is $candidateDef.", keyIdeas))
                    continue
                }
            }

            // Pattern: bulleted or capitalized heading-style line
            val words = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size in 2..6 && sentence.length in 5..60) {
                val cleanLine = sentence.replace(Regex("^[•*\\-–—#0-9.\\s]+"), "").trim()
                if (cleanLine.firstOrNull()?.isUpperCase() == true && isValidVerbatimName(cleanLine, studyText, genericEndings, invalidFirstWords)) {
                    val keyIdeas = words.map { it.replace(Regex("[^a-zA-Z0-9]"), "") }.filter { it.length > 3 }.take(3)
                    foundConcepts.add(Triple(cleanLine, "$cleanLine as discussed in the text.", keyIdeas))
                }
            }
        }

        if (foundConcepts.isEmpty()) {
            return """{"concepts":[]}"""
        }

        // Deduplicate against each other within this chunk
        val uniqueFound = foundConcepts.distinctBy { it.first.lowercase() }.take(6)

        val jsonConcepts = uniqueFound.mapIndexed { idx, (rawName, meaning, keyIdeas) ->
            val links = if (idx > 0) "[\"c${idx}\"]" else "[]"
            """
            {
              "id": "c${idx + 1}",
              "name": "$rawName",
              "meaning": "${meaning.replace("\"", "\\\"")}",
              "key_ideas": ${keyIdeas.joinToString(prefix = "[\"", separator = "\", \"", postfix = "\"]")},
              "source": "Study Material",
              "links": $links
            }
            """.trimIndent()
        }

        return """
        {
          "concepts": [
            ${jsonConcepts.joinToString(",\n            ")}
          ]
        }
        """.trimIndent()
    }

    private fun isValidVerbatimName(
        name: String,
        fullText: String,
        genericEndings: Set<String>,
        invalidFirstWords: Set<String>
    ): Boolean {
        if (name.length < 3 || name.length > 60) return false
        val words = name.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size !in 2..6) return false
        if (words.first().first().isLowerCase()) return false
        if (invalidFirstWords.contains(words.first().lowercase())) return false

        val lastWord = words.last().lowercase().replace(Regex("[^a-z]"), "")
        if (genericEndings.contains(lastWord)) return false

        // MUST appear verbatim in the source text
        return fullText.contains(name, ignoreCase = true)
    }

    private fun extractKeyIdeas(text: String): List<String> {
        val stopWords = setOf("the", "and", "that", "this", "with", "from", "which", "then", "into", "over", "such", "each", "have", "more", "also", "when", "some")
        val words = text.split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-zA-Z0-9]"), "").trim() }
            .filter { it.length > 3 && !stopWords.contains(it.lowercase()) }
            .distinct()
            .take(3)
        return if (words.isNotEmpty()) words else listOf("Key concept detail")
    }

    /**
     * Resolves ambiguous concept pairs: "same concept? yes or no"
     */
    private fun generateMockComparison(prompt: String): String {
        val lines = prompt.lines()
        val c1Line = lines.firstOrNull { it.contains("Concept 1:", ignoreCase = true) } ?: ""
        val c2Line = lines.firstOrNull { it.contains("Concept 2:", ignoreCase = true) } ?: ""

        val c1Clean = c1Line.lowercase().replace(Regex("[^a-z0-9\\s]"), "")
        val c2Clean = c2Line.lowercase().replace(Regex("[^a-z0-9\\s]"), "")

        val words1 = c1Clean.split(Regex("\\s+")).filter { it.length > 3 }.toSet()
        val words2 = c2Clean.split(Regex("\\s+")).filter { it.length > 3 }.toSet()

        val overlap = words1.intersect(words2)
        return if (overlap.isNotEmpty()) "yes" else "no"
    }

    private fun generateMockGrading(prompt: String): String {
        val studentExplanation = prompt.substringAfter("STUDENT EXPLANATION:\n\"").substringBefore("\"")
        val conceptName = prompt.substringAfter("Name: ").substringBefore("\n").removePrefix("[Demo] ").trim()

        val explanationLower = studentExplanation.lowercase().trim()
        val words = studentExplanation.split(Regex("\\s+")).filter { it.isNotBlank() }

        val conceptWords = conceptName.lowercase().split(Regex("\\s+")).filter { it.length > 3 }
        val matchesConcept = conceptWords.any { explanationLower.contains(it) }

        if (explanationLower.isBlank() || words.size < 3) {
            return """
            {
              "status": "missed",
              "evidence": "No supporting quote found in explanation.",
              "followup_question": "Can you explain what $conceptName does?"
            }
            """.trimIndent()
        }

        val quote = studentExplanation.take(50).replace("\"", "")

        return if (matchesConcept) {
            """
            {
              "status": "understood",
              "evidence": "\"$quote...\"",
              "followup_question": "How does $conceptName apply in practice?"
            }
            """.trimIndent()
        } else {
            """
            {
              "status": "partial",
              "evidence": "\"$quote...\"",
              "followup_question": "Can you elaborate further on $conceptName?"
            }
            """.trimIndent()
        }
    }

    private fun generateMockReGrade(prompt: String): String {
        val studentExplanation = prompt.substringAfter("STUDENT EXPLANATION:\n\"").substringBefore("\"")
        val quote = studentExplanation.take(50).replace("\"", "")
        return """
        {
          "status": "understood",
          "evidence": "\"$quote...\"",
          "followup_question": "Can you think of another example for this concept?"
        }
        """.trimIndent()
    }

    private fun generateGenericFallback(): String = """
        {
          "concepts": []
        }
    """.trimIndent()

    override fun close() {}
}
