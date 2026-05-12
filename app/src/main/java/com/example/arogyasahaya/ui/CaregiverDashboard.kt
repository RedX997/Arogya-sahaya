package com.example.arogyasahaya.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.data.local.entity.Medicine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverDashboard(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val medicines by viewModel.allMedicines.observeAsState(initial = emptyList())
    val vitals by viewModel.allVitals.observeAsState(initial = emptyList())
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    val profileName = remember { prefs.getString("profile_name", "Ramesh Kumar") ?: "Ramesh Kumar" }

    val takenCount = medicines.count { it.isTaken }
    val totalCount = medicines.size
    val adherencePercent = if (totalCount > 0) (takenCount * 100) / totalCount else 0

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("Caregiver View", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("Monitoring: $profileName", color = Color.Gray, fontSize = 14.sp)
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$adherencePercent%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                            Text("Today's adherence", color = Color(0xFF00796B), fontSize = 14.sp)
                        }
                        vitals.firstOrNull()?.let { last ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Last check-in", color = Color(0xFF00796B), fontSize = 12.sp)
                                Text("BP: ${last.bpString}", fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                Text("HR: ${last.heartRate} bpm", fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                            }
                        }
                    }
                }
            }

            item { Text("TODAY'S MEDICINES", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

            items(medicines) { med ->
                CaregiverMedicineItem(med)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val missed = medicines.filter { !it.isTaken && it.time < System.currentTimeMillis() }
                    if (missed.isNotEmpty()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFB71C1C))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Alert: Missed dose", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                                Text("${missed.first().name} was not taken today", color = Color(0xFFB71C1C), fontSize = 14.sp)
                            }
                        }
                    } else {
                        Text("No missed doses today", modifier = Modifier.padding(16.dp), color = Color(0xFFE65100))
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val msg = "Update on $profileName's Health:\nAdherence: $adherencePercent%\nLast BP: ${vitals.firstOrNull()?.bpString ?: "N/A"}\nGenerated by Arogya Sahaya"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Report"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("WhatsApp report")
                    }
                    OutlinedButton(
                        onClick = { /* Export PDF */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Export PDF", color = Color.Gray)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun CaregiverMedicineItem(medicine: Medicine) {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (medicine.isTaken) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (medicine.isTaken) Color(0xFF2E7D32) else Color.Gray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(medicine.name, color = Color.White, fontWeight = FontWeight.Bold)
                val status = if (medicine.isTaken) "Taken at ${sdf.format(Date(medicine.lastTakenDate))}" else "Tonight ${sdf.format(Date(medicine.time))}"
                Text(status, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
