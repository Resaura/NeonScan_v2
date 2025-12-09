package com.neonscan.app.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.presentation.tools.ConvertEvent
import com.neonscan.app.presentation.tools.ConvertViewModel
import com.neonscan.app.ui.common.DocumentListItem
import com.neonscan.app.ui.common.OutlineChip
import com.neonscan.app.ui.common.extractExtensionFromPath
import com.neonscan.app.ui.common.fileTypeIconForExtension
import kotlinx.coroutines.launch

@Composable
fun ConvertScreen(
    target: ScanType,
    viewModel: ConvertViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState().value
    var showFilter by remember { mutableStateOf(false) }
    var pendingDocId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConvertEvent.Success -> scope.launch { snackbarHostState.showSnackbar(event.message) }
                is ConvertEvent.Error -> scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        convertTitle(target),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Selectionnez un fichier a convertir",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filtrer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showFilter = true }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.documents.isEmpty()) {
                Text(
                    "Aucun fichier a convertir dans ce format.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                LazyColumn {
                    items(state.documents) { doc ->
                        DocumentListItem(
                            title = doc.title,
                            subtitle = pageLabel(doc.pageCount),
                            leadingIcon = fileTypeIconForExtension(extractExtensionFromPath(doc.path)),
                            onClick = { pendingDocId = doc.id }
                        )
                    }
                }
            }
        }
    }

    if (showFilter) {
        ConvertFilterDialog(
            target = target,
            selected = state.filter,
            onDismiss = { showFilter = false },
            onSelect = {
                viewModel.setFilter(if (it == target) null else it)
                showFilter = false
            }
        )
    }

    pendingDocId?.let { docId ->
        AlertDialog(
            onDismissRequest = { pendingDocId = null },
            title = { Text("Conversion du fichier") },
            text = { Text("Voulez-vous ${convertTitle(target)} en le dupliquant ou en le remplaçant ?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDocId = null
                    viewModel.convert(docId, replace = false)
                }) { Text("Dupliquer") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pendingDocId = null }) { Text("Annuler") }
                    TextButton(onClick = {
                        pendingDocId = null
                        viewModel.convert(docId, replace = true)
                    }) { Text("Remplacer") }
                }
            }
        )
    }
}

@Composable
private fun ConvertFilterDialog(
    target: ScanType,
    selected: ScanType?,
    onDismiss: () -> Unit,
    onSelect: (ScanType?) -> Unit
) {
    val options = ScanType.values().filter { it != target }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineChip(text = "Tous", onClick = { onSelect(null) })
                options.forEach { type ->
                    OutlineChip(
                        text = type.name,
                        onClick = { onSelect(type) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

private fun convertTitle(target: ScanType): String = when (target) {
    ScanType.PDF -> "Convertir en PDF"
    ScanType.IMAGE -> "Convertir en JPG"
    ScanType.TEXT -> "Convertir en DOC"
    ScanType.CSV -> "Convertir en XLS"
}

private fun pageLabel(count: Int): String =
    if (count == 1) "1 page" else "$count pages"

