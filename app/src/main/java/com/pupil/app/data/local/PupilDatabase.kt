package com.pupil.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pupil.app.data.local.dao.ChatMessageDao
import com.pupil.app.data.local.dao.ConceptDao
import com.pupil.app.data.local.dao.ConceptLinkDao
import com.pupil.app.data.local.dao.ConceptSourceDao
import com.pupil.app.data.local.dao.CreatureDao
import com.pupil.app.data.local.dao.SourceDao
import com.pupil.app.data.local.dao.SubjectDao
import com.pupil.app.data.local.dao.TeachingSessionDao
import com.pupil.app.data.local.dao.TopicDao
import com.pupil.app.data.local.dao.CloudUsageDao
import com.pupil.app.data.local.entity.ChatMessageEntity
import com.pupil.app.data.local.entity.CloudUsageEntity
import com.pupil.app.data.local.entity.ConceptEntity
import com.pupil.app.data.local.entity.ConceptLinkEntity
import com.pupil.app.data.local.entity.ConceptSourceEntity
import com.pupil.app.data.local.entity.CreatureEntity
import com.pupil.app.data.local.entity.SourceEntity
import com.pupil.app.data.local.entity.SubjectEntity
import com.pupil.app.data.local.entity.TeachingSessionEntity
import com.pupil.app.data.local.entity.TopicEntity

@Database(
    entities = [
        SubjectEntity::class,
        SourceEntity::class,
        TopicEntity::class,
        ConceptEntity::class,
        ConceptSourceEntity::class,
        ConceptLinkEntity::class,
        TeachingSessionEntity::class,
        ChatMessageEntity::class,
        CreatureEntity::class,
        CloudUsageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PupilDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun sourceDao(): SourceDao
    abstract fun topicDao(): TopicDao
    abstract fun conceptDao(): ConceptDao
    abstract fun conceptSourceDao(): ConceptSourceDao
    abstract fun conceptLinkDao(): ConceptLinkDao
    abstract fun teachingSessionDao(): TeachingSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun creatureDao(): CreatureDao
    abstract fun cloudUsageDao(): CloudUsageDao

    companion object {
        @Volatile
        private var INSTANCE: PupilDatabase? = null

        fun getInstance(context: Context): PupilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PupilDatabase::class.java,
                    "pupil_database.db"
                )
                    // Destructive migration: existing data was junk. Start clean.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
