package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import javax.inject.Inject

class GetFolderUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(id: Long) = repository.getFolderById(id)
}
