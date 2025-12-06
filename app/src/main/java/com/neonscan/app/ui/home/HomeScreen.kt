package com.neonscan.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neonscan.app.core.DateFormatter
import com.neonscan.app.domain.model.Folder
import com.neonscan.app.presentation.home.HomeUiState
import com.neonscan.app.presentation.home.HomeViewModel
import com.neonscan.app.ui.common.PrimaryChip
import com.neonscan.app.ui.common.SectionHeader
import com.neonscan.app.ui.common.extractExtensionFromPath
import com.neonscan.app.ui.common.fileTypeIconForExtension

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onQuickScan: () -> Unit,
    onBatchScan: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onDeleteDocument: (Long) -> Unit = {},
    onAssignToFolder: (Long) -> Unit = { viewModel.showAssignDialog(it) },
    onConfirmAssign: (Long?) -> Unit = { viewModel.assignToFolder(it) },
    onDismissAssign: () -> Unit = { viewModel.showAssignDialog(null) },
    onRemoveFromFolder: (Long) -> Unit = { viewModel.removeFromFolder(it) },
    onEditDocument: (Long) -> Unit = onOpenDocument
) {
    val state by viewModel.state.collectAsState()
    HomeScreen(
        state = state,
        onQuickScan = onQuickScan,
        onBatchScan = onBatchScan,
        onNewClick = onQuickScan,
        onOpenDocument = onOpenDocument,
        onDeleteDocument = onDeleteDocument,
        onAssignToFolder = onAssignToFolder,
        onConfirmAssign = onConfirmAssign,
        onDismissAssign = onDismissAssign,
        onRemoveFromFolder = onRemoveFromFolder,
        onEditDocument = onEditDocument
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQuickScan: () -> Unit,
    onBatchScan: () -> Unit,
    onNewClick: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onDeleteDocument: (Long) -> Unit,
    onAssignToFolder: (Long) -> Unit,
    onConfirmAssign: (Long?) -> Unit,
    onDismissAssign: () -> Unit,
    onRemoveFromFolder: (Long) -> Unit,
    onEditDocument: (Long) -> Unit
) {
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
                Text("Accueil", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Scanner et convertir en un geste.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            PrimaryChip(text = "Nouveau", onClick = onNewClick)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionCard(
                title = "Scan rapide",
                subtitle = "Détection auto + filtres",
                icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 160.dp),
                onClick = onQuickScan
            )
            QuickActionCard(
                title = "Mode lot",
                subtitle = "Enchaîner plusieurs pages",
                icon = { Icon(Icons.Filled.Collections, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 160.dp),
                onClick = onBatchScan
            )
        }
        Spacer(Modifier.height(20.dp))
        SectionHeader(title = "Récents")
        Spacer(Modifier.height(8.dp))

        if (state.showAssignDialogFor != null) {
            AssignFolderDialog(
                folders = state.folders,
                onDismiss = onDismissAssign,
                onAssign = { folderId -> onConfirmAssign(folderId) }
            )
        }

        if (state.recent.isEmpty()) {
            Text(
                "Aucun document scanné pour l'instant",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn {
                items(state.recent) { doc ->
                    HomeRecentItem(
                        title = doc.title,
                        subtitle = "${doc.pageCount} page(s) - ${DateFormatter.format(doc.createdAt)}",
                        icon = fileTypeIconForExtension(extractExtensionFromPath(doc.path)),
                        hasFolder = doc.folderId != null,
                        onOpen = { onOpenDocument(doc.id) },
                        onDelete = { onDeleteDocument(doc.id) },
                        onAssign = { onAssignToFolder(doc.id) },
                        onRemoveFromFolder = { onRemoveFromFolder(doc.id) },
                        onEdit = { onEditDocument(doc.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.padding(bottom = 12.dp)) {
                icon()
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun HomeRecentItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    hasFolder: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onAssign: () -> Unit,
    onRemoveFromFolder: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer") },
            text = { Text("Confirmer la suppression du document ?") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Modifier") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("Supprimer") }, onClick = { expanded = false; confirmDelete = true })
                    val folderActionText = if (hasFolder) "Retirer du dossier" else "Ajouter à un dossier"
                    DropdownMenuItem(
                        text = { Text(folderActionText) },
                        onClick = {
                            expanded = false
                            if (hasFolder) onRemoveFromFolder() else onAssign()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignFolderDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onAssign: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter à un dossier") },
        text = {
            if (folders.isEmpty()) {
                Text("Aucun dossier créé pour l'instant")
            } else {
                Column {
                    folders.forEach { folder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                            onClick = { onAssign(folder.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(folder.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}
