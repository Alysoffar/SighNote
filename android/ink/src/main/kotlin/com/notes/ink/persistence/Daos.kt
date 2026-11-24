package com.notes.ink.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StrokeDao {
    @Query("SELECT * FROM strokes WHERE page_id = :pageId ORDER BY created_at ASC")
    fun getStrokesForPage(pageId: String): Flow<List<StrokeEntity>>

    @Query("SELECT * FROM strokes WHERE page_id = :pageId ORDER BY created_at ASC")
    suspend fun getStrokesListForPage(pageId: String): List<StrokeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStroke(stroke: StrokeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrokes(strokes: List<StrokeEntity>)

    @Query("DELETE FROM strokes WHERE id = :strokeId")
    suspend fun deleteStrokeById(strokeId: String)

    @Query("DELETE FROM strokes WHERE page_id = :pageId")
    suspend fun deleteAllStrokesForPage(pageId: String)

    @Query("SELECT COUNT(*) FROM strokes WHERE page_id = :pageId")
    suspend fun getStrokeCount(pageId: String): Int
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePage(page: PageEntity)

    @Query("UPDATE pages SET revision_counter = revision_counter + 1, updated_at = :timestamp WHERE id = :pageId")
    suspend fun incrementRevision(pageId: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks WHERE id = :notebookId LIMIT 1")
    suspend fun getNotebookById(notebookId: String): NotebookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNotebook(notebook: NotebookEntity)
}
