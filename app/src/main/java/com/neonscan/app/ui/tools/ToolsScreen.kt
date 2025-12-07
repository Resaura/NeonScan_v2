package com.neonscan.app.ui.tools

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neonscan.app.presentation.tools.ToolsViewModel
import com.neonscan.app.ui.common.SectionHeader

@Composable
fun ToolsRoute(
    viewModel: ToolsViewModel,
    onSimpleScan: () -> Unit,
    onBatchScan: () -> Unit,
    onIdScan: () -> Unit,
    onPassportScan: () -> Unit,
    onStubSelected: (String) -> Unit
) {
    ToolsScreen(
        onSimpleScan = onSimpleScan,
        onBatchScan = onBatchScan,
        onIdScan = onIdScan,
        onPassportScan = onPassportScan,
        onStubSelected = onStubSelected
    )
}

@Composable
fun ToolsScreen(
    onSimpleScan: () -> Unit,
    onBatchScan: () -> Unit,
    onIdScan: () -> Unit,
    onPassportScan: () -> Unit,
    onStubSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text("Outils", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        SectionHeader(title = "Scanner")
        Spacer(Modifier.height(10.dp))
        ToolCard(
            title = "Simple",
            subtitle = "Numeriser un document unique",
            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onSimpleScan
        )
        ToolCard(
            title = "Lot",
            subtitle = "Numeriser des documents par lot",
            icon = { Icon(Icons.Filled.Collections, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onBatchScan
        )
        ToolCard(
            title = "Carte d'identite",
            subtitle = "Scanner recto/verso d'une piece",
            icon = { Icon(Icons.Filled.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onIdScan
        )
        ToolCard(
            title = "Passeport",
            subtitle = "Scanner une double page centrale",
            icon = { Icon(Icons.Filled.TravelExplore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onPassportScan
        )
        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Convertir")
        Spacer(Modifier.height(10.dp))
        ToolCard(
            title = "Convertir en PDF",
            subtitle = "Depuis une image ou un document",
            icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { onStubSelected("PDF") }
        )
        ToolCard(
            title = "Convertir en DOC",
            subtitle = "Generer un fichier Word simple",
            icon = { Icon(Icons.Filled.TextSnippet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { onStubSelected("DOC") }
        )
        ToolCard(
            title = "Convertir en JPG",
            subtitle = "Transformez vos documents en images JPG.",
            icon = { Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { onStubSelected("IMAGE") }
        )
        ToolCard(
            title = "Convertir en XLS",
            subtitle = "Tableau CSV/XLS...",
            icon = { Icon(Icons.Filled.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { onStubSelected("CSV") }
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                    icon()
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}



