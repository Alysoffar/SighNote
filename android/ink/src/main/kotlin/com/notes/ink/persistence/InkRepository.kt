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
    private val notebookDao = database.notebookDao()

    // --- Notebook Operations ---
    fun getAllNotebooks(): Flow<List<NotebookEntity>> = notebookDao.getAllNotebooks()

    suspend fun getAllNotebooksList(): List<NotebookEntity> = notebookDao.getAllNotebooksList()

    suspend fun getNotebook(notebookId: String): NotebookEntity? = notebookDao.getNotebookById(notebookId)

    suspend fun createNotebook(id: String = java.util.UUID.randomUUID().toString(), title: String = "Untitled Notebook"): NotebookEntity {
        val nb = NotebookEntity(id = id, title = title)
        notebookDao.insertOrUpdateNotebook(nb)
        // Ensure at least one initial page exists
        val firstPageId = java.util.UUID.randomUUID().toString()
        val firstPage = PageEntity(
            id = firstPageId,
            notebookId = id,
            pageIndex = 0,
            revisionCounter = 0L
        )
        pageDao.insertOrUpdatePage(firstPage)
        return nb
    }

    suspend fun renameNotebook(notebookId: String, newTitle: String) {
        notebookDao.updateTitle(notebookId, newTitle)
    }

    suspend fun deleteNotebook(notebookId: String) {
        val pages = pageDao.getPagesListForNotebook(notebookId)
        for (page in pages) {
            strokeDao.deleteAllStrokesForPage(page.id)
            pageDao.deletePageById(page.id)
        }
        notebookDao.deleteNotebookById(notebookId)
    }

    // --- Page Operations ---
    fun getPagesForNotebook(notebookId: String): Flow<List<PageEntity>> =
        pageDao.getPagesForNotebook(notebookId)

    suspend fun getPagesListForNotebook(notebookId: String): List<PageEntity> =
        pageDao.getPagesListForNotebook(notebookId)

    suspend fun getPage(pageId: String): PageEntity? = pageDao.getPageById(pageId)

    suspend fun addPage(notebookId: String, pdfPath: String? = null, pdfPageIndex: Int = 0): PageEntity {
        val currentCount = pageDao.getPageCount(notebookId)
        val newPageId = java.util.UUID.randomUUID().toString()
        val page = PageEntity(
            id = newPageId,
            notebookId = notebookId,
            pageIndex = currentCount,
            revisionCounter = 0L,
            pdfPath = pdfPath,
            pdfPageIndex = pdfPageIndex
        )
        pageDao.insertOrUpdatePage(page)
        notebookDao.touchNotebook(notebookId)
        return page
    }

    suspend fun deletePage(pageId: String, notebookId: String) {
        strokeDao.deleteAllStrokesForPage(pageId)
        pageDao.deletePageById(pageId)
        notebookDao.touchNotebook(notebookId)
    }

    suspend fun saveViewport(pageId: String, panX: Float, panY: Float, scale: Float) {
        pageDao.updateViewport(pageId, panX, panY, scale)
    }

    // --- Stroke Operations ---
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
            // Also ensure parent notebook exists
            if (notebookDao.getNotebookById(notebookId) == null) {
                notebookDao.insertOrUpdateNotebook(
                    NotebookEntity(id = notebookId, title = "Default Notebook")
                )
            }
            val newPage = PageEntity(
                id = pageId,
                notebookId = notebookId,
                pageIndex = 0,
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
