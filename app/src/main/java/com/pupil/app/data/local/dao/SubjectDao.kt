package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pupil.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY lastStudiedAt DESC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId LIMIT 1")
    suspend fun getSubjectById(subjectId: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :subjectId LIMIT 1")
    fun getSubjectByIdFlow(subjectId: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects ORDER BY lastStudiedAt DESC LIMIT 1")
    suspend fun getLatestSubject(): SubjectEntity?

    @Query("SELECT * FROM subjects ORDER BY lastStudiedAt DESC LIMIT 1")
    fun getLatestSubjectFlow(): Flow<SubjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET name = :name, emoji = :emoji, colourHex = :colourHex WHERE id = :subjectId")
    suspend fun updateSubjectDetails(subjectId: String, name: String, emoji: String, colourHex: String)

    @Query("UPDATE subjects SET lastStudiedAt = :timestamp WHERE id = :subjectId")
    suspend fun updateLastStudied(subjectId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubjectById(subjectId: String)
}
