package com.neonscan.app.di

import com.neonscan.app.data.file.FileStorageManager
import com.neonscan.app.data.repository.ScanRepository
import com.neonscan.app.domain.usecase.CreateScanDocumentUseCase
import com.neonscan.app.domain.usecase.DeleteScanDocumentUseCase
import com.neonscan.app.domain.usecase.GetAllScansUseCase
import com.neonscan.app.domain.usecase.GetRecentScansUseCase
import com.neonscan.app.domain.usecase.GetScanDocumentUseCase
import com.neonscan.app.domain.usecase.CreateFolderUseCase
import com.neonscan.app.domain.usecase.GetFoldersUseCase
import com.neonscan.app.domain.usecase.AssignDocumentToFolderUseCase
import com.neonscan.app.domain.usecase.GetDocumentsByFolderUseCase
import com.neonscan.app.domain.usecase.RenameScanDocumentUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideCreateScanDocumentUseCase(
        repository: ScanRepository,
        fileStorageManager: FileStorageManager
    ) = CreateScanDocumentUseCase(repository, fileStorageManager)

    @Provides
    fun provideGetRecentScansUseCase(repository: ScanRepository) = GetRecentScansUseCase(repository)

    @Provides
    fun provideGetAllScansUseCase(repository: ScanRepository) = GetAllScansUseCase(repository)

    @Provides
    fun provideDeleteScanDocumentUseCase(
        repository: ScanRepository,
        fileStorageManager: FileStorageManager
    ) = DeleteScanDocumentUseCase(repository, fileStorageManager)

    @Provides
    fun provideGetScanDocumentUseCase(repository: ScanRepository) = GetScanDocumentUseCase(repository)

    @Provides
    fun provideCreateFolderUseCase(repository: ScanRepository) = CreateFolderUseCase(repository)

    @Provides
    fun provideGetFoldersUseCase(repository: ScanRepository) = GetFoldersUseCase(repository)

    @Provides
    fun provideAssignDocumentToFolderUseCase(repository: ScanRepository) = AssignDocumentToFolderUseCase(repository)

    @Provides
    fun provideGetDocumentsByFolderUseCase(repository: ScanRepository) = GetDocumentsByFolderUseCase(repository)

    @Provides
    fun provideRenameScanDocumentUseCase(repository: ScanRepository) = RenameScanDocumentUseCase(repository)
}
