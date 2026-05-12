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
    onViewChat: () -> Unit
) {
    val medicines by viewModel.allMedicines.observeAsState(initial = emptyList())
    val familyMembers by viewModel.allFamilyMembers.observeAsState(initial = emptyList())
    val latestVital by viewModel.latestVital.observeAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    val profileName = remember { prefs.getString("profile_name", "Ramesh Kumar") ?: "Ramesh Kumar" }
    val firstName = profileName.split(" ").firstOrNull() ?: profileName

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
            AssistantAction.Unknown -> {
                ArogyaAssistant.speak("I'm sorry, I didn't understand that. You can say log B P, or open my medicines.")
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

    fun startVoiceAssistant() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "How can I help you today?")
        }
        speechLauncher.launch(intent)
    }
    
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface == BackgroundDark
    
    val backgroundBrush = remember(currentHour, isDark) {
        if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF1A1A1A)))
        } else {
            when {
                currentHour in 5..10 -> Brush.verticalGradient(listOf(Color(0xFFFFFDE7), Color(0xFFFFFFFF))) // Morning
                currentHour in 17..20 -> Brush.verticalGradient(listOf(Color(0xFFF3E5F5), Color(0xFFFFFFFF))) // Evening
                currentHour > 20 || currentHour < 5 -> Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF1A1A1A))) // Night
                else -> Brush.verticalGradient(listOf(Color(0xFFF0F7FF), Color(0xFFFFFFFF))) // Day
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


        // Daily Insights (The Blog Feature)
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
            
            // Auto-scrolling effect
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

        // Health Shield Widget (Gamification)
        item {
            HealthShieldWidget(streakDays = 7) // Simulated 7-day streak
        }

        // Health Score Widget
        item {
            val score by viewModel.healthScore.observeAsState(initial = 85f)
            HealthScoreWidget(adherence = score / 100f)
        }

        // Family Circle Widget
        item {
            FamilyCircleWidget(
                members = familyMembers,
                onAddClick = { showAddFamilyDialog = true },
                onViewChat = onViewChat
            )
        }

        // Adherence Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(28.dp)
                        clip = true
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(HealthBlue, HealthBlue.copy(alpha = 0.8f))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.today_adherence),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "$takenCount / $totalCount",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.medicines_taken),
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        fontSize = 14.sp
                                    )
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
                                Text(
                                    text = if (totalCount > 0) "${((takenCount.toFloat() / totalCount) * 100).toInt()}%" else "0%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
            }
        }
        
        // Refill Alerts
        item {
            AnimatedVisibility(
                visible = lowStockMedicines.isNotEmpty(),
                enter = fadeIn() + slideInVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HealthOrangeLight),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthOrange.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = HealthOrange)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.refill_alert), fontWeight = FontWeight.Bold, color = HealthOrange, fontSize = 16.sp)
                            Text(
                                "Some medicines are running low. Tap to refill.",
                                fontSize = 13.sp,
                                color = HealthOrange.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Medicine List Header
        item {
            Text(stringResource(R.string.todays_medicines), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

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
                    onTakenClick = { 
                        viewModel.markAsTaken(medicine)
                        triggerVictory()
                    },
                    formatter = timeFormatter
                )
            }
        }

        // Latest Vitals Summary
        item {
            val vitals by viewModel.allVitals.observeAsState(initial = emptyList())
            val latest = vitals.lastOrNull()
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Latest Vitals",
                        color = greetingColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "View History",
                        color = HealthBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onAddVital() }
                    )
                }
                
                if (latest != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VitalSummaryCard(
                            label = "BP",
                            value = "${latest.systolic}/${latest.diastolic}",
                            unit = "mmHg",
                            color = HealthRed,
                            modifier = Modifier.weight(1f)
                        )
                        VitalSummaryCard(
                            label = "Heart",
                            value = "${latest.heartRate}",
                            unit = "bpm",
                            color = HealthBlue,
                            modifier = Modifier.weight(1f)
                        )
                        if (latest.sugar != null) {
                            VitalSummaryCard(
                                label = "Sugar",
                                value = "${latest.sugar}",
                                unit = "mg/dL",
                                color = HealthOrange,
                                modifier = Modifier.weight(1f)
                            )
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

        // Quick Actions
        item {
            Text(
                stringResource(R.string.quick_actions),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(stringResource(R.string.add_medicine), Icons.Default.Add, HealthBlue, Modifier.weight(1f), onAddMedicine)
                QuickActionCard(stringResource(R.string.log_vitals), Icons.Default.Favorite, HealthRed, Modifier.weight(1f), onAddVital)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(stringResource(R.string.medical_vault), Icons.Default.Folder, HealthTeal, Modifier.weight(1f), onViewMedicalRecords)
                QuickActionCard(stringResource(R.string.symptom_log), Icons.Default.Description, HealthOrange, Modifier.weight(1f), onViewSymptoms)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(stringResource(R.string.appointments), Icons.Default.Event, HealthGreen, Modifier.weight(1f), onViewAppointments)
                QuickActionCard(stringResource(R.string.share_summary), Icons.Default.Share, Color(0xFF673AB7), Modifier.weight(1f), { /* Future implementation */ })
            }
        }

        // SOS Button
        item {
            Card(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:112") }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .graphicsLayer {
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(24.dp)
                    },
                colors = CardDefaults.cardColors(containerColor = HealthRedLight),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).clickable {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=nearest hospital")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("LIFE-LINE SOS", color = HealthRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("TAP TO NAVIGATE TO NEAREST HOSPITAL", color = HealthRed.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(HealthRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        // Emergency Contact Button
        item {
            val emergencyPhone = remember { prefs.getString("emergency_phone", "") ?: "" }
            val emergencyName = remember { prefs.getString("emergency_name", "") ?: "" }
            
            Card(
                onClick = {
                    if (emergencyPhone.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$emergencyPhone") }
                        context.startActivity(intent)
                    } else {
                        onViewSettings()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .graphicsLayer {
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(24.dp)
                    },
                colors = CardDefaults.cardColors(containerColor = if (emergencyPhone.isNotEmpty()) HealthBlue.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (emergencyPhone.isNotEmpty()) HealthBlue.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (emergencyPhone.isNotEmpty()) "CALL $emergencyName" else "SET EMERGENCY CONTACT", 
                            color = if (emergencyPhone.isNotEmpty()) HealthBlue else Color.Gray, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            if (emergencyPhone.isNotEmpty()) "INSTANT CONTACT FOR ASSISTANCE" else "TAP TO CONFIGURE YOUR LIFELINE", 
                            color = (if (emergencyPhone.isNotEmpty()) HealthBlue else Color.Gray).copy(alpha = 0.6f), 
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(if (emergencyPhone.isNotEmpty()) HealthBlue else Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (emergencyPhone.isNotEmpty()) Icons.Default.Call else Icons.Default.Settings, 
                            contentDescription = null, 
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Trends Link
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.view_trends),
                    color = Color(0xFF2196F3),
                    modifier = Modifier.clickable { onViewGraph() }.padding(vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    if (showConfetti) {
        ConfettiOverlay()
    }

    if (showAddFamilyDialog) {
        AddFamilyDialog(
            onDismiss = { showAddFamilyDialog = false },
            onSave = { name, phone ->
                viewModel.addFamilyMember(name, phone)
                showAddFamilyDialog = false
                // Auto-Invite SMS
                try {
                    val inviteMsg = "Hi $name, I've added you to my Arogya Sahaya care circle. Download the app here to track my health and chat with me: https://arogyasahaya.com/download"
                    val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone")).apply {
                        putExtra("sms_body", inviteMsg)
                    }
                    context.startActivity(smsIntent)
                } catch (e: Exception) {
                    // Fallback if SMS intent fails
                }
            }
        )
    }

    // AI Assistant FAB
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { startVoiceAssistant() },
            containerColor = HealthTeal,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Assistant",
                modifier = Modifier.size(32.dp)
            )
        }
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
                initialValue = -50f,
                targetValue = 1500f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500 + (index * 20), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "y"
            )
            val xOffset = remember { (0..1000).random().toFloat() }
            val color = remember { colors.random() }
            
            Box(
                modifier = Modifier
                    .offset(x = xOffset.dp, y = (yOffset/2).dp)
                    .size(8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun MedicineStatusItem(medicine: Medicine, onTakenClick: () -> Unit, formatter: SimpleDateFormat) {
    val timeStr = remember(medicine.time) { formatter.format(Date(medicine.time)) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            shadowElevation = 4.dp.toPx()
            shape = RoundedCornerShape(20.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, onSurfaceColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (medicine.isTaken) HealthGreen.copy(alpha = 0.1f) else HealthBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (medicine.pillImageUri != null) {
                    AsyncImage(
                        model = medicine.pillImageUri,
                        contentDescription = "Pill Photo",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (medicine.isTaken) Icons.Default.CheckCircle else Icons.Default.Medication,
                        contentDescription = null,
                        tint = if (medicine.isTaken) HealthGreen else HealthBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    color = onSurfaceColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "$timeStr · ${medicine.dosage}",
                    color = onSurfaceColor.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
            
            if (medicine.isTaken) {
                Icon(Icons.Default.DoneAll, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(24.dp))
            } else {
                IconButton(
                    onClick = onTakenClick,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(HealthBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Mark Taken", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun HealthScoreWidget(adherence: Float) {
    val score = (adherence * 100).toInt()
    val scoreColor = when {
        score >= 80 -> HealthGreen
        score >= 50 -> HealthOrange
        else -> HealthRed
    }

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            shadowElevation = 8.dp.toPx()
            shape = RoundedCornerShape(28.dp)
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(80.dp),
                    color = scoreColor,
                    strokeWidth = 10.dp,
                    trackColor = scoreColor.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    text = "$score",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(HealthGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE HEALTH SCORE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        score >= 80 -> "Excellent Health State"
                        score >= 50 -> "Keep Following Routine"
                        else -> "Needs Attention"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = scoreColor
                )
                Text(
                    "Reflects medication and recent vitals",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun InsightCard(title: String, subtitle: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.width(220.dp).height(130.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 15.sp, 
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                subtitle, 
                fontSize = 12.sp, 
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun getIconForName(name: String): ImageVector {
    return when (name) {
        "WaterDrop" -> Icons.Default.WaterDrop
        "DirectionsWalk" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "Favorite" -> Icons.Default.Favorite
        "Bedtime" -> Icons.Default.Bedtime
        "Psychology" -> Icons.Default.Psychology
        "SelfImprovement" -> Icons.Default.SelfImprovement
        "Restaurant" -> Icons.Default.Restaurant
        else -> Icons.Default.Lightbulb
    }
}

@Composable
fun FamilyCircleWidget(
    members: List<com.example.arogyasahaya.data.local.entity.FamilyMember>, 
    onAddClick: () -> Unit,
    onViewChat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onViewChat() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Family Circle", fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (members.isEmpty()) "No one is watching over you yet" else "${members.size} people are watching over you", 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                members.take(3).forEach { member ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HealthBlue.copy(alpha = 0.2f))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            member.name.take(1).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HealthBlue
                        )
                    }
                    Spacer(modifier = Modifier.width((-8).dp))
                }
                
                IconButton(onClick = onViewChat) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = HealthGreen, modifier = Modifier.size(26.dp))
                }
                
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = HealthBlue, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun VitalSummaryCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(unit, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFamilyDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotEmpty() && phone.isNotEmpty()) onSave(name, phone) },
                enabled = name.isNotEmpty() && phone.isNotEmpty()
            ) { Text("Add Member") }
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

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(115.dp).graphicsLayer {
            shadowElevation = 6.dp.toPx()
            shape = RoundedCornerShape(24.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                letterSpacing = (-0.2).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun HealthShieldWidget(streakDays: Int) {
    val shieldColor = when {
        streakDays >= 14 -> Color(0xFFFFD700) // Gold
        streakDays >= 7 -> Color(0xFFC0C0C0) // Silver
        else -> Color(0xFFCD7F32) // Bronze
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "shield_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, shieldColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(shieldColor.copy(alpha = glowAlpha), CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = shieldColor,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    "$streakDays Day Health Streak!",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Your Health Shield is getting stronger.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
