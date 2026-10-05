package com.pupil.app.core.concept

import com.pupil.app.data.model.Concept
import java.util.UUID

object ConceptProcessor {

    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with",
        "by", "about", "against", "between", "into", "through", "during", "before",
        "after", "above", "below", "from", "up", "down", "of", "off", "over", "under",
        "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do",
        "does", "did", "this", "that", "these", "those"
    )

    // Words stripped ONLY when comparing names for deduplication
    private val COMPARISON_ONLY_STOP_WORDS = setOf(
        "encoding", "scheme", "technique", "system", "method", "protocol", "approach",
        "mechanism", "process", "model", "algorithm", "type", "mode", "operation"
    )

    // Generic concept endings that MUST be dropped
    private val FORBIDDEN_ENDINGS = setOf(
        "dynamics", "interaction", "fundamentals", "principles", "overview", "concepts"
    )

    private val INVALID_FIRST_WORDS = setOf(
        "and", "or", "but", "because", "so", "that", "which", "where", "when",
        "is", "are", "was", "were", "to", "in", "on", "at", "for", "with", "as",
        "by", "of", "the", "a", "an", "this", "these", "those", "how", "what", "why"
    )

    /**
     * Sanitizes a concept name by stripping leading bullets, numbers, markdown symbols, and surrounding quotes.
     */
    fun sanitizeConceptName(rawName: String): String {
        var name = rawName.trim()
        name = name.replace(Regex("^[\\s•*\\-–—#0-9.\\)]+"), "").trim()
        name = name.removeSurrounding("\"").removeSurrounding("'").trim()
        name = name.replace(Regex("\\s+"), " ")
        return name
    }

    /**
     * Validates if a concept name adheres to strict naming rules:
     * - 2 to 6 words
     * - Length between 3 and 60 characters
     * - Does NOT start with lowercase or mid-sentence fragments
     * - Does NOT end in generic non-concepts (Dynamics, Interaction, Fundamentals, Principles, Overview, Concepts)
     * - Does NOT end with sentence punctuation (., !, ?)
     */
    fun isValidConceptName(name: String): Boolean {
        val clean = sanitizeConceptName(name)
        if (clean.length < 3 || clean.length > 60) return false

        val words = clean.split(" ").filter { it.isNotBlank() }
        if (words.size !in 2..6) return false

        val firstWord = words.first()
        if (firstWord.first().isLowerCase()) return false
        if (INVALID_FIRST_WORDS.contains(firstWord.lowercase())) return false

        if (clean.endsWith(".") || clean.endsWith("!") || clean.endsWith("?")) return false

        val lastWord = words.last().lowercase().replace(Regex("[^a-z]"), "")
        if (FORBIDDEN_ENDINGS.contains(lastWord)) return false

        return true
    }

    /**
     * Verifies that the concept name (or at least half of its significant words)
     * appears in the source chunk it was extracted from.
     */
    fun isGroundedInSource(conceptName: String, sourceText: String): Boolean {
        if (sourceText.isBlank() || conceptName.isBlank()) return false
        val cleanSource = sourceText.lowercase()
        val cleanName = conceptName.lowercase().trim()

        // Direct verbatim substring match
        if (cleanSource.contains(cleanName)) return true

        // Check significant words
        val words = cleanName.split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-z0-9]"), "") }
            .filter { it.length > 2 && !STOP_WORDS.contains(it) }

        if (words.isEmpty()) return false
        val matchCount = words.count { cleanSource.contains(it) }
        return matchCount >= (words.size + 1) / 2
    }

    /**
     * Normalises name for comparison:
     * - lowercase
     * - strip bullets and punctuation
     * - strip trailing plural 's'
     * - strip comparison-only stop words like "encoding", "scheme", etc.
     */
    fun normalizeForComparison(name: String): String {
        val clean = sanitizeConceptName(name).lowercase()
        val tokens = clean.split(Regex("[^a-z0-9]+"))
            .map { token ->
                var t = token.trim()
                // Strip trailing plural 's' (e.g. signals -> signal) if word length > 3
                if (t.endsWith("s") && t.length > 3 && !t.endsWith("ss") && !t.endsWith("us") && !t.endsWith("is")) {
                    t = t.removeSuffix("s")
                }
                t
            }
            .filter { it.isNotBlank() && !COMPARISON_ONLY_STOP_WORDS.contains(it) && !STOP_WORDS.contains(it) }

        return tokens.joinToString(" ")
    }

    /**
     * Levenshtein Distance calculation
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Calculates similarity score (0.0 to 1.0) between two concept names.
     * Incorporates token set inclusion and character-level edit distance.
     */
    fun calculateSimilarity(name1: String, name2: String): Double {
        val norm1 = normalizeForComparison(name1)
        val norm2 = normalizeForComparison(name2)

        if (norm1 == norm2) return 1.0
        if (norm1.isBlank() || norm2.isBlank()) return 0.0

        val tokens1 = norm1.split(" ").filter { it.isNotBlank() }.toSet()
        val tokens2 = norm2.split(" ").filter { it.isNotBlank() }.toSet()

        // Token Jaccard
        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size
        val jaccard = if (union > 0) intersection.toDouble() / union else 0.0

        // Substring / subset token match (e.g. "manchester" in "manchester biphase")
        if (tokens1.isNotEmpty() && tokens2.isNotEmpty()) {
            val minTokens = minOf(tokens1.size, tokens2.size)
            val maxTokens = maxOf(tokens1.size, tokens2.size)
            if (tokens1.containsAll(tokens2) || tokens2.containsAll(tokens1)) {
                val subsetBonus = 0.75 + 0.25 * (minTokens.toDouble() / maxTokens)
                return maxOf(jaccard, subsetBonus)
            }
        }

        // Levenshtein similarity
        val levDist = levenshteinDistance(norm1, norm2)
        val maxLen = maxOf(norm1.length, norm2.length)
        val levSim = 1.0 - (levDist.toDouble() / maxLen)

        return maxOf(jaccard, levSim)
    }

    /**
     * Deduplicates and merges concepts:
     * - Exact & near duplicates (similarity >= 0.85): merged immediately, combining key ideas and sources.
     * - Ambiguous pairs (0.6 <= similarity < 0.85): if askModel provided, asks model; if yes, merges.
     */
    suspend fun deduplicateConcepts(
        concepts: List<Concept>,
        askModelIsSameConcept: (suspend (Concept, Concept) -> Boolean)? = null
    ): List<Concept> {
        val mergedList = mutableListOf<Concept>()

        for (incoming in concepts) {
            var merged = false

            for (i in mergedList.indices) {
                val existing = mergedList[i]
                val sim = calculateSimilarity(existing.name, incoming.name)

                val shouldMerge = when {
                    sim >= 0.85 -> true
                    sim >= 0.60 && askModelIsSameConcept != null -> {
                        askModelIsSameConcept(existing, incoming)
                    }
                    else -> false
                }

                if (shouldMerge) {
                    // Choose the more descriptive name (or keep existing)
                    val chosenName = if (incoming.name.length > existing.name.length && isValidConceptName(incoming.name)) {
                        incoming.name
                    } else existing.name

                    val combinedKeyIdeas = (existing.keyIdeas + incoming.keyIdeas).distinct()
                    val combinedSources = (existing.sources + incoming.sources + listOf(incoming.source, existing.source))
                        .filter { it.isNotBlank() }
                        .distinct()
                    val combinedLinks = (existing.links + incoming.links).distinct().filter { it != existing.id }

                    val combinedMeaning = if (existing.meaning.length >= incoming.meaning.length) existing.meaning else incoming.meaning

                    mergedList[i] = existing.copy(
                        name = chosenName,
                        meaning = combinedMeaning,
                        keyIdeas = combinedKeyIdeas,
                        sources = combinedSources,
                        links = combinedLinks
                    )
                    merged = true
                    break
                }
            }

            if (!merged) {
                val sources = if (incoming.source.isNotBlank()) listOf(incoming.source) else incoming.sources
                mergedList.add(incoming.copy(sources = sources))
            }
        }

        return mergedList
    }

    /**
     * Clusters concepts into topics containing 3 to 8 concepts.
     * If a topic has more than 8 concepts, it is split into balanced sub-topics (e.g. Part 1, Part 2).
     */
    fun clusterIntoTopics(
        concepts: List<Concept>,
        baseTopicName: String = "Core Concepts"
    ): Map<String, List<Concept>> {
        if (concepts.isEmpty()) return emptyMap()

        // If 8 or fewer concepts, one single topic
        if (concepts.size <= 8) {
            return mapOf(baseTopicName to concepts)
        }

        // Split into chunks of at most 8 concepts, each with at least 3 (unless remainder is smaller)
        val result = mutableMapOf<String, List<Concept>>()
        val total = concepts.size
        // Calculate number of parts needed such that each has <= 8
        val partsNeeded = (total + 7) / 8
        val chunkSize = (total + partsNeeded - 1) / partsNeeded

        val chunks = concepts.chunked(chunkSize)
        for ((idx, chunk) in chunks.withIndex()) {
            val name = if (chunks.size == 1) baseTopicName else "$baseTopicName (Part ${idx + 1})"
            result[name] = chunk
        }

        return result
    }
}
