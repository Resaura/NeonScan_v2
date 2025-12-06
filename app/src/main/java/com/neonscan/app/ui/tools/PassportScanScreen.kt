package com.neonscan.app.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neonscan.app.data.scanner.DocumentScannerManager
import com.neonscan.app.presentation.tools.PassportScanViewModel
import kotlinx.coroutines.launch

@Composable
fun PassportScanScreen(
    scannerManager: DocumentScannerManager,
    onFinished: () -> Unit,
    viewModel: PassportScanViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as androidx.activity.ComponentActivity
    var pagesInput by remember { mutableStateOf(TextFieldValue("2")) }
    var activeScanner by remember { mutableStateOf<com.websitebeaver.documentscanner.DocumentScanner?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        activeScanner?.handleDocumentScanIntentResult(result)
    }

    fun startScan() {
        val scanner = scannerManager.buildScanner(
            activity = activity,
            allowMultiple = false,
            onSuccess = { paths ->
                val first = paths.firstOrNull() ?: return@buildScanner
                viewModel.onPageScanned(first, onFinished)
            },
            onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            onCancel = { }
        )
        activeScanner = scanner
        launcher.launch(scanner.createDocumentScanIntent())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (state.totalPages == null) {
                Text("Combien de pages souhaitez-vous scanner ?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pagesInput,
                    onValueChange = { pagesInput = it },
                    label = { Text("Nombre de pages") }
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    val count = pagesInput.text.toIntOrNull() ?: 0
                    if (count > 0) viewModel.setTotalPages(count)
                    else scope.launch { snackbarHostState.showSnackbar("Nombre invalide") }
                }) { Text("Valider") }
            } else {
                val total = state.totalPages ?: 0
                Text("Page ${state.currentPage} / $total du passeport", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text("Appuyez sur scanner pour capturer cette page.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.totalPages != null) {
                Button(onClick = { startScan() }) { Text("Scanner la page ${state.currentPage}") }
            }
            if (state.message != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
