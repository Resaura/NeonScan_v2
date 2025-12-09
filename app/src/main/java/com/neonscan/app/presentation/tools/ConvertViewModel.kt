package com.neonscan.app.presentation.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neonscan.app.data.file.FileStorageManager
import com.neonscan.app.data.file.StoredScanResult
import com.neonscan.app.data.repository.ScanRepository
import com.neonscan.app.domain.model.DocumentKind
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.domain.usecase.GetAllScansUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class ConvertUiState(
    val target: ScanType,
    val documents: List<ScanDocument> = emptyList(),
    val baseDocuments: List<ScanDocument> = emptyList(),
    val filter: ScanType? = null,
    val excludedIds: Set<Long> = emptySet()
)

sealed class ConvertEvent {
    data class Success(val message: String) : ConvertEvent()
    data class Error(val message: String) : ConvertEvent()
}

/**
 * Filtre les documents éligibles pour une conversion vers [target].
 * Exclusion : les fichiers déjà du type cible.
 * Filtrage : si filter != null, on restreint au type choisi (mais jamais le type cible).
 */
@HiltViewModel
class ConvertViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllScansUseCase: GetAllScansUseCase,
    private val repository: ScanRepository,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {
    private val targetType: ScanType =
        mapTarget(savedStateHandle.get<String>("target"))

    private val _state = MutableStateFlow(ConvertUiState(target = targetType))
    val state: StateFlow<ConvertUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ConvertEvent>()
    val events: SharedFlow<ConvertEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            getAllScansUseCase().collectLatest { docs ->
                _state.update { current ->
                    val eligible = docs.filter { it.type != targetType && it.id !in current.excludedIds }
                    current.copy(baseDocuments = eligible, documents = applyFilter(eligible, current.filter, current.excludedIds))
                }
            }
        }
    }

    fun setFilter(filter: ScanType?) {
        _state.update { current ->
            val newList = applyFilter(current.baseDocuments, filter, current.excludedIds)
            current.copy(filter = filter, documents = newList)
        }
    }

    fun convert(documentId: Long, replace: Boolean) {
        val doc = _state.value.documents.find { it.id == documentId } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { performConversion(doc) }
            result.onSuccess {
                _state.update { current ->
                    val newExcluded = current.excludedIds + doc.id
                    val newBase = current.baseDocuments.filterNot { it.id == doc.id }
                    val newDocs = applyFilter(newBase, current.filter, newExcluded)
                    current.copy(excludedIds = newExcluded, baseDocuments = newBase, documents = newDocs)
                }
                if (replace) {
                    runCatching {
                        repository.delete(doc)
                        fileStorageManager.deleteDocument(doc.path)
                    }
                }
                _events.emit(ConvertEvent.Success("Conversion en ${targetType.name} terminee."))
            }.onFailure {
                _events.emit(ConvertEvent.Error("La conversion a echoue. Veuillez reessayer."))
            }
        }
    }

    private fun applyFilter(docs: List<ScanDocument>, filter: ScanType?, excludedIds: Set<Long>): List<ScanDocument> {
        val eligible = docs.filterNot { it.id in excludedIds }
        return if (filter == null) eligible else eligible.filter { it.type == filter }
    }

    private suspend fun performConversion(doc: ScanDocument) {
        val stored: StoredScanResult = when (targetType) {
            ScanType.PDF -> convertToPdf(doc)
            ScanType.IMAGE -> convertToImage(doc)
            ScanType.TEXT -> convertToText(doc)
            ScanType.CSV -> convertToCsv(doc)
        }
        val newDoc = doc.copy(
            id = 0L,
            title = doc.title,
            type = targetType,
            path = stored.primaryPath,
            pageCount = stored.pageCount,
            createdAt = System.currentTimeMillis(),
            kind = doc.kind
        )
        repository.insert(newDoc)
    }

    private suspend fun convertToPdf(doc: ScanDocument): StoredScanResult {
        val bitmap = BitmapFactory.decodeFile(doc.path)
        val dest = fileStorageManager.createEmptyFile("pdf")
        val pdfDocument = PdfDocument()
        if (bitmap != null) {
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
        } else {
            // Fallback: treat DOC/TXT content as text and render to PDF
            val textContent = withContext(Dispatchers.IO) { runCatching { File(doc.path).readText() }.getOrDefault("") }
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4-ish
            val page = pdfDocument.startPage(pageInfo)
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                color = android.graphics.Color.BLACK
            }
            val lines = textContent.lines().ifEmpty { listOf("Document converti") }
            var y = 40f
            val x = 40f
            val lineHeight = paint.textSize * 1.4f
            lines.forEach {
                if (y > pageInfo.pageHeight - 40) {
                    pdfDocument.finishPage(page)
                    val newPage = pdfDocument.startPage(pageInfo)
                    y = 40f
                    lines.dropWhile { false } // no-op, keep rendering
                }
                page.canvas.drawText(it, x, y, paint)
                y += lineHeight
            }
            pdfDocument.finishPage(page)
        }

        withContext(Dispatchers.IO) {
            FileOutputStream(dest).use { output -> pdfDocument.writeTo(output) }
            pdfDocument.close()
        }
        return StoredScanResult(primaryPath = dest.absolutePath, pageCount = 1)
    }

    private suspend fun convertToImage(doc: ScanDocument): StoredScanResult {
        val bitmap = BitmapFactory.decodeFile(doc.path)
            ?: renderPdfFirstPage(doc.path)
            ?: renderTextBitmap(
                withContext(Dispatchers.IO) { runCatching { File(doc.path).readText() }.getOrDefault("") }
                    .ifBlank { "Document converti" }
            )
        return fileStorageManager.saveBitmap(bitmap, "jpg")
    }

    private suspend fun convertToText(doc: ScanDocument): StoredScanResult {
        val content = buildString {
            appendLine("Document converti depuis: ${doc.title}")
            appendLine("Chemin: ${doc.path}")
            appendLine("Date: ${System.currentTimeMillis()}")
        }
        return fileStorageManager.saveBytes(content.toByteArray(), "doc")
    }

    private suspend fun convertToCsv(doc: ScanDocument): StoredScanResult {
        val csv = "Document,Origine,Date\n${doc.title},${doc.path},${System.currentTimeMillis()}"
        return fileStorageManager.saveBytes(csv.toByteArray(), "xls")
    }

    private fun renderPdfFirstPage(path: String): Bitmap? = runCatching {
        val file = File(path)
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount <= 0) return null
                renderer.openPage(0).use { page ->
                    val width = (page.width * 2)
                    val height = (page.height * 2)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }.getOrNull()

    private fun renderTextBitmap(text: String): Bitmap {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 32f
            color = android.graphics.Color.BLACK
        }
        val lines = text.lines().ifEmpty { listOf("Document") }
        val maxWidth = lines.maxOf { paint.measureText(it).toInt().coerceAtLeast(200) }
        val lineHeight = (paint.textSize * 1.5f).toInt()
        val height = (lines.size * lineHeight + 40).coerceAtLeast(200)
        val bmp = Bitmap.createBitmap(maxWidth + 80, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        var y = 40f
        lines.forEach {
            canvas.drawText(it, 40f, y, paint)
            y += lineHeight
        }
        return bmp
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
