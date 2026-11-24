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

    @Query("SELECT * FROM pages WHERE notebook_id = :notebookId ORDER BY page_index ASC")
    fun getPagesForNotebook(notebookId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE notebook_id = :notebookId ORDER BY page_index ASC")
    suspend fun getPagesListForNotebook(notebookId: String): List<PageEntity>

    @Query("SELECT COUNT(*) FROM pages WHERE notebook_id = :notebookId")
    suspend fun getPageCount(notebookId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePage(page: PageEntity)

    @Query("UPDATE pages SET revision_counter = revision_counter + 1, updated_at = :timestamp WHERE id = :pageId")
    suspend fun incrementRevision(pageId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pages SET pan_x = :panX, pan_y = :panY, scale = :scale, updated_at = :timestamp WHERE id = :pageId")
    suspend fun updateViewport(pageId: String, panX: Float, panY: Float, scale: Float, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: String)

    @Query("DELETE FROM pages WHERE notebook_id = :notebookId")
    suspend fun deletePagesForNotebook(notebookId: String)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY updated_at DESC")
    fun getAllNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks ORDER BY updated_at DESC")
    suspend fun getAllNotebooksList(): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE id = :notebookId LIMIT 1")
    suspend fun getNotebookById(notebookId: String): NotebookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNotebook(notebook: NotebookEntity)

    @Query("UPDATE notebooks SET title = :title, updated_at = :timestamp WHERE id = :notebookId")
    suspend fun updateTitle(notebookId: String, title: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notebooks SET updated_at = :timestamp WHERE id = :notebookId")
    suspend fun touchNotebook(notebookId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM notebooks WHERE id = :notebookId")
    suspend fun deleteNotebookById(notebookId: String)
}
