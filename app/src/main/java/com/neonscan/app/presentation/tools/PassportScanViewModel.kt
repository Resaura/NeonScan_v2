package com.neonscan.app.presentation.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.neonscan.app.domain.model.DocumentKind
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.domain.usecase.CreateScanDocumentUseCase
import com.neonscan.app.domain.usecase.CreateScanInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PassportUiState(
    val totalPages: Int? = null,
    val currentPage: Int = 1,
    val pagePaths: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class PassportScanViewModel @Inject constructor(
    private val createScanDocumentUseCase: CreateScanDocumentUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(PassportUiState())
    val state: StateFlow<PassportUiState> = _state.asStateFlow()

    fun setTotalPages(count: Int) {
        if (count > 0) {
            _state.update { it.copy(totalPages = count, currentPage = 1, pagePaths = emptyList(), message = null) }
        }
    }

    fun onPageScanned(path: String, onSaved: () -> Unit) {
        val newList = state.value.pagePaths + path
        val total = state.value.totalPages ?: return
        val nextPage = state.value.currentPage + 1
        if (newList.size >= total) {
            savePassport(newList, onSaved)
        } else {
            _state.update { it.copy(pagePaths = newList, currentPage = nextPage, message = null) }
        }
    }

    private fun savePassport(paths: List<String>, onSaved: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val title = "Passeport du ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}"
                createScanDocumentUseCase(
                    CreateScanInput(
                        sourceImagePaths = paths,
                        title = title,
                        type = ScanType.IMAGE,
                        isBatch = true,
                        kind = DocumentKind.PASSPORT
                    )
                )
                _state.update { it.copy(isSaving = false) }
                onSaved()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, message = e.localizedMessage ?: "Erreur lors de l'enregistrement") }
            }
        }
    }
}
