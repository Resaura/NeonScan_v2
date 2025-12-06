package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import com.neonscan.app.domain.model.ScanDocument
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentsByFolderUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    operator fun invoke(folderId: Long?): Flow<List<ScanDocument>> = repository.getByFolder(folderId)
}
