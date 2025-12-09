package com.neonscan.app.ui.files

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import com.neonscan.app.core.DateFormatter
import com.neonscan.app.domain.model.ScanDocument
import com.neonscan.app.domain.model.ScanType
import com.neonscan.app.presentation.files.FilesFilter
import com.neonscan.app.presentation.files.FilesUiState
import com.neonscan.app.presentation.files.FilesViewModel
import com.neonscan.app.ui.common.DocumentListItem
import com.neonscan.app.ui.common.OutlineChip
import com.neonscan.app.ui.common.PrimaryChip
import com.neonscan.app.ui.common.SectionHeader
import com.neonscan.app.ui.common.extractExtensionFromPath
import com.neonscan.app.ui.common.fileTypeIconForExtension

@Composable
fun FilesRoute(
    viewModel: FilesViewModel,
    onNewScan: () -> Unit,
    onOpenDocument: (Long) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var newFolderName by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    FilesScreen(
        state = state,
        onNewScan = onNewScan,
        onOpenDocument = onOpenDocument,
        onDelete = { viewModel.delete(it) },
        onNewFolder = { viewModel.showCreateFolderDialog(true) },
        onCreateFolder = { name -> viewModel.createFolder(name) },
        onShowFilter = { showFilterSheet = true },
        onApplyFilter = { filter -> viewModel.applyFilter(filter) },
        onShowAssign = { id -> viewModel.showAssignDialog(id) },
        onAssignToFolder = { folderId -> viewModel.assignToFolder(folderId) },
        onToggleSelection = { id -> viewModel.toggleSelection(id) },
        onClearSelection = { viewModel.clearSelection() },
        onOpenFolder = { folderId -> viewModel.openFolder(folderId) },
        newFolderName = newFolderName,
        onFolderNameChange = { newFolderName = it },
        onDismissFolderDialog = { viewModel.showCreateFolderDialog(false) },
        showFilterSheet = showFilterSheet,
        onDismissFilter = { showFilterSheet = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesScreen(
    state: FilesUiState,
    onNewScan: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onDelete: (ScanDocument) -> Unit,
    onNewFolder: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onShowFilter: () -> Unit,
    onApplyFilter: (FilesFilter) -> Unit,
    onShowAssign: (Long?) -> Unit,
    onAssignToFolder: (Long?) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onOpenFolder: (Long?) -> Unit,
    newFolderName: String,
    onFolderNameChange: (String) -> Unit,
    onDismissFolderDialog: () -> Unit,
    showFilterSheet: Boolean,
    onDismissFilter: () -> Unit
) {
    if (state.showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = onDismissFolderDialog,
            title = { Text("Nouveau dossier") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = onFolderNameChange,
                    label = { Text("Nom du dossier") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onCreateFolder(newFolderName)
                        onFolderNameChange("")
                    }
                }) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = onDismissFolderDialog) { Text("Annuler") }
            }
        )
    }

    if (state.showAssignDialogFor != null || state.selectedIds.isNotEmpty()) {
        AssignFolderDialog(
            folders = state.folders,
            onDismiss = { onShowAssign(null); onClearSelection() },
            onAssign = { folderId -> onAssignToFolder(folderId) }
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            current = state.filter,
            onApply = { filter ->
                onApplyFilter(filter)
                onDismissFilter()
            },
            onReset = {
                onApplyFilter(FilesFilter())
                onDismissFilter()
            },
            onDismiss = onDismissFilter
        )
    }

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
                Text("Fichiers", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Gérez vos dossiers et exports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            PrimaryChip(text = "Nouveau", onClick = onNewScan)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineChip(text = "Filtrer", onClick = onShowFilter)
            OutlineChip(text = "Nouveau dossier", onClick = onNewFolder)
        }
        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Dossiers")
        Spacer(Modifier.height(8.dp))
        if (state.folders.isEmpty()) {
            Text(
                "Aucun dossier. Créez-en un pour organiser vos fichiers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.folders) { folder ->
                    FolderCard(
                        name = folder.name,
                        documentCount = folder.documentCount,
                        selected = state.currentFolderId == folder.id,
                        onClick = { onOpenFolder(folder.id) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionHeader(title = "Fichiers")
        Spacer(Modifier.height(8.dp))
        if (state.documents.isEmpty()) {
            Text(
                "Aucun document enregistré",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn {
                items(state.documents) { doc ->
                    FileListItemWithMenu(
                        document = doc,
                        subtitle = "${doc.pageCount} page(s) • ${DateFormatter.format(doc.createdAt)}",
                        onOpen = { onOpenDocument(doc.id) },
                        onDelete = { onDelete(doc) },
                        showConfirm = true,
                        onAssign = { onShowAssign(doc.id) },
                        onEdit = { onOpenDocument(doc.id) },
                        onLongPress = { onToggleSelection(doc.id) },
                        selected = state.selectedIds.contains(doc.id)
                    )
                }
            }
            if (state.selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${state.selectedIds.size} sélectionné(s)", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlineChip(text = "Ajouter à un dossier", onClick = { onShowAssign(null) })
                        OutlineChip(text = "Annuler", onClick = onClearSelection)
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCard(name: String, documentCount: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("$documentCount document(s)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItemWithMenu(
    document: ScanDocument,
    subtitle: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onAssign: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
    selected: Boolean,
    showConfirm: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && showConfirm) {
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
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    fileTypeIconForExtension(extractExtensionFromPath(document.path)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        document.title,
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
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { expanded = true }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Modifier") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("Supprimer") }, onClick = { expanded = false; onDelete() })
                    DropdownMenuItem(text = { Text("Ajouter à un dossier") }, onClick = { expanded = false; onAssign() })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    current: FilesFilter,
    onApply: (FilesFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<ScanType?>(current.type) }
    var sortDesc by remember { mutableStateOf(current.sortDescending) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Filtrer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Type", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipOption("Tous", selected = selectedType == null) { selectedType = null }
                    FilterChipOption("PDF", selected = selectedType == ScanType.PDF) { selectedType = ScanType.PDF }
                    FilterChipOption("Image", selected = selectedType == ScanType.IMAGE) { selectedType = ScanType.IMAGE }
                    FilterChipOption("DOC", selected = selectedType == ScanType.TEXT) { selectedType = ScanType.TEXT }
                    FilterChipOption("XLS", selected = selectedType == ScanType.CSV) { selectedType = ScanType.CSV }
                }
                Text("Tri", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipOption("Plus récents", selected = sortDesc) { sortDesc = true }
                    FilterChipOption("Plus anciens", selected = !sortDesc) { sortDesc = false }
                }
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReset) { Text("Réinitialiser") }
                TextButton(onClick = { onApply(FilesFilter(type = selectedType, sortDescending = sortDesc)) }) { Text("Appliquer") }
            }
        }
    )
}

@Composable
private fun FilterChipOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = background,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .clickable { onClick() }
            .padding(0.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AssignFolderDialog(
    folders: List<com.neonscan.app.domain.model.Folder>,
    onDismiss: () -> Unit,
    onAssign: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter à un dossier") },
        text = {
            if (folders.isEmpty()) {
                Text("Aucun dossier créé pour l’instant")
            } else {
                Column {
                    folders.forEach { folder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
