package com.neonscan.app.data.repository

import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun getRecent(limit: Int): Flow<List<ScanDocument>>
    fun getAll(): Flow<List<ScanDocument>>
    fun getByFolder(folderId: Long?): Flow<List<ScanDocument>>
    suspend fun getById(id: Long): ScanDocument?
    suspend fun insert(document: ScanDocument): Long
    suspend fun delete(document: ScanDocument)
    suspend fun assignFolder(documentIds: List<Long>, folderId: Long?)
    suspend fun rename(documentId: Long, title: String)

    suspend fun createFolder(name: String): Long
    fun getFolders(): Flow<List<Folder>>
    suspend fun getFolderById(id: Long): Folder?
    suspend fun updateFolder(id: Long, name: String, colorHex: String)
    suspend fun deleteFolder(id: Long)
    suspend fun updateFolderOrders(order: List<Pair<Long, Int>>)
}
