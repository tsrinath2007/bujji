package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "teaching_sessions",
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
    indices = [Index("subjectId"), Index("topicId")]
)
data class TeachingSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val topicId: String?,
    val isGapsOnly: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CANCELLED"
    val turnCount: Int = 0,
    val summaryJson: String? = null
)
