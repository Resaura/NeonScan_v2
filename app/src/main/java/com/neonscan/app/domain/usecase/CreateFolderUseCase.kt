package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(name: String): Long {
        require(name.isNotBlank()) { "Le nom du dossier ne peut pas être vide" }
        return repository.createFolder(name.trim())
    }
}
