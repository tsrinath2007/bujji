package com.pupil.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "concept_links",
    primaryKeys = ["fromConceptId", "toConceptId"],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromConceptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["toConceptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fromConceptId"), Index("toConceptId")]
)
data class ConceptLinkEntity(
    val fromConceptId: String,
    val toConceptId: String
)
