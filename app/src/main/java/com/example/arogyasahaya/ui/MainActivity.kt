package com.example.arogyasahaya.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.arogyasahaya.ui.theme.ArogyaSahayaTheme
import com.example.arogyasahaya.utils.DailyResetWorker
import com.example.arogyasahaya.utils.NotificationHelper
import java.util.concurrent.TimeUnit
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.example.arogyasahaya.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val language = prefs.getString("app_language", "English") ?: "English"
        applyLocale(language)
        
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        scheduleDailyReset()
        com.example.arogyasahaya.utils.ArogyaAssistant.init(this)
        
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
            var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    if (key == "dark_mode") {
                        darkMode = p.getBoolean("dark_mode", false)
                    } else if (key == "app_language") {
                        recreate()
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            ArogyaSahayaTheme(darkTheme = darkMode) {
                val permissions = mutableListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                        add(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(permissions.toTypedArray())
                }

                ArogyaSahayaApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.arogyasahaya.utils.ArogyaAssistant.shutdown()
    }

    private fun scheduleDailyReset() {
        val resetRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reset",
            ExistingPeriodicWorkPolicy.KEEP,
            resetRequest
        )
    }

    private fun applyLocale(language: String) {
        val langCode = when (language) {
            "Hindi" -> "hi"
            "Marathi" -> "mr"
            "Kannada" -> "kn"
            else -> "en"
        }
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Also update context for Compose
        createConfigurationContext(config)
    }
}

@Composable
fun ArogyaSahayaApp() {
    val navController = rememberNavController()
    val viewModel: HealthViewModel = viewModel()
    val medicines by viewModel.allMedicines.observeAsState(initial = emptyList())
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Auto-population disabled for real data experience
    /*
    LaunchedEffect(medicines) {
        if (medicines.isEmpty()) {
            viewModel.populateMockData()
        }
    }
    */

    LaunchedEffect(Unit) {
        viewModel.syncInsights()
    }

    val showBottomBar = currentDestination?.route != "splash"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                val items = listOf(
                    Triple(stringResource(R.string.home), "home", Icons.Default.Dashboard),
                    Triple(stringResource(R.string.add_medicine), "add_medicine", Icons.Default.AddCircle), 
                    Triple(stringResource(R.string.vitals), "add_vital", Icons.Default.Adjust),
                    Triple(stringResource(R.string.trends), "graph", Icons.AutoMirrored.Filled.TrendingUp),
                    Triple(stringResource(R.string.profile), "profile", Icons.Default.Person)
                )
                
                items.forEach { (label, route, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))
        ) {
            composable("splash") {
                SplashScreen(onNext = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onAddMedicine = { navController.navigate("add_medicine") },
                    onAddVital = { navController.navigate("add_vital") },
                    onViewGraph = { navController.navigate("graph") },
                    onViewAppointments = { navController.navigate("appointments") },
                    onViewSymptoms = { navController.navigate("symptoms") },
                    onViewSettings = { navController.navigate("settings") },
                    onViewMedicalRecords = { navController.navigate("medical_records") },
                    onViewChat = { navController.navigate("family_chat") }
                )
            }
            composable("medicines") {
                MedicinesScreen(
                    viewModel = viewModel,
                    onAddMedicine = { navController.navigate("add_medicine") }
                )
            }
            composable("add_medicine") {
                AddMedicineScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add_vital") {
                AddVitalScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("graph") {
                GraphScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("appointments") {
                AppointmentScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("symptoms") {
                SymptomScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToCaregiver = { navController.navigate("caregiver") },
                    onNavigateToTargets = { navController.navigate("targets") }
                )
            }
            composable("caregiver") {
                CaregiverDashboard(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("targets") {
                HealthTargetsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("medical_records") {
                MedicalRecordsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("family_chat") {
                FamilyChatScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("asha_connect") {
                AshaConnectScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = { 
                        navController.navigate("splash") {
                            popUpTo(0)
                        }
                    }
                )
            }
        }
    }
}
