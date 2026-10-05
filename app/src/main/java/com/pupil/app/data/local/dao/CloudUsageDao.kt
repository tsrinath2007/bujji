package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.data.local.entity.CloudUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudUsageDao {
    @Query("SELECT * FROM cloud_usage WHERE date = :date LIMIT 1")
    suspend fun getUsageForDate(date: String): CloudUsageEntity?

    @Query("SELECT * FROM cloud_usage WHERE date = :date LIMIT 1")
    fun getUsageFlowForDate(date: String): Flow<CloudUsageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: CloudUsageEntity)

    @Query("UPDATE cloud_usage SET groqCalls = groqCalls + :delta WHERE date = :date")
    suspend fun incrementGroqCalls(date: String, delta: Int = 1)

    @Query("UPDATE cloud_usage SET ttsChars = ttsChars + :chars WHERE date = :date")
    suspend fun incrementTtsChars(date: String, chars: Int)
}
