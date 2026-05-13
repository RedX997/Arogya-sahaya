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
import com.example.arogyasahaya.utils.PreferenceManager
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
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("Firebase", "Failed to initialize Firebase: ${e.message}")
        }
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
        val resetRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reset",
            ExistingPeriodicWorkPolicy.KEEP,
            resetRequest
        )
    }

    private fun applyLocale(language: String) {
        val langCode = when {
            language.contains("Hindi") -> "hi"
            language.contains("Marathi") -> "mr"
            language.contains("Kannada") -> "kn"
            else -> "en"
        }
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

@Composable
fun ArogyaSahayaApp() {
    val navController = rememberNavController()
    val viewModel: HealthViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefManager = remember { PreferenceManager(context) }

    val showBottomBar = currentDestination?.route !in listOf("splash", "login")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
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
                            label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentDestination?.route == "home") {
                val speechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == android.app.Activity.RESULT_OK) {
                        val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
                        val action = com.example.arogyasahaya.utils.ArogyaAssistant.parseCommand(spokenText)
                        
                        // Handle action
                        when (action) {
                            is com.example.arogyasahaya.utils.AssistantAction.LogBP -> {
                                viewModel.addVital(action.sys, action.dia, 75, null, "Logged via AI")
                                com.example.arogyasahaya.utils.ArogyaAssistant.speak("Logged your blood pressure as ${action.sys} over ${action.dia}")
                            }
                            is com.example.arogyasahaya.utils.AssistantAction.Navigate -> {
                                com.example.arogyasahaya.utils.ArogyaAssistant.speak("Opening ${action.destination}")
                                when (action.destination) {
                                    "medicines" -> navController.navigate("add_medicine")
                                    "vitals" -> navController.navigate("add_vital")
                                    "trends" -> navController.navigate("graph")
                                    "vault" -> navController.navigate("medical_records")
                                    "appointments" -> navController.navigate("appointments")
                                }
                            }
                            com.example.arogyasahaya.utils.AssistantAction.Emergency -> {
                                val gmmIntentUri = android.net.Uri.parse("geo:0,0?q=nearest hospital")
                                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            }
                            else -> {
                                // Default handling or speak unknown
                                if (action is com.example.arogyasahaya.utils.AssistantAction.Unknown) {
                                    com.example.arogyasahaya.utils.ArogyaAssistant.speak("I'm sorry, I didn't understand that.")
                                }
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "How can I help you today?")
                        }
                        speechLauncher.launch(intent)
                    },
                    containerColor = com.example.arogyasahaya.ui.theme.HealthTeal,
                    contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(64.dp).offset(y = (-10).dp) // Slight offset to avoid hugging the bar too tight
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(32.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(onNext = {
                    val nextRoute = if (prefManager.isLoggedIn()) "home" else "login"
                    navController.navigate(nextRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }
            composable("login") {
                LoginScreen(viewModel = viewModel, onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                    viewModel.syncWithCloud() // Initial sync after login
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
                    onViewChat = { navController.navigate("family_chat") },
                    onViewAshaConnect = { navController.navigate("asha_connect") },
                    onViewCaregiver = { navController.navigate("caregiver") }
                )
            }
            composable("add_medicine") { AddMedicineScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("add_vital") { AddVitalScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("graph") { GraphScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("appointments") { AppointmentScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("symptoms") { SymptomScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }, onNavigateToCaregiver = { navController.navigate("caregiver") }, onNavigateToTargets = { navController.navigate("targets") }) }
            composable("caregiver") { CaregiverDashboard(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("targets") { HealthTargetsScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("medical_records") { MedicalRecordsScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("family_chat") { FamilyChatScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("asha_connect") { AshaConnectScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
            composable("profile") {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = { 
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                )
            }
        }
    }
}
