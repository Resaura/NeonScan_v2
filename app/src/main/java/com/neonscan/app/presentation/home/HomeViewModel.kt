package com.neonscan.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.domain.usecase.AssignDocumentToFolderUseCase
import com.neonscan.app.domain.usecase.CreateScanDocumentUseCase
import com.neonscan.app.domain.usecase.CreateScanInput
import com.neonscan.app.domain.usecase.GetFoldersUseCase
import com.neonscan.app.domain.usecase.GetRecentScansUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val recent: List<ScanDocument> = emptyList(),
    val folders: List<com.neonscan.app.domain.model.Folder> = emptyList(),
    val showAssignDialogFor: Long? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentScansUseCase: GetRecentScansUseCase,
    private val createScanDocumentUseCase: CreateScanDocumentUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val assignDocumentToFolderUseCase: AssignDocumentToFolderUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val titleFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    init {
        observeRecent()
        observeFolders()
    }

    private fun observeRecent() {
        viewModelScope.launch {
            getRecentScansUseCase().collect { docs ->
                _state.update { it.copy(recent = docs) }
            }
        }
    }

    private fun observeFolders() {
        viewModelScope.launch {
            getFoldersUseCase().collect { folders ->
                _state.update { it.copy(folders = folders) }
            }
        }
    }

    fun saveScan(paths: List<String>, isBatch: Boolean) {
        if (paths.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            try {
                createScanDocumentUseCase(
                    CreateScanInput(
                        sourceImagePaths = paths,
                        title = "Scan du ${titleFormat.format(Date())}",
                        type = ScanType.IMAGE,
                        isBatch = isBatch
                    )
                )
            } catch (ex: Exception) {
                _state.update { it.copy(message = ex.localizedMessage ?: "Erreur inconnue") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun removeFromFolder(documentId: Long) {
        viewModelScope.launch {
            assignDocumentToFolderUseCase(listOf(documentId), null)
        }
    }

    fun showAssignDialog(documentId: Long?) {
        _state.update { it.copy(showAssignDialogFor = documentId) }
    }

    fun assignToFolder(folderId: Long?) {
        viewModelScope.launch {
            state.value.showAssignDialogFor?.let { targetId ->
                assignDocumentToFolderUseCase(listOf(targetId), folderId)
            }
            _state.update { it.copy(showAssignDialogFor = null) }
        }
    }
}
