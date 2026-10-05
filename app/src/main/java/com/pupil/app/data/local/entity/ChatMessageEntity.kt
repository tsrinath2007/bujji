package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = TeachingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val sender: String, // "CREATURE" or "STUDENT"
    val text: String,
    val turnIndex: Int,
    val targetConceptIdsJson: String? = null, // JSON list of concepts evaluated in this turn
    val creatureDoubt: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
