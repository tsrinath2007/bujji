package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_usage")
data class CloudUsageEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD format
    val groqCalls: Int = 0,
    val ttsChars: Int = 0
)
