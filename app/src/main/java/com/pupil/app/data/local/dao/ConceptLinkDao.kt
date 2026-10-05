package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.data.local.entity.ConceptLinkEntity

@Dao
interface ConceptLinkDao {

    @Query("SELECT * FROM concept_links WHERE fromConceptId = :conceptId OR toConceptId = :conceptId")
    suspend fun getLinksForConcept(conceptId: String): List<ConceptLinkEntity>

    @Query("SELECT toConceptId FROM concept_links WHERE fromConceptId = :conceptId")
    suspend fun getLinkedConceptIds(conceptId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: ConceptLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<ConceptLinkEntity>)

    @Query("DELETE FROM concept_links WHERE fromConceptId = :conceptId OR toConceptId = :conceptId")
    suspend fun deleteLinksForConcept(conceptId: String)

    @Query("""
        SELECT DISTINCT c.toConceptId
        FROM concept_links c
        WHERE c.fromConceptId IN (
            SELECT id FROM concepts WHERE topicId IS NULL AND subjectId = :subjectId
        )
    """)
    suspend fun getLinkedPairsForUnclustered(subjectId: String): List<String>

    /** All links within a subject, for graph rendering */
    @Query("""
        SELECT cl.* FROM concept_links cl
        JOIN concepts c ON cl.fromConceptId = c.id
        WHERE c.subjectId = :subjectId
    """)
    suspend fun getAllLinksForSubject(subjectId: String): List<ConceptLinkEntity>
}
