package com.neonscan.app.ui.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neonscan.app.core.DateFormatter
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.ui.common.extractExtensionFromPath
import com.neonscan.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FileDetailScreen(
    document: ScanDocument?,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onRename: (Long, String) -> Unit = { _, _ -> },
    onEdit: (Long) -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember(document?.title) { mutableStateOf(document?.title.orEmpty()) }
    var pagePaths by remember(document?.path) { mutableStateOf<List<String>>(emptyList()) }
    var currentPageIndex by remember(document?.path) { mutableStateOf(0) }
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap?>() }
    val scrollState = rememberScrollState()
    val maxImageHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.5f).coerceAtLeast(200.dp)
    val density = LocalDensity.current

    LaunchedEffect(document?.path) {
        if (document?.path.isNullOrBlank()) {
            pagePaths = emptyList()
            return@LaunchedEffect
        }
        val path = document?.path ?: return@LaunchedEffect
        val pages = collectPagePaths(path)
        pagePaths = pages
        currentPageIndex = 0
        val first = loadBitmap(path)
        pageBitmaps[0] = first
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer") },
            text = { Text("Voulez-vous vraiment supprimer ce scan ?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete?.invoke() }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") } }
        )
    }

    if (showRenameDialog && document != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renommer") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Titre") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        if (renameValue.isNotBlank()) onRename(document.id, renameValue.trim())
                    }
                ) { Text("Valider") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Annuler") } }
        )
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
        }
        Spacer(Modifier.height(4.dp))
                if (document == null) {
            Text("Document introuvable", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        } else {
            val pageCount = pagePaths.size.coerceAtLeast(1)
            LaunchedEffect(currentPageIndex, pagePaths) {
                val path = pagePaths.getOrNull(currentPageIndex) ?: document.path
                if (pageBitmaps[currentPageIndex] == null && !path.isNullOrBlank()) {
                    pageBitmaps[currentPageIndex] = loadBitmap(path)
                }
            }
            val currentBitmap = pageBitmaps[currentPageIndex]
            currentBitmap?.let { bmp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = maxImageHeight),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val imgModifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = maxImageHeight)
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
                                        accumulated > threshold && currentPageIndex > 0 -> currentPageIndex -= 1
                                        accumulated < -threshold && currentPageIndex < pageCount - 1 -> currentPageIndex += 1
                                    }
                                    accumulated = 0f
                                }
                            )
                        }
                    androidx.compose.foundation.layout.Box(
                        modifier = imgModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Apercu du scan",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(maxImageHeight),
                            contentScale = ContentScale.Fit
                        )
                        if (pageCount > 1) {
                            IconButton(
                                onClick = { if (currentPageIndex > 0) currentPageIndex -= 1 },
                                enabled = currentPageIndex > 0,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Page precedente")
                            }
                            IconButton(
                                onClick = { if (currentPageIndex < pageCount - 1) currentPageIndex += 1 },
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
                }
                Spacer(Modifier.height(16.dp))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            document.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { renameValue = document.title; showRenameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renommer", modifier = Modifier.padding(end = 4.dp))
                            Text("Renommer")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val ext = extractExtensionFromPath(document.path)?.takeIf { it.isNotBlank() }?.uppercase()
                    val typeLabel = if (document.type == ScanType.IMAGE && !ext.isNullOrBlank()) {
                        "Type : IMAGE ($ext)"
                    } else {
                        "Type : ${document.type}"
                    }
                    Text(typeLabel, style = MaterialTheme.typography.bodyMedium)
                    Text("Pages : ${document.pageCount}", style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.file_detail_created_label, DateFormatter.format(document.createdAt)), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onEdit(document.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) { Text("Modifier", color = MaterialTheme.colorScheme.onPrimary) }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Supprimer", color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

private suspend fun loadBitmap(path: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
}

private fun collectPagePaths(primaryPath: String): List<String> {
    val primaryFile = java.io.File(primaryPath)
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





