package com.neonscan.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.neonscan.app.data.scanner.DocumentScannerManager
import com.neonscan.app.presentation.home.HomeViewModel
import com.neonscan.app.ui.common.NeonScanTheme
import com.neonscan.app.ui.navigation.NavItem
import com.neonscan.app.ui.navigation.NeonNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var documentScannerManager: DocumentScannerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeonScanApp(documentScannerManager = documentScannerManager)
        }
    }
}

@Composable
private fun NeonScanApp(documentScannerManager: DocumentScannerManager) {
    NeonScanTheme {
        val navController = rememberNavController()
        val homeViewModel: HomeViewModel = hiltViewModel()
        val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val activity = LocalContext.current as ComponentActivity
        var pendingBatch: Boolean? by remember { mutableStateOf(null) }
        var activeScanner by remember { mutableStateOf<com.websitebeaver.documentscanner.DocumentScanner?>(null) }
        val noAnimOptions = navOptions {
            anim {
                enter = 0
                exit = 0
                popEnter = 0
                popExit = 0
            }
        }

        val scannerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            activeScanner?.handleDocumentScanIntentResult(result)
        }

        fun launchScanner(isBatch: Boolean) {
            val scanner = documentScannerManager.buildScanner(
                activity = activity,
                allowMultiple = isBatch,
                onSuccess = { paths -> homeViewModel.saveScan(paths, isBatch) },
                onError = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
                onCancel = {}
            )
            activeScanner = scanner
            scannerLauncher.launch(scanner.createDocumentScanIntent())
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            val batch = pendingBatch
            if (granted && batch != null) {
                launchScanner(batch)
            } else if (!granted) {
                scope.launch { snackbarHostState.showSnackbar("La permission camera est requise") }
            }
            pendingBatch = null
        }

        fun requestScan(isBatch: Boolean) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                launchScanner(isBatch)
            } else {
                pendingBatch = isBatch
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                BottomNavigationBar(navController = navController)
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { requestScan(false) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(end = 16.dp, bottom = 16.dp)
                        .height(60.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouveau scan")
                }
            },
            floatingActionButtonPosition = androidx.compose.material3.FabPosition.End
        ) { innerPadding ->
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val tabs = NavItem.bottomItems
            val currentIndex = tabs.indexOfFirst { currentRoute?.startsWith(it.route) == true }.coerceAtLeast(0)
            val density = LocalDensity.current
            val isTabSwipeEnabled = currentRoute?.let { route ->
                tabs.any { route.startsWith(it.route) }
            } ?: false
            fun navigateTo(index: Int) {
                val target = tabs.getOrNull(index) ?: return
                if (currentRoute == target.route) return
                navController.navigate(target.route, noAnimOptions)
            }

            val boxModifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .let { base ->
                    if (!isTabSwipeEnabled) {
                        base
                    } else {
                        base.pointerInput(currentIndex) {
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
                                        accumulated > threshold && currentIndex > 0 -> navigateTo(currentIndex - 1)
                                        accumulated < -threshold && currentIndex < tabs.lastIndex -> navigateTo(currentIndex + 1)
                                    }
                                    accumulated = 0f
                                }
                            )
                        }
                    }
                }

            Box(
                modifier = boxModifier
            ) {
                NeonNavHost(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    onRequestScan = { isBatch -> requestScan(isBatch) },
                    documentScannerManager = documentScannerManager
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: androidx.navigation.NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        NavItem.bottomItems.forEach { item ->
            val selected = currentRoute?.startsWith(item.route) == true
            val bg = if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else Color.Transparent
            val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            androidx.compose.material3.Surface(
                color = bg,
                contentColor = contentColor,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = if (selected) 2.dp else 0.dp,
                modifier = Modifier
                    .height(54.dp)
                    .weight(1f)
                    .clickable {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route)
                        }
                    }
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = contentColor)
                    Text(item.label, color = contentColor)
                }
            }
        }
    }
}
