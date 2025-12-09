package com.neonscan.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Query("SELECT * FROM folders ORDER BY sort_order ASC, created_at DESC")
    fun getAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FolderEntity?

    @Query("UPDATE folders SET name = :name, color_hex = :colorHex WHERE id = :id")
    suspend fun updateFolder(id: Long, name: String, colorHex: String)

    @Query("UPDATE folders SET sort_order = :order WHERE id = :id")
    suspend fun updateSort(id: Long, order: Int)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)
}
