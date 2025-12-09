package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import javax.inject.Inject

class ReorderFoldersUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(orderedIds: List<Long>) {
        repository.updateFolderOrders(orderedIds.mapIndexed { index, id -> id to index })
    }
}
