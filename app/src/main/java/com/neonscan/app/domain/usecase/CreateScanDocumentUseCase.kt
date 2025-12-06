package com.neonscan.app.domain.usecase

import com.neonscan.app.data.file.FileStorageManager
import com.neonscan.app.data.file.StoredScanResult
import com.neonscan.app.data.repository.ScanRepository
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.domain.model.DocumentKind
import javax.inject.Inject

data class CreateScanInput(
    val sourceImagePaths: List<String>,
    val title: String,
    val type: ScanType,
    val isBatch: Boolean,
    val kind: DocumentKind = DocumentKind.GENERIC
)

class CreateScanDocumentUseCase @Inject constructor(
    private val repository: ScanRepository,
    private val fileStorageManager: FileStorageManager
) {
    suspend operator fun invoke(input: CreateScanInput): Long {
        val stored: StoredScanResult = fileStorageManager.saveImages(input.sourceImagePaths)
        val document = ScanDocument(
            title = input.title,
            type = input.type,
            path = stored.primaryPath,
            pageCount = stored.pageCount,
            createdAt = System.currentTimeMillis(),
            kind = input.kind
        )
        return repository.insert(document)
    }
}
