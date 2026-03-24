package com.cardkeeper.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cardkeeper.BuildConfig

@Database(
    entities = [CardEntity::class, TagEntity::class, CardTagCrossRef::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cardkeeper.db"
                )
                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }
                builder.build().also { INSTANCE = it }
            }
        }
    }
}
