package com.ipdial

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import com.ipdial.data.model.CallDirection
import com.ipdial.data.model.RegStatus
import com.ipdial.data.model.CallState
import com.ipdial.ui.SipViewModel
import com.ipdial.ui.screens.*
import com.ipdial.ui.theme.IPDialTheme

class MainActivity : ComponentActivity() {

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handle results if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
        com.ipdial.service.SipService.start(this)

        setContent {
            IPDialTheme {
                IPDialApp()
            }
        }
    }

    private fun requestRequiredPermissions() {
        val required = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionsLauncher.launch(missing.toTypedArray())
    }
}

sealed class NavDest(val route: String, val label: String, val icon: ImageVector) {
    object Home    : NavDest("home",    "Home",     Icons.Default.Home)
    object Keypad  : NavDest("keypad",  "Keypad",   Icons.Default.Dialpad)
    object Contacts: NavDest("contacts","Contacts", Icons.Default.Contacts)
    object Settings: NavDest("settings","Settings", Icons.Default.Settings)
    object Accounts: NavDest("accounts","Accounts", Icons.Default.AccountBalance)
    object About   : NavDest("about",   "About",    Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPDialApp() {
    val vm: SipViewModel = viewModel()
    val callSession by vm.callSession.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val bottomTabs = listOf(NavDest.Home, NavDest.Keypad, NavDest.Contacts)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Full-screen call overlay
    val activeCallSession = callSession

    // Wrapper to put drawer on the right
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                // Inside the drawer, we want content to be Ltr again
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.width(300.dp),
                        drawerShape = androidx.compose.ui.graphics.RectangleShape
                    ) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Menu",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Divider()
                        NavigationDrawerItem(
                            label = { Text("Accounts") },
                            selected = currentRoute == NavDest.Accounts.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(NavDest.Accounts.route)
                            },
                            icon = { Icon(NavDest.Accounts.icon, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Contacts") },
                            selected = currentRoute == NavDest.Contacts.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(NavDest.Contacts.route)
                            },
                            icon = { Icon(NavDest.Contacts.icon, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Recordings") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Mic, null) },
                            badge = { Text("Coming soon", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = currentRoute == NavDest.Settings.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(NavDest.Settings.route)
                            },
                            icon = { Icon(NavDest.Settings.icon, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("About") },
                            selected = currentRoute == NavDest.About.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(NavDest.About.route)
                            },
                            icon = { Icon(NavDest.About.icon, null) }
                        )
                    }
                }
            }
        ) {
            // Main content back to Ltr
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    bottomBar = {
                        // Hide bottom bar during incoming/active call
                        if (activeCallSession == null) {
                            NavigationBar(tonalElevation = 3.dp) {
                                bottomTabs.forEach { dest ->
                                    NavigationBarItem(
                                        selected = currentRoute == dest.route,
                                        onClick = {
                                            navController.navigate(dest.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(dest.icon, dest.label) },
                                        label = { Text(dest.label) },
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    // Call overlay takes full screen
                    if (activeCallSession != null) {
                        when {
                            activeCallSession.direction == CallDirection.INCOMING &&
                                    activeCallSession.state == CallState.INCOMING -> {
                                IncomingCallScreen(vm = vm, session = activeCallSession)
                            }
                            else -> {
                                CallScreen(vm = vm, session = activeCallSession)
                            }
                        }
                    } else {
                        // Header logic moved to screens or passed down
                        // Since user wants the hamburger on right and dot on left in home, 
                        // we can either add a TopAppBar here or let HomeScreen handle it.
                        // User mentioned "in home ... move settings ... to hamburger", 
                        // so probably a TopAppBar in IPDialApp or specific to HomeScreen.
                        
                        NavHost(
                            navController = navController,
                            startDestination = NavDest.Home.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable(NavDest.Home.route) { 
                                HomeScreen(
                                    vm = vm, 
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                ) 
                            }
                            composable(NavDest.Keypad.route) { 
                                DialpadScreen(
                                    vm = vm, 
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                ) 
                            }
                            composable(NavDest.Contacts.route) { 
                                ContactsScreen(
                                    vm = vm, 
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                ) 
                            }
                            composable(NavDest.Settings.route) { 
                                SettingsScreen(
                                    vm = vm, 
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                ) 
                            }
                            composable(NavDest.Accounts.route) { 
                                AccountsScreen(
                                    vm = vm, 
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                ) 
                            }
                            composable(NavDest.About.route) { AboutScreen() }
                        }
                    }
                }
            }
        }
    }
}
