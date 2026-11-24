package com.notes.ink.persistence

import com.notes.model.Page
import com.notes.model.Stroke
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InkRepository(
    private val database: NotesDatabase
) {
    private val strokeDao = database.strokeDao()
    private val pageDao = database.pageDao()

    fun getStrokesForPage(pageId: String): Flow<List<Stroke>> {
        return strokeDao.getStrokesForPage(pageId).map { entities ->
            entities.map { it.toDomainStroke() }
        }
    }

    suspend fun getStrokesList(pageId: String): List<Stroke> {
        return strokeDao.getStrokesListForPage(pageId).map { it.toDomainStroke() }
    }

    suspend fun saveStroke(stroke: Stroke, pageId: String) {
        val entity = StrokeEntity.fromDomainStroke(stroke, pageId)
        strokeDao.insertStroke(entity)
        pageDao.incrementRevision(pageId)
    }

    suspend fun saveStrokes(strokes: List<Stroke>, pageId: String) {
        val entities = strokes.map { StrokeEntity.fromDomainStroke(it, pageId) }
        strokeDao.insertStrokes(entities)
        pageDao.incrementRevision(pageId)
    }

    suspend fun deleteStroke(strokeId: String, pageId: String) {
        strokeDao.deleteStrokeById(strokeId)
        pageDao.incrementRevision(pageId)
    }

    suspend fun clearPage(pageId: String) {
        strokeDao.deleteAllStrokesForPage(pageId)
        pageDao.incrementRevision(pageId)
    }

    suspend fun ensurePageExists(pageId: String, notebookId: String = "default-notebook"): Page {
        val existing = pageDao.getPageById(pageId)
        return if (existing != null) {
            existing.toDomainPage()
        } else {
            val newPage = PageEntity(
                id = pageId,
                notebookId = notebookId,
                revisionCounter = 0L
            )
            pageDao.insertOrUpdatePage(newPage)
            newPage.toDomainPage()
        }
    }

    suspend fun getPageRevision(pageId: String): Long {
        return pageDao.getPageById(pageId)?.revisionCounter ?: 0L
    }
}
