package com.neonscan.app.domain.usecase

import com.neonscan.app.data.file.FileStorageManager
import com.neonscan.app.data.repository.ScanRepository
import com.neonscan.app.domain.model.ScanDocument
import javax.inject.Inject

class DeleteScanDocumentUseCase @Inject constructor(
    private val repository: ScanRepository,
    private val fileStorageManager: FileStorageManager
) {
    suspend operator fun invoke(document: ScanDocument) {
        repository.delete(document)
        fileStorageManager.deleteDocument(document.path)
    }
}
