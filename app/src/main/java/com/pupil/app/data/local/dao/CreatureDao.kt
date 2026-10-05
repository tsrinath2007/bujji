package com.pupil.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.data.local.entity.CreatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreatureDao {

    @Query("SELECT * FROM creature WHERE id = 1 LIMIT 1")
    fun getCreatureFlow(): Flow<CreatureEntity?>

    @Query("SELECT * FROM creature WHERE id = 1 LIMIT 1")
    suspend fun getCreature(): CreatureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(creature: CreatureEntity)
}
