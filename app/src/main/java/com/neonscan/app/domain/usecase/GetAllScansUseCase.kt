package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import com.neonscan.app.domain.model.ScanDocument
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllScansUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    operator fun invoke(): Flow<List<ScanDocument>> = repository.getAll()
}
