package com.neonscan.app.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.presentation.tools.ConvertViewModel
import com.neonscan.app.ui.common.DocumentListItem
import com.neonscan.app.ui.common.OutlineChip
import com.neonscan.app.ui.common.extractExtensionFromPath
import com.neonscan.app.ui.common.fileTypeIconForExtension

@Composable
fun ConvertScreen(
    target: ScanType,
    onOpen: (Long) -> Unit,
    viewModel: ConvertViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState().value
    var showFilter by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Convertir en ${target.name}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("Sélectionnez un fichier à convertir", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
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
            Text("Aucun fichier à convertir dans ce format.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        } else {
            LazyColumn {
                items(state.documents) { doc ->
                    DocumentListItem(
                        title = doc.title,
                        subtitle = "${doc.pageCount} page(s)",
                        leadingIcon = fileTypeIconForExtension(extractExtensionFromPath(doc.path)),
                        onClick = { onOpen(doc.id) }
                    )
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
