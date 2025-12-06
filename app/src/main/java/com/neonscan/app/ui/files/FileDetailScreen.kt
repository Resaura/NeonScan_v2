package com.neonscan.app.ui.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.neonscan.app.core.DateFormatter
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.ui.common.extractExtensionFromPath
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
    var imageBitmap by remember(document?.path) { mutableStateOf<Bitmap?>(null) }
    val scrollState = rememberScrollState()
    val maxImageHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.5f).coerceAtLeast(200.dp)

    LaunchedEffect(document?.path) {
        if (document?.path.isNullOrBlank()) {
            imageBitmap = null
            return@LaunchedEffect
        }
        val path = document?.path ?: return@LaunchedEffect
        imageBitmap = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
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
            imageBitmap?.let { bmp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = maxImageHeight),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Aperçu du scan",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        contentScale = ContentScale.FillWidth
                    )
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
                    Text("Créé le : ${DateFormatter.format(document.createdAt)}", style = MaterialTheme.typography.bodyMedium)
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
