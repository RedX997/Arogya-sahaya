package com.example.arogyasahaya.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.arogyasahaya.R
import com.example.arogyasahaya.data.local.entity.Medicine
import com.example.arogyasahaya.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import com.example.arogyasahaya.utils.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.arogyasahaya.ui.components.*

@Composable
fun HomeScreen(
    viewModel: HealthViewModel,
    onAddMedicine: () -> Unit,
    onAddVital: () -> Unit,
    onViewGraph: () -> Unit,
    onViewAppointments: () -> Unit,
    onViewSymptoms: () -> Unit,
    onViewSettings: () -> Unit,
    onViewMedicalRecords: () -> Unit,
    onViewChat: () -> Unit,
    onViewAshaConnect: () -> Unit,
    onViewCaregiver: () -> Unit
) {
    val medicines by viewModel.allMedicines.observeAsState(initial = emptyList())
    val familyMembers by viewModel.allFamilyMembers.observeAsState(initial = emptyList())
    val latestVital by viewModel.latestVital.observeAsState()
    val context = LocalContext.current
    val prefManager = remember { PreferenceManager(context) }
    val profileName = prefManager.getUserName()
    val firstName = profileName.split(" ").firstOrNull() ?: profileName
    
    var showGuestPopup by remember { mutableStateOf(prefManager.isGuest()) }

    if (showGuestPopup) {
        AlertDialog(
            onDismissRequest = { showGuestPopup = false },
            title = { Text("Demo Mode Active") },
            text = { Text("You are currently using the app as a Guest. Your health data is being stored locally for demo purposes only and will NOT be synced to the cloud.\n\nTo save your data permanently and view it on other devices, please Sign Up or Log In.") },
            confirmButton = {
                Button(onClick = { showGuestPopup = false }) {
                    Text("Continue Demo")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showGuestPopup = false
                    viewModel.logout()
                    onViewSettings() // This will navigate to settings/login eventually if we handle logout
                }) {
                    Text("Sign Up Now")
                }
            }
        )
    }

    val takenCount = medicines.count { it.isTaken }
    val totalCount = medicines.size
    
    val lowStockMedicines = medicines.filter { it.remainingTablets < 7 }

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    val coroutineScope = rememberCoroutineScope()
    var showConfetti by remember { mutableStateOf(false) }
    var showAddFamilyDialog by remember { mutableStateOf(false) }

    fun triggerVictory() {
        coroutineScope.launch {
            showConfetti = true
            delay(2000)
            showConfetti = false
        }
    }

    fun processAssistantAction(action: AssistantAction) {
        when (action) {
            is AssistantAction.LogBP -> {
                viewModel.addVital(action.sys, action.dia, 75, null, "Logged via AI")
                ArogyaAssistant.speak("Logged your blood pressure as ${action.sys} over ${action.dia}")
            }
            is AssistantAction.Navigate -> {
                ArogyaAssistant.speak("Opening ${action.destination}")
                when (action.destination) {
                    "medicines" -> onAddMedicine()
                    "vitals" -> onAddVital()
                    "trends" -> onViewGraph()
                    "vault" -> onViewMedicalRecords()
                    "appointments" -> onViewAppointments()
                }
            }
            AssistantAction.Emergency -> {
                val gmmIntentUri = Uri.parse("geo:0,0?q=nearest hospital")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                context.startActivity(mapIntent)
            }
            AssistantAction.CheckHealthScore -> {
                val score = viewModel.healthScore.value ?: 0f
                ArogyaAssistant.speak("Your health score is currently ${score.toInt()} out of 100. ${if(score > 80) "You are doing great!" else "Keep taking your medicines on time to improve it."}")
            }
            AssistantAction.QueryEvents -> {
                val events = viewModel.allAshaEvents.value ?: emptyList()
                if (events.isEmpty()) {
                    ArogyaAssistant.speak("There are no upcoming health events scheduled at the moment.")
                } else {
                    val event = events.first()
                    ArogyaAssistant.speak("The next event is ${event.title} at ${event.location}. Would you like to join?")
                    onViewAshaConnect()
                }
            }
            AssistantAction.AttendPolio -> {
                val events = viewModel.allAshaEvents.value ?: emptyList()
                val polioEvent = events.find { it.title.lowercase().contains("polio") }
                if (polioEvent != null) {
                    viewModel.updateAshaEventStatus(polioEvent, "ATTENDING")
                    ArogyaAssistant.speak("Great! I've marked you as attending the Polio drive.")
                } else {
                    ArogyaAssistant.speak("I couldn't find a Polio drive in the schedule, but I'm opening the events calendar for you.")
                    onViewAshaConnect()
                }
            }
            AssistantAction.Unknown -> {
                ArogyaAssistant.speak("I'm sorry, I didn't understand that. You can say log B P, check my health score, or are there any camps.")
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            val action = ArogyaAssistant.parseCommand(spokenText)
            processAssistantAction(action)
        }
    }

    // Removed manual startVoiceAssistant as it's now in MainActivity FAB
    
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface == BackgroundDark
    
    val backgroundBrush = remember(currentHour, isDark) {
        if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF1A1A1A)))
        } else {
            // Light Theme: Always keep it light/white as per user request
            when {
                currentHour in 5..10 -> Brush.verticalGradient(listOf(Color(0xFFFFFDE7), Color(0xFFFFFFFF))) // Morning
                currentHour in 17..20 -> Brush.verticalGradient(listOf(Color(0xFFF3E5F5), Color(0xFFFFFFFF))) // Evening
                else -> Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFFFFFFF))) // Default light
            }
        }
    }
    
    val greetingColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = greetingColor.copy(alpha = 0.6f)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.greeting, firstName),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = greetingColor,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            "How are you feeling today?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = subtitleColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.clearAllData() }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Reset Data", tint = HealthRed.copy(alpha = 0.5f))
                        }
                        IconButton(onClick = onViewSettings) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(HealthGreen, HealthBlue)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(firstName.take(1), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // Mood Selector
            item {
                Text(
                    "How are you feeling?",
                    color = greetingColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("😊" to "Good", "😐" to "Okay", "😔" to "Low", "🤒" to "Sick", "😴" to "Tired").forEach { (emoji, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { /* Logic to log mood */ }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = subtitleColor)
                        }
                    }
                }
            }

            // Daily Insights
            item {
                Text(
                    "Daily Insights",
                    color = greetingColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                val insights by viewModel.dailyInsights.observeAsState(initial = emptyList())
                val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
                
                LaunchedEffect(insights) {
                    if (insights.isNotEmpty()) {
                        while (true) {
                            delay(3000)
                            val nextIndex = (scrollState.firstVisibleItemIndex + 1) % insights.size
                            scrollState.animateScrollToItem(nextIndex)
                        }
                    }
                }

                androidx.compose.foundation.lazy.LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(insights) { insight ->
                        InsightCard(
                            insight.title,
                            insight.subtitle,
                            getIconForName(insight.icon),
                            Color(insight.color)
                        )
                    }
                }
            }

            item { HealthShieldWidget(streakDays = 7) }

            item {
                val score by viewModel.healthScore.observeAsState(initial = 85f)
                HealthScoreWidget(adherence = score / 100f)
            }


            // Adherence Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(28.dp)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(HealthBlue, HealthBlue.copy(alpha = 0.8f))))
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.today_adherence), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$takenCount / $totalCount", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.medicines_taken), color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 8.dp), fontSize = 14.sp)
                                }
                            }
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { if (totalCount > 0) takenCount.toFloat() / totalCount else 0f },
                                    modifier = Modifier.size(70.dp),
                                    color = Color.White,
                                    strokeWidth = 8.dp,
                                    trackColor = Color.White.copy(alpha = 0.2f),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text(text = if (totalCount > 0) "${((takenCount.toFloat() / totalCount) * 100).toInt()}%" else "0%", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
            
            // Refill Alerts
            item {
                AnimatedVisibility(visible = lowStockMedicines.isNotEmpty(), enter = fadeIn() + slideInVertically()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HealthOrangeLight),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthOrange.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = HealthOrange)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.refill_alert), fontWeight = FontWeight.Bold, color = HealthOrange, fontSize = 16.sp)
                                Text("Some medicines are running low. Tap to refill.", fontSize = 13.sp, color = HealthOrange.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            item { Text(stringResource(R.string.todays_medicines), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

            if (medicines.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_medicines), color = Color.Gray)
                    }
                }
            } else {
                items(medicines, key = { it.id }) { medicine ->
                    MedicineStatusItem(
                        medicine = medicine,
                        onTakenClick = { viewModel.markAsTaken(medicine); triggerVictory() },
                        formatter = timeFormatter
                    )
                }
            }

            item {
                FamilyCircleWidget(
                    members = familyMembers,
                    onAddClick = { showAddFamilyDialog = true },
                    onInviteClick = { com.example.arogyasahaya.utils.ShareHelper.shareAppInvite(context) },
                    onViewChat = { onViewChat() }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Latest Vitals", color = greetingColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("View History", color = HealthBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onAddVital() })
                    }
                    
                    if (latestVital != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VitalSummaryCard("BP", "${latestVital?.systolic}/${latestVital?.diastolic}", "mmHg", HealthRed, Modifier.weight(1f))
                            VitalSummaryCard("Heart", "${latestVital?.heartRate}", "bpm", HealthBlue, Modifier.weight(1f))
                            if (latestVital?.sugar != null) {
                                VitalSummaryCard("Sugar", "${latestVital?.sugar}", "mg/dL", HealthOrange, Modifier.weight(1f))
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onAddVital() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No vitals logged yet. Tap to start.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.quick_actions), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickActionCard(stringResource(R.string.asha_connect), Icons.Default.Groups, Color(0xFF1976D2), Modifier.weight(1f), onViewAshaConnect)
                    QuickActionCard(stringResource(R.string.caregiver_view), Icons.Default.AdminPanelSettings, Color(0xFF455A64), Modifier.weight(1f), onViewCaregiver)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickActionCard(stringResource(R.string.log_vitals), Icons.Default.Favorite, HealthRed, Modifier.weight(1f), onAddVital)
                    QuickActionCard(stringResource(R.string.add_medicine), Icons.Default.AddCircle, HealthBlue, Modifier.weight(1f), onAddMedicine)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickActionCard(stringResource(R.string.medical_vault), Icons.Default.Folder, HealthTeal, Modifier.weight(1f), onViewMedicalRecords)
                    QuickActionCard(stringResource(R.string.symptom_log), Icons.Default.Description, HealthOrange, Modifier.weight(1f), onViewSymptoms)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickActionCard(stringResource(R.string.appointments), Icons.Default.Event, HealthGreen, Modifier.weight(1f), onViewAppointments)
                    val score by viewModel.healthScore.observeAsState(initial = 85f)
                    QuickActionCard(stringResource(R.string.share_summary), Icons.Default.Share, Color(0xFF673AB7), Modifier.weight(1f), { 
                        ShareHelper.shareHealthSummary(context, profileName, score.toInt())
                    })
                }
                // End of actions
            }

            item {
                val emergencyName = prefManager.getEmergencyName()
                val emergencyPhone = prefManager.getEmergencyPhone()
                
                if (emergencyName != "Not Set" && emergencyPhone != "Not Set") {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(90.dp).graphicsLayer { shadowElevation = 4.dp.toPx(); shape = RoundedCornerShape(24.dp) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthOrange.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyPhone"))
                                context.startActivity(intent)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.emergency_call_btn), color = HealthOrange, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Text("${stringResource(R.string.tap_to_call)}: $emergencyName", color = HealthOrange.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(HealthOrange), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(90.dp).graphicsLayer { shadowElevation = 4.dp.toPx(); shape = RoundedCornerShape(24.dp) },
                    colors = CardDefaults.cardColors(containerColor = HealthRedLight),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).clickable {
                            val gmmIntentUri = Uri.parse("geo:0,0?q=nearest hospital")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply { setPackage("com.google.android.apps.maps") }
                            context.startActivity(mapIntent)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.sos_emergency), color = HealthRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text(stringResource(R.string.tap_to_navigate), color = HealthRed.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(HealthRed), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        if (showConfetti) { ConfettiOverlay() }

        if (showAddFamilyDialog) {
            AddFamilyDialog(
                onDismiss = { showAddFamilyDialog = false },
                onSave = { name, phone -> viewModel.addFamilyMember(name, phone); showAddFamilyDialog = false }
            )
        }
    }
}

@Composable
fun ConfettiOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        val colors = listOf(HealthGreen, HealthBlue, HealthTeal, HealthOrange, HealthRed)
        repeat(50) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "confetti")
            val yOffset by infiniteTransition.animateFloat(
                initialValue = -50f, targetValue = 1500f,
                animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1500 + (index * 20), easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "y"
            )
            val xOffset = remember { (0..1000).random().toFloat() }
            val color = remember { colors.random() }
            Box(modifier = Modifier.offset(x = xOffset.dp, y = (yOffset/2).dp).size(8.dp).background(color, CircleShape))
        }
    }
}

fun getIconForName(name: String): ImageVector {
    return when (name) {
        "DirectionsWalk" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "WaterDrop" -> Icons.Default.WaterDrop
        "Bedtime" -> Icons.Default.Bedtime
        "Restaurant" -> Icons.Default.Restaurant
        "SelfImprovement" -> Icons.Default.SelfImprovement
        "Psychology" -> Icons.Default.Psychology
        "RemoveRedEye" -> Icons.Default.RemoveRedEye
        "Person" -> Icons.Default.Person
        "Groups" -> Icons.Default.Groups
        "MenuBook" -> Icons.Default.MenuBook
        "WbSunny" -> Icons.Default.WbSunny
        "PhonelinkOff" -> Icons.Default.PhonelinkOff
        else -> Icons.Default.HealthAndSafety
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFamilyDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                TextButton(
                    onClick = { if (name.isNotEmpty() && phone.isNotEmpty()) { onSave(name, phone); ShareHelper.sendSmsInvite(context, phone) } },
                    enabled = name.isNotEmpty() && phone.isNotEmpty()
                ) { Text("Save & Invite") }
                TextButton(
                    onClick = { if (name.isNotEmpty() && phone.isNotEmpty()) onSave(name, phone) },
                    enabled = name.isNotEmpty() && phone.isNotEmpty()
                ) { Text("Save Only") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add Family Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
