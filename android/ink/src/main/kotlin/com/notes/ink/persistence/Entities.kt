package com.notes.ink.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.notes.model.BoundingBox
import com.notes.model.Stroke
import com.notes.model.Page
import com.notes.model.Notebook

@Entity(
    tableName = "strokes",
    indices = [Index(value = ["page_id"])]
)
data class StrokeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "page_id") val pageId: String,
    @ColumnInfo(name = "brush_id") val brushId: String,
    @ColumnInfo(name = "min_x") val minX: Float,
    @ColumnInfo(name = "min_y") val minY: Float,
    @ColumnInfo(name = "max_x") val maxX: Float,
    @ColumnInfo(name = "max_y") val maxY: Float,
    @ColumnInfo(name = "points_blob") val pointsBlob: ByteArray,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainStroke(): Stroke {
        return Stroke(
            id = id,
            brush_id = brushId,
            points = StrokePointsBinarySerializer.deserialize(pointsBlob),
            bounds = BoundingBox(minX, minY, maxX, maxY)
        )
    }

    companion object {
        fun fromDomainStroke(stroke: Stroke, pageId: String): StrokeEntity {
            return StrokeEntity(
                id = stroke.id,
                pageId = pageId,
                brushId = stroke.brush_id,
                minX = stroke.bounds.min_x,
                minY = stroke.bounds.min_y,
                maxX = stroke.bounds.max_x,
                maxY = stroke.bounds.max_y,
                pointsBlob = StrokePointsBinarySerializer.serialize(stroke.points)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StrokeEntity
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Entity(
    tableName = "pages",
    indices = [Index(value = ["notebook_id"])]
)
data class PageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "notebook_id") val notebookId: String,
    @ColumnInfo(name = "page_index") val pageIndex: Int = 0,
    @ColumnInfo(name = "revision_counter") val revisionCounter: Long = 0L,
    @ColumnInfo(name = "pan_x") val panX: Float = 0f,
    @ColumnInfo(name = "pan_y") val panY: Float = 0f,
    @ColumnInfo(name = "scale") val scale: Float = 1f,
    @ColumnInfo(name = "pdf_path") val pdfPath: String? = null,
    @ColumnInfo(name = "pdf_page_index") val pdfPageIndex: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainPage(strokes: List<Stroke> = emptyList()): Page {
        return Page(
            id = id,
            notebook_id = notebookId,
            revision_counter = revisionCounter,
            strokes = strokes
        )
    }
}

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
