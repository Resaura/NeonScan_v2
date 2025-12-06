package com.neonscan.app.domain.model

data class ScanDocument(
    val id: Long = 0L,
    val title: String,
    val type: ScanType,
    val path: String,
    val pageCount: Int,
    val createdAt: Long,
    val folderId: Long? = null,
    val kind: DocumentKind = DocumentKind.GENERIC
)
