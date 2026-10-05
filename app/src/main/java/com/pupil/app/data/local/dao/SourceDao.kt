package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pupil.app.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Query("SELECT * FROM sources WHERE subjectId = :subjectId ORDER BY createdAt ASC")
    fun getSourcesForSubject(subjectId: String): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE id = :sourceId LIMIT 1")
    suspend fun getSourceById(sourceId: String): SourceEntity?

    @Query("SELECT COUNT(*) FROM sources WHERE subjectId = :subjectId")
    fun getSourceCountForSubject(subjectId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<SourceEntity>)

    @Update
    suspend fun updateSource(source: SourceEntity)

    @Query("UPDATE sources SET processingState = :state, progress = :progress WHERE id = :sourceId")
    suspend fun updateProgress(sourceId: String, state: String, progress: Float)

    @Query("UPDATE sources SET processingState = :state, errorMessage = :error WHERE id = :sourceId")
    suspend fun updateFailed(sourceId: String, state: String = "FAILED", error: String)

    @Query("UPDATE sources SET processingState = 'DONE', progress = 1.0, pageCount = :pageCount WHERE id = :sourceId")
    suspend fun markDone(sourceId: String, pageCount: Int)

    @Query("UPDATE sources SET pageCount = :pageCount WHERE id = :sourceId")
    suspend fun updatePageCount(sourceId: String, pageCount: Int)

    @Query("DELETE FROM sources WHERE id = :sourceId")
    suspend fun deleteSourceById(sourceId: String)

    @Query("SELECT * FROM sources WHERE processingState IN ('QUEUED', 'READING') ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextPendingSource(): SourceEntity?

    @Query("SELECT * FROM sources WHERE processingState IN ('QUEUED', 'READING')")
    suspend fun getAllPendingSources(): List<SourceEntity>
}
