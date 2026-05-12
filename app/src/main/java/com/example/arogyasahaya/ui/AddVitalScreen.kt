package com.example.arogyasahaya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.arogyasahaya.data.local.entity.Vital
import com.example.arogyasahaya.ui.theme.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVitalScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val vitals by viewModel.allVitals.observeAsState(initial = emptyList())
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vital Log", fontWeight = FontWeight.Bold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Today's Readings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Today's Readings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Blood Pressure (mmHg)", fontSize = 14.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = systolic,
                            onValueChange = { systolic = it },
                            placeholder = { Text("Systolic (e.g. 120)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = diastolic,
                            onValueChange = { diastolic = it },
                            placeholder = { Text("Diastolic (e.g. 80)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Heart Rate (bpm)", fontSize = 14.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = heartRate,
                        onValueChange = { heartRate = it },
                        placeholder = { Text("e.g. 72") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Blood Glucose (mg/dL)", fontSize = 14.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = sugar,
                        onValueChange = { sugar = it },
                        placeholder = { Text("e.g. 110") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (systolic.isNotEmpty() && diastolic.isNotEmpty()) {
                                viewModel.addVital(
                                    systolic = systolic.toIntOrNull() ?: 120,
                                    diastolic = diastolic.toIntOrNull() ?: 80,
                                    heartRate = heartRate.toIntOrNull() ?: 72,
                                    sugar = sugar.toIntOrNull(),
                                    notes = notes
                                )
                                systolic = ""; diastolic = ""; heartRate = ""; sugar = ""; notes = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAVE TODAY'S VITALS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7-Day Trend Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("7-Day Trend", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (vitals.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No data for trends yet.", color = Color.Gray)
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                LineChart(ctx).apply {
                                    description.isEnabled = false
                                    setTouchEnabled(true)
                                    isDragEnabled = true
                                    setScaleEnabled(true)
                                    setPinchZoom(true)
                                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                                    xAxis.setDrawGridLines(false)
                                    axisRight.isEnabled = false
                                    legend.isEnabled = true
                                    legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                                    legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                                    legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                                    legend.setDrawInside(false)
                                }
                            },
                            update = { chart ->
                                val last7Vitals = vitals.takeLast(7)
                                
                                val sysEntries = last7Vitals.mapIndexed { index, vital -> Entry(index.toFloat(), vital.systolic.toFloat()) }
                                val hrEntries = last7Vitals.mapIndexed { index, vital -> Entry(index.toFloat(), vital.heartRate.toFloat()) }
                                val sugarEntries = last7Vitals.mapNotNullIndexed { index, vital -> 
                                    vital.sugar?.let { Entry(index.toFloat(), it.toFloat()) } 
                                }

                                val sysDataSet = LineDataSet(sysEntries, "BP Systolic").apply {
                                    color = Color.Red.toArgb()
                                    setCircleColor(Color.Red.toArgb())
                                    lineWidth = 2f
                                    circleRadius = 4f
                                    setDrawValues(false)
                                }

                                val hrDataSet = LineDataSet(hrEntries, "Heart Rate").apply {
                                    color = Color.Blue.toArgb()
                                    setCircleColor(Color.Blue.toArgb())
                                    lineWidth = 2f
                                    circleRadius = 4f
                                    setDrawValues(false)
                                }

                                val sugarDataSet = LineDataSet(sugarEntries, "Blood Glucose").apply {
                                    color = Color(0xFF2E7D32).toArgb()
                                    setCircleColor(Color(0xFF2E7D32).toArgb())
                                    lineWidth = 2f
                                    circleRadius = 4f
                                    setDrawValues(false)
                                }

                                chart.data = LineData(sysDataSet, hrDataSet, sugarDataSet)
                                chart.invalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
