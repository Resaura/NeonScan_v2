package com.neonscan.app.ui.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun FileEditScreen(
    document: ScanDocument?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var originalBitmap by remember(document?.path) { mutableStateOf<Bitmap?>(null) }
    var bitmap by remember(document?.path) { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableStateOf(0f) }
    var cropRect by remember { mutableStateOf(CropRect()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var colorMode by remember { mutableStateOf(ColorMode.COLOR) }
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var previewBitmap by remember(document?.path) { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(document?.path) {
        val path = document?.path
        if (path.isNullOrBlank()) {
            originalBitmap = null
            bitmap = null
            showError = true
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
        originalBitmap = loaded
        bitmap = loaded
        showError = loaded == null
        cropRect = CropRect()
        rotation = 0f
        brightness = 0f
        contrast = 1f
        colorMode = ColorMode.COLOR
        previewBitmap = loaded
    }

    LaunchedEffect(originalBitmap, cropRect, rotation, brightness, contrast, colorMode) {
        val src = originalBitmap ?: return@LaunchedEffect
        previewBitmap = withContext(Dispatchers.Default) {
            applyEdits(
                source = src,
                rotation = rotation,
                cropRect = cropRect,
                brightness = brightness,
                contrast = contrast,
                colorMode = colorMode
            )
        }
    }

    if (showError && (originalBitmap == null || previewBitmap == null)) {
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
                "Aperçu indisponible pour ce type de document.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Retour") }
        }
        return
    }

    val currentBitmap = bitmap

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
                if (hasPendingChanges(originalBitmap, bitmap, rotation, brightness, contrast, colorMode, cropRect)) {
                    showUnsavedDialog = true
                } else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text("Modifier le scan", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))

        if (currentBitmap == null || previewBitmap == null) {
            Text(
                "Chargement de l'aperçu...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Retour") }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(currentBitmap.width.toFloat() / currentBitmap.height.toFloat(), matchHeightConstraintsFirst = false)
                        .heightIn(min = 220.dp, max = 420.dp)
                        .onGloballyPositioned { coords -> imageSize = coords.size }
                ) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Aperçu éditable",
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(rotationZ = rotation),
                        contentScale = ContentScale.Crop
                    )
                    CropOverlay(
                        cropRect = cropRect,
                        onUpdate = { cropRect = it },
                        containerSize = imageSize
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { rotation = (rotation - 90f) % 360f }) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotation gauche")
                }
                IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotation droite")
                }
                TextButton(onClick = { cropRect = CropRect() }) { Text("Réinitialiser le cadrage") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Mode")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("Couleur", selected = colorMode == ColorMode.COLOR) { colorMode = ColorMode.COLOR }
                    ModeButton("Gris", selected = colorMode == ColorMode.GRAYSCALE) { colorMode = ColorMode.GRAYSCALE }
                    ModeButton("Noir & blanc", selected = colorMode == ColorMode.HIGH_CONTRAST) { colorMode = ColorMode.HIGH_CONTRAST }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Luminosité")
            Slider(
                value = brightness,
                onValueChange = { brightness = it },
                valueRange = -0.5f..0.5f
            )
            Text("Contraste")
            Slider(
                value = contrast,
                onValueChange = { contrast = it },
                valueRange = 0.5f..1.5f
            )
            Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val path = document.path
                if (previewBitmap != null && originalBitmap != null && !isSaving && path.isNotBlank()) {
                    isSaving = true
                    scope.launch(Dispatchers.IO) {
                        saveBitmapToPath(previewBitmap!!, path)
                        withContext(Dispatchers.Main) {
                            isSaving = false
                            onSaved()
                        }
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
                cropRect = CropRect()
                rotation = 0f
                brightness = 0f
                contrast = 1f
                colorMode = ColorMode.COLOR
                bitmap = originalBitmap
            }) {
                Text("Réinitialiser toutes les modifications")
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Quitter sans enregistrer ?") },
            text = { Text("Les modifications non enregistrées seront perdues.") },
            confirmButton = { TextButton(onClick = { showUnsavedDialog = false; onBack() }) { Text("Quitter") } },
            dismissButton = { TextButton(onClick = { showUnsavedDialog = false }) { Text("Annuler") } }
        )
    }
}

private fun hasPendingChanges(
    original: Bitmap?,
    current: Bitmap?,
    rotation: Float,
    brightness: Float,
    contrast: Float,
    colorMode: ColorMode,
    cropRect: CropRect
): Boolean {
    if (original == null || current == null) return false
    if (rotation != 0f) return true
    if (brightness != 0f) return true
    if (contrast != 1f) return true
    if (colorMode != ColorMode.COLOR) return true
    if (cropRect != CropRect()) return true
    return false
}

private fun applyEdits(
    source: Bitmap,
    rotation: Float,
    cropRect: CropRect,
    brightness: Float,
    contrast: Float,
    colorMode: ColorMode
): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(rotation)
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

    val left = (rotated.width * cropRect.left).toInt().coerceIn(0, rotated.width - 1)
    val top = (rotated.height * cropRect.top).toInt().coerceIn(0, rotated.height - 1)
    val width = (rotated.width * cropRect.width).toInt().coerceAtLeast(1).coerceAtMost(rotated.width - left)
    val height = (rotated.height * cropRect.height).toInt().coerceAtLeast(1).coerceAtMost(rotated.height - top)
    val cropped = Bitmap.createBitmap(rotated, left, top, width, height)

    val adjusted = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(cropped.width * cropped.height)
    cropped.getPixels(pixels, 0, cropped.width, 0, 0, cropped.width, cropped.height)
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
    adjusted.setPixels(pixels, 0, cropped.width, 0, 0, cropped.width, cropped.height)
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

private data class CropRect(
    val left: Float = 0.1f,
    val top: Float = 0.1f,
    val width: Float = 0.8f,
    val height: Float = 0.8f
)

private enum class ColorMode { COLOR, GRAYSCALE, HIGH_CONTRAST }

@Composable
private fun CropOverlay(
    cropRect: CropRect,
    onUpdate: (CropRect) -> Unit,
    containerSize: IntSize
) {
    val handleRadius = 10.dp
    val overlayColor = Color.Black.copy(alpha = 0.35f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cropRect, containerSize) {
                detectDragGestures { change, dragAmount ->
                    change.consumeAllChanges()
                    if (containerSize.width == 0 || containerSize.height == 0) return@detectDragGestures
                    val dx = dragAmount.x / containerSize.width.toFloat()
                    val dy = dragAmount.y / containerSize.height.toFloat()
                    val centerX = cropRect.left + cropRect.width / 2f
                    val centerY = cropRect.top + cropRect.height / 2f
                    val touchX = change.position.x / containerSize.width.toFloat()
                    val touchY = change.position.y / containerSize.height.toFloat()
                    val distanceToCenter = kotlin.math.abs(touchX - centerX) + kotlin.math.abs(touchY - centerY)
                    val threshold = 0.15f
                    val isCenterDrag = distanceToCenter < threshold
                    if (isCenterDrag) {
                        val newLeft = (cropRect.left + dx).coerceIn(0f, 1f - cropRect.width)
                        val newTop = (cropRect.top + dy).coerceIn(0f, 1f - cropRect.height)
                        onUpdate(cropRect.copy(left = newLeft, top = newTop))
                    } else {
                        val leftEdge = (cropRect.left + dx).coerceIn(0f, 1f)
                        val topEdge = (cropRect.top + dy).coerceIn(0f, 1f)
                        val rightEdge = (leftEdge + cropRect.width).coerceIn(0.2f, 1f)
                        val bottomEdge = (topEdge + cropRect.height).coerceIn(0.2f, 1f)
                        val newW = (rightEdge - leftEdge).coerceIn(0.2f, 1f)
                        val newH = (bottomEdge - topEdge).coerceIn(0.2f, 1f)
                        val clampedLeft = leftEdge.coerceIn(0f, 1f - newW)
                        val clampedTop = topEdge.coerceIn(0f, 1f - newH)
                        onUpdate(cropRect.copy(left = clampedLeft, top = clampedTop, width = newW, height = newH))
                    }
                }
            }
    ) {
        val leftPx = cropRect.left * size.width
        val topPx = cropRect.top * size.height
        val widthPx = cropRect.width * size.width
        val heightPx = cropRect.height * size.height

        // Zones d'ombre autour du cadre pour laisser l'image visible à l'intérieur
        drawRect(color = overlayColor, size = Size(size.width, topPx)) // haut
        drawRect(color = overlayColor, topLeft = Offset(0f, topPx + heightPx), size = Size(size.width, size.height - (topPx + heightPx))) // bas
        drawRect(color = overlayColor, topLeft = Offset(0f, topPx), size = Size(leftPx, heightPx)) // gauche
        drawRect(color = overlayColor, topLeft = Offset(leftPx + widthPx, topPx), size = Size(size.width - (leftPx + widthPx), heightPx)) // droite

        drawRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(leftPx, topPx),
            size = Size(widthPx, heightPx),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
        val handles = listOf(
            Offset(leftPx, topPx),
            Offset(leftPx + widthPx, topPx),
            Offset(leftPx, topPx + heightPx),
            Offset(leftPx + widthPx, topPx + heightPx)
        )
        handles.forEach { pos ->
            drawCircle(
                color = Color.White,
                radius = handleRadius.toPx(),
                center = pos
            )
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        colors = if (selected) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        } else {
            ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onPrimary)
        },
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label)
    }
}
