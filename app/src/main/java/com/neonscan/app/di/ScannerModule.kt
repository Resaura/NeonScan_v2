package com.neonscan.app.di

import com.neonscan.app.data.scanner.DocumentScannerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {
    @Provides
    @Singleton
    fun provideDocumentScannerManager(): DocumentScannerManager = DocumentScannerManager()
}
