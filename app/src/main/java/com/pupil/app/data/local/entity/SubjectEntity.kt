package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String = "📚",
    val colourHex: String = "#5B4DFF",
    val createdAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = System.currentTimeMillis()
)
