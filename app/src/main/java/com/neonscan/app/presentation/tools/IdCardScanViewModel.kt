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

enum class IdCardStep { FRONT, BACK, DONE }

data class IdCardUiState(
    val step: IdCardStep = IdCardStep.FRONT,
    val frontPath: String? = null,
    val backPath: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class IdCardScanViewModel @Inject constructor(
    private val createScanDocumentUseCase: CreateScanDocumentUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(IdCardUiState())
    val state: StateFlow<IdCardUiState> = _state.asStateFlow()

    fun onFrontScanned(path: String) {
        _state.update { it.copy(frontPath = path, step = IdCardStep.BACK, message = null) }
    }

    fun onBackScanned(path: String, onSaved: () -> Unit) {
        _state.update { it.copy(backPath = path, message = null) }
        saveIfReady(onSaved)
    }

    private fun saveIfReady(onSaved: () -> Unit) {
        val front = state.value.frontPath
        val back = state.value.backPath
        if (front != null && back != null) {
            viewModelScope.launch {
                _state.update { it.copy(isSaving = true) }
                try {
                    val title = "Carte d'identité du ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}"
                    createScanDocumentUseCase(
                        CreateScanInput(
                            sourceImagePaths = listOf(front, back),
                            title = title,
                            type = ScanType.IMAGE,
                            isBatch = true,
                            kind = DocumentKind.ID_CARD
                        )
                    )
                    _state.update { it.copy(step = IdCardStep.DONE) }
                    onSaved()
                } catch (e: Exception) {
                    _state.update { it.copy(message = e.localizedMessage ?: "Erreur lors de l'enregistrement", isSaving = false) }
                }
            }
        }
    }
}
