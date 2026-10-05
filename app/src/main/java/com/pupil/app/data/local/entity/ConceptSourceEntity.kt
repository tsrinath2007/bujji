package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "concept_sources",
    primaryKeys = ["conceptId", "sourceId", "pageRef"],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conceptId"), Index("sourceId")]
)
data class ConceptSourceEntity(
    val conceptId: String,
    val sourceId: String,
    val pageRef: String // e.g. "Page 14" or "Photo 1"
)
