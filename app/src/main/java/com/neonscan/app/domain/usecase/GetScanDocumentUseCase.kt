package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import com.neonscan.app.domain.model.ScanDocument
import javax.inject.Inject

class GetScanDocumentUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(id: Long): ScanDocument? = repository.getById(id)
}
