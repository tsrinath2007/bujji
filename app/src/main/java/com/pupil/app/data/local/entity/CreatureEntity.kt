package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creature")
data class CreatureEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val evolutionStage: Int = 1, // 1: Sprout, 2: Lumina, 3: Auron
    val streakDays: Int = 1,
    val lastStudyDateMillis: Long = System.currentTimeMillis()
)
