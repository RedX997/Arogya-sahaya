package com.example.arogyasahaya.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.data.local.entity.Vital
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTargetsScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var sysTarget by remember { mutableStateOf(prefs.getInt("target_sys", 130).toString()) }
    var diaTarget by remember { mutableStateOf(prefs.getInt("target_dia", 85).toString()) }

    val vitals by viewModel.allVitals.observeAsState(initial = emptyList())

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("Health targets", color = Color.White) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("YOUR BP TARGET (set by doctor)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Systolic target", color = Color.Gray)
                            Text(sysTarget, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diastolic target", color = Color.Gray)
                            Text(diaTarget, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("HOW READINGS ARE CLASSIFIED", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                ClassificationRow("Normal", "Below $sysTarget/$diaTarget", Color(0xFF2E7D32))
                ClassificationRow("Borderline", "$sysTarget-140 / $diaTarget-90", Color(0xFFEF6C00))
                ClassificationRow("High", "Above 140/90", Color(0xFFB71C1C))
            }

            item {
                Text("RECENT READINGS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            items(vitals.take(5)) { vital ->
                RecentReadingItem(vital, sysTarget.toIntOrNull() ?: 130, diaTarget.toIntOrNull() ?: 85)
            }
        }
    }
}

@Composable
fun ClassificationRow(label: String, range: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Text(range, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun RecentReadingItem(vital: Vital, targetSys: Int, targetDia: Int) {
    val sdf = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault())
    val (status, color) = when {
        vital.systolic < targetSys && vital.diastolic < targetDia -> "Normal" to Color(0xFF2E7D32)
        vital.systolic > 140 || vital.diastolic > 90 -> "High" to Color(0xFFB71C1C)
        else -> "Borderline" to Color(0xFFEF6C00)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).background(color))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${vital.systolic} / ${vital.diastolic}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(sdf.format(Date(vital.date)) + " · HR ${vital.heartRate}", color = Color.Gray, fontSize = 12.sp)
            }
            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                Text(status, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
