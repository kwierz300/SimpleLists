package com.example.simplelists.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ItemEntity::class, ListEntity::class],
    version = 3,                           // ← PODBIJAMY (mamy nową kolumnę)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simple_lists.db"
                )
                    .fallbackToDestructiveMigration()   // dev: czyści starą bazę
                    .build().also { INSTANCE = it }
            }
        }
    }
}
