package com.neonscan.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDocumentDao {
    @Query("SELECT * FROM scan_document ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ScanDocumentEntity>>

    @Query("SELECT * FROM scan_document ORDER BY created_at DESC")
    fun getAll(): Flow<List<ScanDocumentEntity>>

    @Query(
        """
        SELECT * FROM scan_document 
        WHERE (:folderId IS NULL AND folder_id IS NULL) OR (:folderId IS NOT NULL AND folder_id = :folderId)
        ORDER BY created_at DESC
        """
    )
    fun getByFolder(folderId: Long?): Flow<List<ScanDocumentEntity>>

    @Query("SELECT * FROM scan_document WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: ScanDocumentEntity): Long

    @Query("UPDATE scan_document SET folder_id = :folderId WHERE id IN (:ids)")
    suspend fun assignFolder(ids: List<Long>, folderId: Long?)

    @Query("UPDATE scan_document SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Delete
    suspend fun delete(document: ScanDocumentEntity)

    @Query("SELECT folder_id as id, COUNT(*) as count FROM scan_document WHERE folder_id IS NOT NULL GROUP BY folder_id")
    fun countByFolder(): Flow<List<FolderCount>>
}

data class FolderCount(
    val id: Long?,
    val count: Int
)
