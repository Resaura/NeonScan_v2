package com.neonscan.app.domain.model

data class Folder(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long,
    val documentCount: Int = 0,
    val sortOrder: Int = 0,
    val colorHex: String = "#5CE1E6"
)
