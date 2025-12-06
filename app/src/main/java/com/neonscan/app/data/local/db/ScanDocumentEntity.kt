package com.neonscan.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.domain.model.DocumentKind

@Entity(tableName = "scan_document")
data class ScanDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "type") val type: ScanType,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "folder_id") val folderId: Long?,
    @ColumnInfo(name = "kind") val kind: DocumentKind = DocumentKind.GENERIC
)
