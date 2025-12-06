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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neonscan.app.data.scanner.DocumentScannerManager
import com.neonscan.app.presentation.tools.IdCardScanViewModel
import com.neonscan.app.presentation.tools.IdCardStep
import kotlinx.coroutines.launch

@Composable
fun IdCardScanScreen(
    scannerManager: DocumentScannerManager,
    onFinished: () -> Unit,
    viewModel: IdCardScanViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as androidx.activity.ComponentActivity
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
                when (state.step) {
                    IdCardStep.FRONT -> viewModel.onFrontScanned(first)
                    IdCardStep.BACK -> viewModel.onBackScanned(first, onFinished)
                    else -> {}
                }
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
            val header = when (state.step) {
                IdCardStep.FRONT -> "Scannez le recto de votre carte d'identité"
                IdCardStep.BACK -> "Scannez le verso de votre carte d'identité"
                IdCardStep.DONE -> "Terminé"
            }
            Text(header, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Placez la carte dans le cadre puis appuyez sur Scanner.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { startScan() }) {
                Text("Scanner")
            }
            Spacer(Modifier.height(12.dp))
            if (state.message != null) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
