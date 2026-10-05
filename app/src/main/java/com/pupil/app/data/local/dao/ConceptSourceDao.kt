package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.data.local.entity.ConceptSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptSourceDao {

    @Query("SELECT * FROM concept_sources WHERE conceptId = :conceptId")
    suspend fun getSourcesForConcept(conceptId: String): List<ConceptSourceEntity>

    @Query("""
        SELECT cs.sourceId, s.displayName, cs.pageRef
        FROM concept_sources cs
        JOIN sources s ON cs.sourceId = s.id
        WHERE cs.conceptId = :conceptId
    """)
    suspend fun getSourceRefsForConcept(conceptId: String): List<SourceRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConceptSource(conceptSource: ConceptSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConceptSources(conceptSources: List<ConceptSourceEntity>)

    @Query("DELETE FROM concept_sources WHERE conceptId = :conceptId AND sourceId = :sourceId")
    suspend fun removeConceptSource(conceptId: String, sourceId: String)

    @Query("DELETE FROM concept_sources WHERE sourceId = :sourceId")
    suspend fun removeAllForSource(sourceId: String)

    /** Formatted strings like "Campbell_Bio.pdf: Page 14" */
    @Query("""
        SELECT (s.displayName || ': ' || cs.pageRef) as ref
        FROM concept_sources cs
        JOIN sources s ON cs.sourceId = s.id
        WHERE cs.conceptId = :conceptId
        ORDER BY s.displayName, cs.pageRef
    """)
    suspend fun getFormattedSourceRefsForConcept(conceptId: String): List<String>
}

/** Lightweight DTO returned by getSourceRefsForConcept */
data class SourceRef(
    val sourceId: String,
    val displayName: String,
    val pageRef: String
) {
    val formatted: String get() = "$displayName: $pageRef"
}
