package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pupil.app.data.local.entity.TeachingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeachingSessionDao {

    @Query("SELECT * FROM teaching_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): TeachingSessionEntity?

    @Query("SELECT * FROM teaching_sessions WHERE subjectId = :subjectId ORDER BY startedAt DESC")
    fun getSessionsForSubject(subjectId: String): Flow<List<TeachingSessionEntity>>

    @Query("SELECT * FROM teaching_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): TeachingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TeachingSessionEntity)

    @Update
    suspend fun updateSession(session: TeachingSessionEntity)

    @Query("UPDATE teaching_sessions SET status = :status, completedAt = :completedAt, summaryJson = :summaryJson WHERE id = :sessionId")
    suspend fun completeSession(
        sessionId: String,
        status: String = "COMPLETED",
        completedAt: Long = System.currentTimeMillis(),
        summaryJson: String? = null
    )

    @Query("UPDATE teaching_sessions SET turnCount = :turnCount WHERE id = :sessionId")
    suspend fun updateTurnCount(sessionId: String, turnCount: Int)

    @Query("DELETE FROM teaching_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM teaching_sessions WHERE status = 'COMPLETED' AND completedAt < :olderThanMillis")
    suspend fun purgeOldCompletedSessions(olderThanMillis: Long)
}
