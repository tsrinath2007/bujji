package com.pupil.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Concept(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerialName("name")
    val name: String,

    @SerialName("meaning")
    val meaning: String,

    @SerialName("key_ideas")
    val keyIdeas: List<String> = emptyList(),

    @SerialName("source")
    val source: String = "",

    @SerialName("links")
    val links: List<String> = emptyList(),

    // Subject & Topic linkage
    val subjectId: String = "",
    val topicId: String? = null,
    val normalizedName: String = normalizeConceptName(name),
    val sources: List<String> = emptyList(), // e.g. ["Campbell_Bio.pdf: Page 14"]

    // Learning & Grading state
    val status: GradingStatus = GradingStatus.UNSTUDIED,
    val evidence: String = "",
    val followupQuestion: String = "",
    val studentExplanation: String = "",
    val regraded: Boolean = false,
    val lastTaught: Long = 0L,
    val nextDue: Long = 0L,
    val earnedXp: Int = 0,
    val revisionIntervalIndex: Int = 0
) {
    companion object {
        fun normalizeConceptName(name: String): String {
            return name
                .trim()
                .lowercase()
                .replace(Regex("^[•\\-*\\d.]+\\s*"), "") // strip bullets and numbers
                .replace(Regex("[^a-z0-9\\s]"), "") // strip punctuation
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}

@Serializable
data class ExtractionResponse(
    @SerialName("concepts")
    val concepts: List<Concept> = emptyList()
)
