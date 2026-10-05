package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sources",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class SourceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val type: String, // "PDF", "PHOTO", "TEXT"
    val displayName: String,
    val filePath: String? = null,
    val pageCount: Int = 1,
    val processingState: String = "QUEUED", // "QUEUED", "READING", "DONE", "FAILED"
    val progress: Float = 0f, // 0.0 to 1.0
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
