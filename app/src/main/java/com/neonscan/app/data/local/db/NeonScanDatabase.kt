package com.neonscan.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neonscan.app.data.local.db.ScanDocumentEntity
import com.neonscan.app.data.local.db.FolderEntity
import com.neonscan.app.data.local.db.ScanTypeConverter
import com.neonscan.app.data.local.db.DocumentKindConverter
import com.neonscan.app.data.local.db.ScanDocumentDao
import com.neonscan.app.data.local.db.FolderEntityDao

@Database(
    entities = [ScanDocumentEntity::class, FolderEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(ScanTypeConverter::class, DocumentKindConverter::class)
abstract class NeonScanDatabase : RoomDatabase() {
    abstract fun scanDocumentDao(): ScanDocumentDao
    abstract fun folderDao(): FolderEntityDao
}
