package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Entity(
    tableName = "concepts",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("topicId"), Index("normalizedName")]
)
data class ConceptEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val topicId: String? = null,
    val name: String,
    val normalizedName: String = Concept.normalizeConceptName(name),
    val meaning: String,
    val keyIdeasJson: String = "[]",
    val status: String = GradingStatus.UNSTUDIED.serializedValue,
    val evidence: String = "",
    val followupQuestion: String = "",
    val studentExplanation: String = "",
    val regraded: Boolean = false,
    val lastTaught: Long = 0L,
    val nextDue: Long = 0L,
    val earnedXp: Int = 0,
    val revisionIntervalIndex: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toConcept(sources: List<String> = emptyList(), links: List<String> = emptyList()): Concept {
        val json = Json { ignoreUnknownKeys = true }
        val parsedKeyIdeas: List<String> = try {
            json.decodeFromString(keyIdeasJson)
        } catch (_: Exception) {
            emptyList()
        }

        return Concept(
            id = id,
            name = name,
            normalizedName = normalizedName,
            meaning = meaning,
            keyIdeas = parsedKeyIdeas,
            source = sources.firstOrNull() ?: "",
            sources = sources,
            links = links,
            subjectId = subjectId,
            topicId = topicId,
            status = GradingStatus.fromString(status),
            evidence = evidence,
            followupQuestion = followupQuestion,
            studentExplanation = studentExplanation,
            regraded = regraded,
            lastTaught = lastTaught,
            nextDue = nextDue,
            earnedXp = earnedXp,
            revisionIntervalIndex = revisionIntervalIndex
        )
    }

    companion object {
        fun fromConcept(concept: Concept, subjectId: String, topicId: String? = null): ConceptEntity {
            val json = Json { ignoreUnknownKeys = true }
            return ConceptEntity(
                id = concept.id.ifBlank { UUID.randomUUID().toString() },
                subjectId = subjectId.ifBlank { concept.subjectId },
                topicId = topicId ?: concept.topicId,
                name = concept.name,
                normalizedName = Concept.normalizeConceptName(concept.name),
                meaning = concept.meaning,
                keyIdeasJson = json.encodeToString(concept.keyIdeas),
                status = concept.status.serializedValue,
                evidence = concept.evidence,
                followupQuestion = concept.followupQuestion,
                studentExplanation = concept.studentExplanation,
                regraded = concept.regraded,
                lastTaught = concept.lastTaught,
                nextDue = concept.nextDue,
                earnedXp = concept.earnedXp,
                revisionIntervalIndex = concept.revisionIntervalIndex,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}
