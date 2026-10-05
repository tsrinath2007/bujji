package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pupil.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY createdAt ASC")
    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY createdAt ASC")
    suspend fun getTopicsForSubjectSync(subjectId: String): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: String): TopicEntity?

    @Query("SELECT COUNT(*) FROM topics WHERE subjectId = :subjectId")
    fun getTopicCountForSubject(subjectId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE topics SET name = :name WHERE id = :topicId")
    suspend fun renameTopic(topicId: String, name: String)

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteTopicById(topicId: String)

    @Query("DELETE FROM topics WHERE subjectId = :subjectId")
    suspend fun deleteAllTopicsForSubject(subjectId: String)
}
