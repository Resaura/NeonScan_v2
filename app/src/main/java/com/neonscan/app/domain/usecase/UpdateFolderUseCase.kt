package com.neonscan.app.domain.usecase

import com.neonscan.app.data.repository.ScanRepository
import javax.inject.Inject

class UpdateFolderUseCase @Inject constructor(
    private val repository: ScanRepository
) {
    suspend operator fun invoke(id: Long, name: String, colorHex: String) {
        require(name.isNotBlank()) { "Le nom du dossier ne peut pas être vide" }
        repository.updateFolder(id, name.trim(), colorHex)
    }
}
