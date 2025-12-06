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

    @Query("SELECT * FROM folders ORDER BY created_at DESC")
    fun getAll(): Flow<List<FolderEntity>>
}
