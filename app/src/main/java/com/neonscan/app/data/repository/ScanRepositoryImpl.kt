package com.neonscan.app.data.repository

import com.neonscan.app.data.local.db.FolderEntity
import com.neonscan.app.data.local.db.ScanDocumentDao
import com.neonscan.app.data.local.db.FolderEntityDao
import com.neonscan.app.data.local.db.ScanDocumentEntity
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.Folder
import com.neonscan.app.domain.model.ScanType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val scanDocumentDao: ScanDocumentDao,
    private val folderDao: FolderEntityDao
) : ScanRepository {

    override fun getRecent(limit: Int): Flow<List<ScanDocument>> =
        scanDocumentDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun getAll(): Flow<List<ScanDocument>> =
        scanDocumentDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByFolder(folderId: Long?): Flow<List<ScanDocument>> =
        scanDocumentDao.getByFolder(folderId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): ScanDocument? = scanDocumentDao.getById(id)?.toDomain()

    override suspend fun insert(document: ScanDocument): Long =
        scanDocumentDao.insert(document.toEntity())

    override suspend fun delete(document: ScanDocument) {
        scanDocumentDao.delete(document.toEntity())
    }

    override suspend fun assignFolder(documentIds: List<Long>, folderId: Long?) {
        scanDocumentDao.assignFolder(documentIds, folderId)
    }

    override suspend fun rename(documentId: Long, title: String) {
        scanDocumentDao.rename(documentId, title)
    }

    override suspend fun createFolder(name: String): Long =
        folderDao.insert(
            FolderEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                sortOrder = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            )
        )

    override fun getFolders(): Flow<List<Folder>> =
        kotlinx.coroutines.flow.combine(
            folderDao.getAll(),
            scanDocumentDao.countByFolder()
        ) { folders, counts ->
            val countMap = counts.associate { it.id to it.count }
            folders.map { f ->
                Folder(
                    id = f.id,
                    name = f.name,
                    createdAt = f.createdAt,
                    documentCount = countMap[f.id] ?: 0,
                    sortOrder = f.sortOrder,
                    colorHex = f.colorHex
                )
            }
        }

    override suspend fun getFolderById(id: Long): Folder? =
        folderDao.getById(id)?.let {
            Folder(
                id = it.id,
                name = it.name,
                createdAt = it.createdAt,
                sortOrder = it.sortOrder,
                colorHex = it.colorHex
            )
        }

    override suspend fun updateFolder(id: Long, name: String, colorHex: String) {
        folderDao.updateFolder(id, name, colorHex)
    }

    override suspend fun deleteFolder(id: Long) {
        folderDao.delete(id)
    }

    override suspend fun updateFolderOrders(order: List<Pair<Long, Int>>) {
        order.forEach { (id, index) -> folderDao.updateSort(id, index) }
    }

    private fun ScanDocumentEntity.toDomain() = ScanDocument(
        id = id,
        title = title,
        type = type,
        path = path,
        pageCount = pageCount,
        createdAt = createdAt,
        folderId = folderId,
        kind = kind
    )

    private fun ScanDocument.toEntity() = ScanDocumentEntity(
        id = id,
        title = title,
        type = type,
        path = path,
        pageCount = pageCount,
        createdAt = createdAt,
        folderId = folderId,
        kind = kind
    )
}
