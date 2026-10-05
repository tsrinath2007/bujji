package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pupil.app.data.local.entity.ConceptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptDao {

    // ──── Reads ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM concepts ORDER BY name ASC")
    fun getAllConceptsFlow(): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts WHERE subjectId = :subjectId ORDER BY name ASC")
    fun getConceptsForSubject(subjectId: String): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts WHERE subjectId = :subjectId ORDER BY name ASC")
    suspend fun getConceptsForSubjectSync(subjectId: String): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE topicId = :topicId ORDER BY name ASC")
    fun getConceptsForTopic(topicId: String): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts WHERE topicId = :topicId ORDER BY name ASC")
    suspend fun getConceptsForTopicSync(topicId: String): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE id = :conceptId LIMIT 1")
    suspend fun getConceptById(conceptId: String): ConceptEntity?

    @Query("SELECT * FROM concepts WHERE subjectId = :subjectId AND normalizedName = :normalizedName LIMIT 1")
    suspend fun getConceptByNormalizedName(subjectId: String, normalizedName: String): ConceptEntity?

    @Query("""
        SELECT * FROM concepts 
        WHERE subjectId = :subjectId 
        AND LOWER(status) IN ('missed', 'misconception', 'partial', 'unstudied')
        ORDER BY lastTaught ASC
    """)
    suspend fun getGapConceptsForSubject(subjectId: String): List<ConceptEntity>

    @Query("""
        SELECT * FROM concepts 
        WHERE subjectId = :subjectId 
        AND LOWER(status) = 'understood'
        AND nextDue > 0 
        AND nextDue <= :nowMillis
    """)
    suspend fun getDueConceptsForSubject(subjectId: String, nowMillis: Long): List<ConceptEntity>

    @Query("SELECT COUNT(*) FROM concepts WHERE subjectId = :subjectId")
    fun getTotalCountForSubject(subjectId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM concepts WHERE subjectId = :subjectId AND LOWER(status) = 'understood'")
    fun getUnderstoodCountForSubject(subjectId: String): Flow<Int>

    @Query("SELECT * FROM concepts WHERE subjectId = :subjectId AND topicId IS NULL")
    suspend fun getUnclusteredConceptsForSubject(subjectId: String): List<ConceptEntity>

    // ──── Writes ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcept(concept: ConceptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcepts(concepts: List<ConceptEntity>)

    @Update
    suspend fun updateConcept(concept: ConceptEntity)

    @Query("""
        UPDATE concepts SET 
            status = :status,
            evidence = :evidence,
            followupQuestion = :followupQuestion,
            studentExplanation = :studentExplanation,
            earnedXp = :earnedXp,
            lastTaught = :lastTaught,
            nextDue = :nextDue,
            revisionIntervalIndex = :revisionIntervalIndex,
            lastUpdated = :lastUpdated
        WHERE id = :conceptId
    """)
    suspend fun updateGradingResult(
        conceptId: String,
        status: String,
        evidence: String,
        followupQuestion: String,
        studentExplanation: String,
        earnedXp: Int,
        lastTaught: Long,
        nextDue: Long,
        revisionIntervalIndex: Int,
        lastUpdated: Long = System.currentTimeMillis()
    )

    @Query("UPDATE concepts SET topicId = :topicId WHERE id = :conceptId")
    suspend fun assignTopic(conceptId: String, topicId: String)

    @Query("UPDATE concepts SET topicId = :topicId WHERE id IN (:conceptIds)")
    suspend fun assignTopicToMany(conceptIds: List<String>, topicId: String)

    @Query("DELETE FROM concepts WHERE id = :conceptId")
    suspend fun deleteConceptById(conceptId: String)

    @Query("DELETE FROM concepts WHERE subjectId = :subjectId")
    suspend fun deleteAllConceptsForSubject(subjectId: String)

    // ──── Merging ─────────────────────────────────────────────────────────────

    /** Merge additional key ideas from a newly extracted concept into an existing one */
    @Query("UPDATE concepts SET keyIdeasJson = :mergedKeyIdeasJson, lastUpdated = :ts WHERE id = :conceptId")
    suspend fun mergeKeyIdeas(conceptId: String, mergedKeyIdeasJson: String, ts: Long = System.currentTimeMillis())
}
