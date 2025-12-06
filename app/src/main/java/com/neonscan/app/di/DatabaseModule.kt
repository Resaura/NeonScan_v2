package com.neonscan.app.di

import android.content.Context
import androidx.room.Room
import com.neonscan.app.data.local.db.NeonScanDatabase
import com.neonscan.app.data.local.db.ScanDocumentDao
import com.neonscan.app.data.local.db.FolderEntityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeonScanDatabase =
        Room.databaseBuilder(context, NeonScanDatabase::class.java, "neonscan.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideScanDocumentDao(db: NeonScanDatabase): ScanDocumentDao = db.scanDocumentDao()

    @Provides
    fun provideFolderDao(db: NeonScanDatabase): FolderEntityDao = db.folderDao()
}
