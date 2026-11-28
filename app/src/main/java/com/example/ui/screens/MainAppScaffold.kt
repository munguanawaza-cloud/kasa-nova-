package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.ui.AppLanguage
import com.example.ui.ArabicStrings
import com.example.ui.FrenchStrings
import com.example.ui.KasaViewModel
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.KasaNovaTheme
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmeraldDark
import com.example.ui.theme.kasaAmbientBackgroundBrush
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

object KasaRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_NEW_SALE = "new_sale"
    const val ROUTE_INVENTORY = "inventory"
    const val ROUTE_COLLECTIONS = "collections"
    const val ROUTE_CRM = "crm"
    const val ROUTE_CASH_VAULT = "cash_vault"
    const val ROUTE_CHAT = "chat"
    const val ROUTE_MAP = "map"
    const val ROUTE_AI = "ai_assistant"
    const val ROUTE_CASH_FLOW = "cash_flow"
    const val ROUTE_LEAD_FINDER = "lead_finder"
}

@Composable
fun AppNavigation() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { /* handled reactively by authStateFlow */ }
        )
    } else {
        MainAppScaffold(
            onSignOut = {
                signOutUser(
                    credentialManager = credentialManager,
                    scope = scope
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    onSignOut: () -> Unit,
    kasaViewModel: KasaViewModel = viewModel()
) {
    val state by kasaViewModel.uiState.collectAsStateWithLifecycle()
    val strings = if (state.language == AppLanguage.AR) ArabicStrings else FrenchStrings
    val layoutDir = if (state.language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: KasaRoutes.ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.bannerMessage) {
        val msg = state.bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        kasaViewModel.clearBanner()
    }

    LaunchedEffect(state.greenToastMessage) {
        if (state.greenToastMessage != null) {
            delay(4200L)
            kasaViewModel.clearGreenToast()
        }
    }

    val isDashboard = currentRoute == KasaRoutes.ROUTE_DASHBOARD

    KasaNovaTheme(darkTheme = state.isDarkTheme) {
        CompositionLocalProvider(
            LocalKasaStrings provides strings,
            LocalLayoutDirection provides layoutDir
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(kasaAmbientBackgroundBrush(state.isDarkTheme))
            ) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = SovereignGold
                            ),
                            navigationIcon = {
                                if (!isDashboard) {
                                    IconButton(
                                        onClick = { navController.popBackStack() },
                                        modifier = Modifier.testTag("top_back_button")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = SovereignGold
                                        )
                                    }
                                }
                            },
                            title = {
                                Text(
                                    text = strings.appTitle,
                                    fontWeight = FontWeight.Bold,
                                    color = SovereignGold
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { kasaViewModel.toggleTheme() },
                                    modifier = Modifier.testTag("toggle_theme_button")
                                ) {
                                    Icon(
                                        imageVector = if (state.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = strings.themeToggleLabel,
                                        tint = SovereignGold
                                    )
                                }
                                TextButton(
                                    onClick = { kasaViewModel.toggleLanguage() },
                                    modifier = Modifier.testTag("toggle_language_button")
                                ) {
                                Icon(
                                    Icons.Filled.Translate,
                                    contentDescription = "Switch Language",
                                    tint = SovereignGold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.language == AppLanguage.FR) "AR" else "FR",
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            val items = listOf(
                                Triple(KasaRoutes.ROUTE_DASHBOARD, strings.navHome, Icons.Filled.Dashboard),
                                Triple(KasaRoutes.ROUTE_NEW_SALE, strings.navSales, Icons.Filled.AddShoppingCart),
                                Triple(KasaRoutes.ROUTE_COLLECTIONS, strings.navCollections, Icons.Filled.Payments),
                                Triple(KasaRoutes.ROUTE_INVENTORY, strings.navStock, Icons.Filled.Inventory2),
                                Triple(KasaRoutes.ROUTE_AI, strings.navAi, Icons.Filled.AutoAwesome)
                            )
                            items.forEach { (route, label, icon) ->
                                NavigationBarItem(
                                    selected = currentRoute == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo(KasaRoutes.ROUTE_DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            val items = listOf(
                                Triple(KasaRoutes.ROUTE_DASHBOARD, strings.navHome, Icons.Filled.Dashboard),
                                Triple(KasaRoutes.ROUTE_NEW_SALE, strings.navSales, Icons.Filled.AddShoppingCart),
                                Triple(KasaRoutes.ROUTE_COLLECTIONS, strings.navCollections, Icons.Filled.Payments),
                                Triple(KasaRoutes.ROUTE_INVENTORY, strings.navStock, Icons.Filled.Inventory2),
                                Triple(KasaRoutes.ROUTE_AI, strings.navAi, Icons.Filled.AutoAwesome)
                            )
                            items.forEach { (route, label, icon) ->
                                NavigationRailItem(
                                    selected = currentRoute == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo(KasaRoutes.ROUTE_DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = KasaRoutes.ROUTE_DASHBOARD
                        ) {
                            composable(KasaRoutes.ROUTE_DASHBOARD) {
                                OwnerDashboardScreen(
                                    state = state,
                                    onSyncNow = kasaViewModel::syncToCloud,
                                    onSeedSampleData = kasaViewModel::seedSampleData,
                                    onAddExpense = kasaViewModel::addExpense,
                                    onConfirmCashDropOtp = kasaViewModel::confirmCashDropOtp,
                                    onNavigateInventory = { navController.navigate(KasaRoutes.ROUTE_INVENTORY) },
                                    onNavigateCrm = { navController.navigate(KasaRoutes.ROUTE_CRM) },
                                    onNavigateNewSale = { navController.navigate(KasaRoutes.ROUTE_NEW_SALE) },
                                    onNavigateCollections = { navController.navigate(KasaRoutes.ROUTE_COLLECTIONS) },
                                    onNavigateCashVault = { navController.navigate(KasaRoutes.ROUTE_CASH_VAULT) },
                                    onNavigateMap = { navController.navigate(KasaRoutes.ROUTE_MAP) },
                                    onNavigateChat = { navController.navigate(KasaRoutes.ROUTE_CHAT) },
                                    onNavigateAi = { navController.navigate(KasaRoutes.ROUTE_AI) },
                                    onNavigateCashFlow = { navController.navigate(KasaRoutes.ROUTE_CASH_FLOW) },
                                    onNavigateLeadFinder = { navController.navigate(KasaRoutes.ROUTE_LEAD_FINDER) }
                                )
                            }
                            composable(KasaRoutes.ROUTE_CASH_FLOW) {
                                DashboardScreen()
                            }
                            composable(KasaRoutes.ROUTE_LEAD_FINDER) {
                                AiLeadFinderScreen(
                                    state = state,
                                    onSearchLeads = kasaViewModel::searchHighIntentLeads,
                                    onGenerateSwahiliPitch = kasaViewModel::generateSwahiliPitchForLead,
                                    onSavePotentialLead = kasaViewModel::savePotentialLead
                                )
                            }
                            composable(KasaRoutes.ROUTE_NEW_SALE) {
                                NewSaleScreen(
                                    state = state,
                                    onCreateSale = kasaViewModel::createSale
                                )
                            }
                            composable(KasaRoutes.ROUTE_INVENTORY) {
                                InventoryScreen(
                                    state = state,
                                    onSaveProduct = kasaViewModel::saveProduct,
                                    onAdjustStock = kasaViewModel::adjustStock
                                )
                            }
                            composable(KasaRoutes.ROUTE_COLLECTIONS) {
                                CollectionsScreen(
                                    state = state,
                                    onReceivePayment = kasaViewModel::receiveInstallmentPayment
                                )
                            }
                            composable(KasaRoutes.ROUTE_CRM) {
                                CrmScreen(
                                    state = state,
                                    onSaveCustomer = kasaViewModel::saveCustomer
                                )
                            }
                            composable(KasaRoutes.ROUTE_CASH_VAULT) {
                                CashVaultScreen(
                                    state = state,
                                    onCreateCashDrop = kasaViewModel::createCashDrop,
                                    onConfirmCashDropOtp = kasaViewModel::confirmCashDropOtp
                                )
                            }
                            composable(KasaRoutes.ROUTE_CHAT) {
                                ChatScreen(
                                    state = state,
                                    onSendMessage = kasaViewModel::sendChatMessage
                                )
                            }
                            composable(KasaRoutes.ROUTE_MAP) {
                                MapScreen(state = state)
                            }
                            composable(KasaRoutes.ROUTE_AI) {
                                AiAssistantScreen(
                                    state = state,
                                    onAnalyzeToday = kasaViewModel::analyzeTodayWithAi,
                                    onRunQuickAction = kasaViewModel::runAiQuickAction,
                                    onAskAi = kasaViewModel::askAi
                                )
                            }
                        }

                        // Green Toast Overlay (e.g. "تم تطبيق خصم السداد المبكر!")
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(visible = state.greenToastMessage != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(UsdEmeraldDark)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("green_toast_banner"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = state.greenToastMessage ?: "",
                                        tint = Color.White
                                    )
                                    Text(
                                        text = state.greenToastMessage ?: "",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
