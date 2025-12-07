package com.neonscan.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.neonscan.app.presentation.account.AccountViewModel
import com.neonscan.app.presentation.files.FilesViewModel
import com.neonscan.app.presentation.home.HomeViewModel
import com.neonscan.app.presentation.tools.ToolsViewModel
import com.neonscan.app.ui.account.AccountRoute
import com.neonscan.app.ui.files.FileDetailScreen
import com.neonscan.app.ui.files.FileEditScreen
import com.neonscan.app.ui.files.FilesRoute
import com.neonscan.app.ui.home.HomeRoute
import com.neonscan.app.ui.tools.ToolStubScreen
import com.neonscan.app.ui.tools.ToolsRoute
import com.neonscan.app.ui.tools.IdCardScanScreen
import com.neonscan.app.ui.tools.PassportScanScreen
import com.neonscan.app.data.scanner.DocumentScannerManager
import com.neonscan.app.ui.tools.ConvertScreen
import com.neonscan.app.domain.model.ScanType

@Composable
fun NeonNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
    onRequestScan: (Boolean) -> Unit,
    documentScannerManager: DocumentScannerManager
) {
    NavHost(
        navController = navController,
        startDestination = NavItem.Home.route,
        modifier = modifier
    ) {
        composable(NavItem.Home.route) {
            HomeRoute(
                viewModel = homeViewModel,
                onQuickScan = { onRequestScan(false) },
                onBatchScan = { onRequestScan(true) },
                onOpenDocument = { id -> navController.navigate("file_detail/$id") }
            )
        }
        composable(NavItem.Files.route) {
            val vm: FilesViewModel = hiltViewModel()
            FilesRoute(
                viewModel = vm,
                onNewScan = { onRequestScan(false) },
                onOpenDocument = { id -> navController.navigate("file_detail/$id") }
            )
        }
        composable(
            route = "file_detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val vm: FilesViewModel = hiltViewModel()
            val state = vm.state.collectAsState()
            val id = backStack.arguments?.getLong("id") ?: -1L
            vm.loadDetail(id)
            FileDetailScreen(
                document = state.value.selected,
                onBack = { navController.popBackStack() },
                onDelete = {
                    state.value.selected?.let { doc ->
                        vm.delete(doc)
                        navController.popBackStack()
                    }
                },
                onRename = { docId, title -> vm.renameDocument(docId, title) },
                onEdit = { navController.navigate("file_edit/$it") }
            )
        }
        composable(
            route = "file_edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStack ->
            val vm: FilesViewModel = hiltViewModel()
            val id = backStack.arguments?.getLong("id") ?: -1L
            vm.loadDetail(id)
            val state = vm.state.collectAsState()
            FileEditScreen(
                document = state.value.selected,
                onBack = { navController.popBackStack() },
                onSaved = {
                    vm.loadDetail(id)
                    navController.popBackStack()
                }
            )
        }
        composable(NavItem.Tools.route) {
            val vm: ToolsViewModel = hiltViewModel()
            ToolsRoute(
                viewModel = vm,
                onSimpleScan = { onRequestScan(false) },
                onBatchScan = { onRequestScan(true) },
                onIdScan = { navController.navigate("id_card_scan") },
                onPassportScan = { navController.navigate("passport_scan") },
                onStubSelected = { targetName ->
                    val encoded = targetName.toUri().toString()
                    navController.navigate("convert/$encoded")
                }
            )
        }
        composable(
            route = "tool_stub/{title}",
            arguments = listOf(navArgument("title") { type = NavType.StringType })
        ) { backStack ->
            val encoded = backStack.arguments?.getString("title") ?: ""
            ToolStubScreen(title = android.net.Uri.decode(encoded))
        }
        composable(NavItem.Account.route) {
            val vm: AccountViewModel = hiltViewModel()
            AccountRoute(viewModel = vm)
        }
        composable("id_card_scan") {
            IdCardScanScreen(scannerManager = documentScannerManager, onFinished = { navController.popBackStack() })
        }
        composable("passport_scan") {
            PassportScanScreen(scannerManager = documentScannerManager, onFinished = { navController.popBackStack() })
        }
        composable(
            route = "convert/{target}",
            arguments = listOf(navArgument("target") { type = NavType.StringType })
        ) { backStack ->
            val targetName = backStack.arguments?.getString("target") ?: ScanType.PDF.name
            val target = runCatching { ScanType.valueOf(targetName) }.getOrDefault(ScanType.PDF)
            ConvertScreen(
                target = target
            )
        }
    }
}

