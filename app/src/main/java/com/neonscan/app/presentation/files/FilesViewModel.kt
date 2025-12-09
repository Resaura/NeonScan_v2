package com.neonscan.app.presentation.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.usecase.AssignDocumentToFolderUseCase
import com.neonscan.app.domain.usecase.CreateFolderUseCase
import com.neonscan.app.domain.usecase.DeleteFolderUseCase
import com.neonscan.app.domain.usecase.DeleteScanDocumentUseCase
import com.neonscan.app.domain.usecase.GetDocumentsByFolderUseCase
import com.neonscan.app.domain.usecase.GetFoldersUseCase
import com.neonscan.app.domain.usecase.ReorderFoldersUseCase
import com.neonscan.app.domain.usecase.GetScanDocumentUseCase
import com.neonscan.app.domain.usecase.GetFolderUseCase
import com.neonscan.app.domain.usecase.RenameScanDocumentUseCase
import com.neonscan.app.domain.usecase.UpdateFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilesUiState(
    val documents: List<ScanDocument> = emptyList(),
    val baseDocuments: List<ScanDocument> = emptyList(), // documents bruts venant de la base avant filtrage
    val folders: List<com.neonscan.app.domain.model.Folder> = emptyList(),
    val reorderFolders: List<com.neonscan.app.domain.model.Folder> = emptyList(),
    val isReorderMode: Boolean = false,
    val selected: ScanDocument? = null,
    val selectedIds: Set<Long> = emptySet(),
    val filter: FilesFilter = FilesFilter(),
    val showFilter: Boolean = false,
    val showCreateFolderDialog: Boolean = false,
    val showAssignDialogFor: Long? = null,
    val currentFolderId: Long? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

data class FilesFilter(
    val type: com.neonscan.app.domain.model.ScanType? = null,
    val sortDescending: Boolean = true
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val deleteScanDocumentUseCase: DeleteScanDocumentUseCase,
    private val getScanDocumentUseCase: GetScanDocumentUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val getFolderUseCase: GetFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val reorderFoldersUseCase: ReorderFoldersUseCase,
    private val assignDocumentToFolderUseCase: AssignDocumentToFolderUseCase,
    private val getDocumentsByFolderUseCase: GetDocumentsByFolderUseCase,
    private val renameScanDocumentUseCase: RenameScanDocumentUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FilesUiState())
    val state: StateFlow<FilesUiState> = _state.asStateFlow()

    private var documentsJob: Job? = null

    init {
        observeFolders()
        observeDocuments(null)
    }

    private fun observeDocuments(folderId: Long?) {
        documentsJob?.cancel()
        documentsJob = viewModelScope.launch {
            getDocumentsByFolderUseCase(folderId).collect { docs ->
                _state.update { ui ->
                    ui.copy(
                        baseDocuments = docs,
                        documents = applyFilter(docs, ui.filter),
                        currentFolderId = folderId
                    )
                }
            }
        }
    }

    private fun observeFolders() {
        viewModelScope.launch {
            getFoldersUseCase().collect { folders ->
                _state.update {
                    it.copy(
                        folders = folders,
                        reorderFolders = if (it.isReorderMode && it.reorderFolders.isEmpty()) folders else it.reorderFolders
                    )
                }
            }
        }
    }

    fun applyFilter(filter: FilesFilter) {
        _state.update { it.copy(filter = filter, documents = applyFilter(it.baseDocuments, filter)) }
    }

    private fun applyFilter(docs: List<ScanDocument>, filter: FilesFilter): List<ScanDocument> {
        val filteredByType = filter.type?.let { type -> docs.filter { it.type == type } } ?: docs
        return if (filter.sortDescending) {
            filteredByType.sortedByDescending { it.createdAt }
        } else {
            filteredByType.sortedBy { it.createdAt }
        }
    }

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            val doc = getScanDocumentUseCase(id)
            _state.update { it.copy(selected = doc) }
        }
    }

    fun delete(document: ScanDocument) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            try {
                deleteScanDocumentUseCase(document)
            } catch (ex: Exception) {
                _state.update { it.copy(message = ex.localizedMessage ?: "Erreur lors de la suppression") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleSelection(id: Long) {
        _state.update {
            val newSet = it.selectedIds.toMutableSet()
            if (newSet.contains(id)) newSet.remove(id) else newSet.add(id)
            it.copy(selectedIds = newSet)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet()) }
    }

    fun showCreateFolderDialog(show: Boolean) {
        _state.update { it.copy(showCreateFolderDialog = show) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            createFolderUseCase(name)
            _state.update { it.copy(showCreateFolderDialog = false) }
        }
    }

    fun showFilter(show: Boolean) {
        _state.update { it.copy(showFilter = show) }
    }

    fun showAssignDialog(docId: Long?) {
        _state.update { it.copy(showAssignDialogFor = docId) }
    }

    fun assignToFolder(folderId: Long?) {
        viewModelScope.launch {
            val targetIds = if (state.value.selectedIds.isNotEmpty()) state.value.selectedIds.toList()
            else state.value.showAssignDialogFor?.let { listOf(it) } ?: emptyList()
            if (targetIds.isNotEmpty()) {
                assignDocumentToFolderUseCase(targetIds, folderId)
            }
            _state.update { it.copy(showAssignDialogFor = null, selectedIds = emptySet()) }
            observeDocuments(state.value.currentFolderId)
        }
    }

    fun openFolder(folderId: Long?) {
        val target = if (state.value.currentFolderId == folderId) null else folderId
        observeDocuments(target)
    }

    fun startReorderFolders() {
        _state.update { it.copy(isReorderMode = true, reorderFolders = it.folders) }
    }

    fun updateReorderList(newList: List<com.neonscan.app.domain.model.Folder>) {
        _state.update { it.copy(reorderFolders = newList) }
    }

    fun finishReorder(save: Boolean) {
        viewModelScope.launch {
            if (save) {
                val orderedIds = state.value.reorderFolders.map { it.id }
                reorderFoldersUseCase(orderedIds)
            }
            _state.update { it.copy(isReorderMode = false, reorderFolders = emptyList()) }
        }
    }

    suspend fun getFolder(id: Long): com.neonscan.app.domain.model.Folder? = getFolderUseCase(id)

    fun updateFolder(id: Long, name: String, colorHex: String) {
        viewModelScope.launch {
            updateFolderUseCase(id, name, colorHex)
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            deleteFolderUseCase(id)
        }
    }

    fun renameDocument(id: Long, title: String) {
        viewModelScope.launch {
            renameScanDocumentUseCase(id, title)
            if (state.value.selected?.id == id) {
                loadDetail(id)
            }
        }
    }
}
