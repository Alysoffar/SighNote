package com.notes.ink.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StrokeEntity::class, PageEntity::class, NotebookEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun strokeDao(): StrokeDao
    abstract fun pageDao(): PageDao
    abstract fun notebookDao(): NotebookDao

    companion object {
        @Volatile
        private var INSTANCE: NotesDatabase? = null

        fun getInstance(context: Context): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "notes_database.db"
                ).build().also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): NotesDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                NotesDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
