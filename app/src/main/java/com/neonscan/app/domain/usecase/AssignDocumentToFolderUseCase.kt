package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import javax.inject.Inject

class AssignDocumentToFolderUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(documentIds: List<Long>, folderId: Long?) {
        repository.assignFolder(documentIds, folderId)
    }
}
