package com.neonscan.app.presentation.tools

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.domain.usecase.GetAllScansUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvertUiState(
    val target: ScanType,
    val documents: List<ScanDocument> = emptyList(),
    val baseDocuments: List<ScanDocument> = emptyList(),
    val filter: ScanType? = null
)

/**
 * Filtre les documents éligibles pour une conversion vers [target].
 * Exclusion : les fichiers déjà du type cible.
 * Filtrage : si filter != null, on restreint au type choisi (mais jamais le type cible).
 */
@HiltViewModel
class ConvertViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllScansUseCase: GetAllScansUseCase
) : ViewModel() {
    private val targetType: ScanType =
        mapTarget(savedStateHandle.get<String>("target"))

    private val _state = MutableStateFlow(ConvertUiState(target = targetType))
    val state: StateFlow<ConvertUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getAllScansUseCase().collectLatest { docs ->
                val eligible = docs.filter { it.type != targetType }
                _state.update { it.copy(baseDocuments = eligible, documents = applyFilter(eligible, it.filter)) }
            }
        }
    }

    fun setFilter(filter: ScanType?) {
        _state.update { current ->
            val newList = applyFilter(current.baseDocuments, filter)
            current.copy(filter = filter, documents = newList)
        }
    }

    private fun applyFilter(docs: List<ScanDocument>, filter: ScanType?): List<ScanDocument> {
        return if (filter == null) docs else docs.filter { it.type == filter }
    }

    private fun mapTarget(raw: String?): ScanType {
        return when (raw?.uppercase()) {
            "PDF" -> ScanType.PDF
            "IMAGE", "JPG", "JPEG", "PNG", "WEBP" -> ScanType.IMAGE
            "DOC", "DOCX", "TXT" -> ScanType.TEXT
            "XLS", "XLSX", "CSV" -> ScanType.CSV
            else -> ScanType.PDF
        }
    }
}
