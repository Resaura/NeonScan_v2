package com.neonscan.app.ui.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

@Composable
fun FileEditScreen(
    document: ScanDocument?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var pagePaths by remember(document?.path) { mutableStateOf<List<String>>(emptyList()) }
    var currentPageIndex by remember(document?.path) { mutableStateOf(0) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val pageStates = remember { mutableStateMapOf<Int, PageEditState>() }

    suspend fun loadPageState(index: Int): PageEditState? {
        val path = pagePaths.getOrNull(index) ?: return null
        val bmp = loadBitmap(path)
        return bmp?.let { PageEditState(base = it, preview = it) }
    }

    suspend fun ensurePage(index: Int) {
        if (pageStates.containsKey(index)) return
        val state = loadPageState(index)
        if (state != null) {
            pageStates[index] = state
        } else {
            showError = true
        }
    }

    suspend fun loadPageForIndex(index: Int) {
        if (index !in pagePaths.indices) return
        ensurePage(index)
        currentPageIndex = index
    }

    fun updatePage(index: Int, transform: (PageEditState) -> PageEditState) {
        val current = pageStates[index] ?: return
        val updated = transform(current)
        val base = updated.base ?: return
        scope.launch(Dispatchers.Default) {
            val preview = applyPipeline(
                source = base,
                rotation = updated.rotation,
                colorMode = updated.colorMode,
                brightness = updated.brightness,
                contrast = updated.contrast
            )
            withContext(Dispatchers.Main) {
                pageStates[index] = updated.copy(preview = preview)
            }
        }
    }

    LaunchedEffect(document?.path) {
        val path = document?.path
        if (path.isNullOrBlank()) {
            pagePaths = emptyList()
            showError = true
            return@LaunchedEffect
        }
        val pages = collectPagePaths(path)
        pagePaths = pages
        currentPageIndex = 0
        ensurePage(0)
    }

    if (showError && (pageStates[currentPageIndex]?.preview == null)) {
        AlertDialog(
            onDismissRequest = { showError = false; onBack() },
            title = { Text("Erreur") },
            text = { Text("Impossible de charger ce scan.") },
            confirmButton = { TextButton(onClick = { showError = false; onBack() }) { Text("Fermer") } }
        )
        return
    }

    if (document == null || document.type != ScanType.IMAGE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Apercu indisponible pour ce type de document.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Retour") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (hasPendingChanges(pageStates)) {
                    showUnsavedDialog = true
                } else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text("Modifier le scan", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))

        val currentState = pageStates[currentPageIndex]
        val bitmap = currentState?.preview
        if (bitmap == null) {
            Text("Chargement de l'Apercu...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Retour") }
        } else {
            val pageCount = pagePaths.size.coerceAtLeast(1)
            val density = LocalDensity.current
            val imageHeight = (LocalConfiguration.current.screenHeightDp * 0.75f).dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = imageHeight)
                    .background(Color.Transparent)
                    .pointerInput(pagePaths, currentPageIndex) {
                        val threshold = with(density) { 48.dp.toPx() }
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                accumulated += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                when {
                                    accumulated > threshold && currentPageIndex > 0 -> {
                                        scope.launch { loadPageForIndex(currentPageIndex - 1) }
                                    }
                                    accumulated < -threshold && currentPageIndex < pageCount - 1 -> {
                                        scope.launch { loadPageForIndex(currentPageIndex + 1) }
                                    }
                                }
                                accumulated = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Apercu",
                    modifier = Modifier
                            .fillMaxWidth()
                            .height(imageHeight)
                            .graphicsLayer(rotationZ = currentState.rotation),
                        contentScale = ContentScale.Fit
                    )
                    if (pageCount > 1) {
                        IconButton(
                        onClick = { scope.launch { loadPageForIndex(currentPageIndex - 1) } },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                        ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Page precedente")
                    }
                    IconButton(
                        onClick = { scope.launch { loadPageForIndex(currentPageIndex + 1) } },
                        enabled = currentPageIndex < pageCount - 1,
                        modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Page suivante")
                    }
                    Text(
                        "${currentPageIndex + 1}/$pageCount",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { updatePage(currentPageIndex) { it.copy(rotation = (it.rotation - 90f) % 360f) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotation gauche")
                }
                IconButton(
                    onClick = { updatePage(currentPageIndex) { it.copy(rotation = (it.rotation + 90f) % 360f) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotation droite")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Mode", color = MaterialTheme.colorScheme.onSurface)
                ModeButton("Couleur", selected = currentState.colorMode == ColorMode.COLOR) { updatePage(currentPageIndex) { it.copy(colorMode = ColorMode.COLOR) } }
                ModeButton("Gris", selected = currentState.colorMode == ColorMode.GRAYSCALE) { updatePage(currentPageIndex) { it.copy(colorMode = ColorMode.GRAYSCALE) } }
                ModeButton("Noir & blanc", selected = currentState.colorMode == ColorMode.HIGH_CONTRAST) { updatePage(currentPageIndex) { it.copy(colorMode = ColorMode.HIGH_CONTRAST) } }
            }
            Spacer(Modifier.height(8.dp))
            Text("Luminosite", color = MaterialTheme.colorScheme.onSurface)
            Slider(
                value = currentState.brightness,
                onValueChange = { value -> updatePage(currentPageIndex) { it.copy(brightness = value) } },
                valueRange = -0.5f..0.5f
            )
            Text("Contraste", color = MaterialTheme.colorScheme.onSurface)
            Slider(
                value = currentState.contrast,
                onValueChange = { value -> updatePage(currentPageIndex) { it.copy(contrast = value) } },
                valueRange = 0.5f..1.5f
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch(Dispatchers.IO) {
                        pagePaths.forEachIndexed { index, path ->
                            ensurePage(index)
                            val state = pageStates[index]
                            val base = state?.base ?: loadBitmap(path)
                            if (base != null && state != null) {
                                val processed = applyPipeline(
                                    source = base,
                                    rotation = state.rotation,
                                    colorMode = state.colorMode,
                                    brightness = state.brightness,
                                    contrast = state.contrast
                                )
                                saveBitmapToPath(processed, path)
                                withContext(Dispatchers.Main) {
                                    pageStates[index] = state.copy(preview = processed, base = processed)
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            isSaving = false
                            onSaved()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Enregistrement..." else "Enregistrer", color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                val state = pageStates[currentPageIndex] ?: return@TextButton
                val base = state.base
                if (base != null) {
                    pageStates[currentPageIndex] = state.copy(
                        rotation = 0f,
                        brightness = 0f,
                        contrast = 1f,
                        colorMode = ColorMode.COLOR,
                        preview = base
                    )
                }
            }) {
                Text("Reinitialiser toutes les modifications")
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Quitter sans enregistrer ?") },
            text = { Text("Les modifications non enregistrees seront perdues.") },
            confirmButton = { TextButton(onClick = { showUnsavedDialog = false; onBack() }) { Text("Quitter") } },
            dismissButton = { TextButton(onClick = { showUnsavedDialog = false }) { Text("Annuler") } }
        )
    }
}

private fun hasPendingChanges(states: Map<Int, PageEditState>): Boolean =
    states.values.any {
        it.rotation != 0f ||
            it.brightness != 0f ||
            it.contrast != 1f ||
            it.colorMode != ColorMode.COLOR
    }

private fun applyPipeline(
    source: Bitmap,
    rotation: Float,
    colorMode: ColorMode,
    brightness: Float,
    contrast: Float
): Bitmap {
    val matrix = Matrix().apply { postRotate(rotation) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    val adjusted = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(rotated.width * rotated.height)
    rotated.getPixels(pixels, 0, rotated.width, 0, 0, rotated.width, rotated.height)
    val brightnessOffset = (brightness * 255).toInt()
    val contrastFactor = contrast.coerceIn(0.5f, 1.5f)

    for (i in pixels.indices) {
        var r = (pixels[i] shr 16) and 0xFF
        var g = (pixels[i] shr 8) and 0xFF
        var b = pixels[i] and 0xFF

        when (colorMode) {
            ColorMode.GRAYSCALE -> {
                val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
                r = gray; g = gray; b = gray
            }
            ColorMode.HIGH_CONTRAST -> {
                val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
                val boosted = if (gray > 128) 255 else 0
                r = boosted; g = boosted; b = boosted
            }
            else -> Unit
        }

        r = ((r - 128) * contrastFactor + 128 + brightnessOffset).toInt()
        g = ((g - 128) * contrastFactor + 128 + brightnessOffset).toInt()
        b = ((b - 128) * contrastFactor + 128 + brightnessOffset).toInt()

        r = r.coerceIn(0, 255)
        g = g.coerceIn(0, 255)
        b = b.coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    adjusted.setPixels(pixels, 0, rotated.width, 0, 0, rotated.width, rotated.height)
    return adjusted
}

private fun saveBitmapToPath(bitmap: Bitmap, path: String) {
    val file = File(path)
    val format = when (file.extension.lowercase()) {
        "png" -> Bitmap.CompressFormat.PNG
        else -> Bitmap.CompressFormat.JPEG
    }
    FileOutputStream(file).use { out ->
        bitmap.compress(format, 95, out)
    }
}

private data class PageEditState(
    val base: Bitmap? = null,
    val preview: Bitmap? = null,
    val rotation: Float = 0f,
    val colorMode: ColorMode = ColorMode.COLOR,
    val brightness: Float = 0f,
    val contrast: Float = 1f
)

private enum class ColorMode { COLOR, GRAYSCALE, HIGH_CONTRAST }

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = MaterialTheme.colorScheme.primary
    val selectedColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White
    )
    val ghostColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = Color.White
    )
    OutlinedButton(
        onClick = onClick,
        colors = if (selected) selectedColors else ghostColors,
        border = if (selected) null else BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White)
    }
}

private suspend fun loadBitmap(path: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
}

private fun collectPagePaths(primaryPath: String): List<String> {
    val primaryFile = File(primaryPath)
    val parent = primaryFile.parentFile ?: return listOf(primaryPath)
    val allowed = setOf("jpg", "jpeg", "png", "webp")
    val regex = Regex("page_(\\d+)", RegexOption.IGNORE_CASE)
    val files = parent.listFiles()
        ?.filter { it.isFile && allowed.contains(it.extension.lowercase()) }
        ?.sortedWith(
            compareBy(
                { regex.find(it.name)?.groups?.get(1)?.value?.toIntOrNull() ?: Int.MAX_VALUE },
                { it.name }
            )
        )
        ?.map { it.absolutePath }
        ?: emptyList()
    return if (files.isNotEmpty()) files else listOf(primaryPath)
}




