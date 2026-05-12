package com.example.arogyasahaya.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.arogyasahaya.R
import com.example.arogyasahaya.data.local.entity.Vital
import com.example.arogyasahaya.utils.*
import com.example.arogyasahaya.ui.theme.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val vitals by viewModel.allVitals.observeAsState(initial = emptyList())
    val medicines by viewModel.allMedicines.observeAsState(initial = emptyList())
    val symptoms by viewModel.allSymptoms.observeAsState(initial = emptyList())
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    val firstName = (prefs.getString("profile_name", "Ramesh Kumar") ?: "Ramesh Kumar").split(" ").first()

    val avgSys = if (vitals.isNotEmpty()) vitals.map { it.systolic }.average().toInt() else 0
    val avgDia = if (vitals.isNotEmpty()) vitals.map { it.diastolic }.average().toInt() else 0
    val avgHr = if (vitals.isNotEmpty()) vitals.map { it.heartRate }.average().toInt() else 0
    val avgSugar = if (vitals.isNotEmpty()) vitals.filter { it.sugar != null }.map { it.sugar!! }.average().toInt() else 0
    
    val bpTrend = remember(vitals) {
        if (vitals.size >= 3) {
            val last3 = vitals.takeLast(3)
            val diff = last3.last().systolic - last3.first().systolic
            when {
                diff > 10 -> "RISING"
                diff < -10 -> "IMPROVING"
                else -> "STABLE"
            }
        } else "NOT_ENOUGH_DATA"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.health_trends_title), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.avg_bp), "$avgSys/$avgDia", HealthRed, HealthRedLight, Modifier.weight(1f))
                StatCard(stringResource(R.string.avg_hr), "$avgHr bpm", HealthBlue, HealthBlueLight, Modifier.weight(1f))
                StatCard(stringResource(R.string.avg_sugar), if (avgSugar > 0) "$avgSugar" else "--", HealthOrange, HealthOrangeLight, Modifier.weight(1f))
            }

            // Chart
            Card(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVITY TRENDS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                            }
                        },
                        update = { chart ->
                            if (vitals.isEmpty()) return@AndroidView
                            
                            val sysEntries = vitals.mapIndexed { index, vital -> Entry(index.toFloat(), vital.systolic.toFloat()) }
                            val heartRateEntries = vitals.mapIndexed { index, vital -> Entry(index.toFloat(), vital.heartRate.toFloat()) }
                            val sugarEntries = vitals.mapNotNullIndexed { index, vital -> vital.sugar?.let { Entry(index.toFloat(), it.toFloat()) } }

                            val sysDataSet = LineDataSet(sysEntries, context.getString(R.string.bp_systolic)).apply {
                                color = HealthRed.toArgb()
                                setCircleColor(HealthRed.toArgb())
                                lineWidth = 3f
                                circleRadius = 4f
                                setDrawCircleHole(false)
                                setDrawValues(false)
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawFilled(true)
                                fillAlpha = 20
                                fillColor = HealthRed.toArgb()
                            }

                            val hrDataSet = LineDataSet(heartRateEntries, context.getString(R.string.heart_rate)).apply {
                                color = HealthGreen.toArgb()
                                setCircleColor(HealthGreen.toArgb())
                                lineWidth = 3f
                                circleRadius = 4f
                                setDrawCircleHole(false)
                                setDrawValues(false)
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawFilled(true)
                                fillAlpha = 20
                                fillColor = HealthGreen.toArgb()
                            }

                            val sugarDataSet = LineDataSet(sugarEntries, context.getString(R.string.sugar_label)).apply {
                                color = HealthOrange.toArgb()
                                setCircleColor(HealthOrange.toArgb())
                                lineWidth = 3f
                                circleRadius = 4f
                                setDrawCircleHole(false)
                                setDrawValues(false)
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawFilled(true)
                                fillAlpha = 20
                                fillColor = HealthOrange.toArgb()
                            }

                            chart.data = LineData(sysDataSet, hrDataSet, sugarDataSet)
                            chart.invalidate()
                        },
                        modifier = Modifier.padding(16.dp).fillMaxSize()
                    )
                }
            }

            // Predictive Health Card
            if (bpTrend != "NOT_ENOUGH_DATA") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when(bpTrend) {
                            "RISING" -> HealthRedLight
                            "IMPROVING" -> HealthGreen.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        when(bpTrend) {
                            "RISING" -> HealthRed.copy(alpha = 0.2f)
                            "IMPROVING" -> HealthGreen.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                    )
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(
                                when(bpTrend) {
                                    "RISING" -> HealthRed
                                    "IMPROVING" -> HealthGreen
                                    else -> Color.Gray
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (bpTrend == "RISING") Icons.Default.TrendingUp else if (bpTrend == "IMPROVING") Icons.Default.TrendingDown else Icons.Default.HorizontalRule,
                                contentDescription = null, 
                                tint = Color.White, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when(bpTrend) {
                                    "RISING" -> "PREDICTIVE ALERT: BP RISING"
                                    "IMPROVING" -> "HEALTH STATUS: IMPROVING"
                                    else -> "HEALTH STATUS: STABLE"
                                },
                                fontWeight = FontWeight.Black, 
                                color = when(bpTrend) {
                                    "RISING" -> HealthRed
                                    "IMPROVING" -> HealthGreen
                                    else -> Color.Gray
                                }, 
                                fontSize = 14.sp
                            )
                            Text(
                                text = when(bpTrend) {
                                    "RISING" -> "Your blood pressure has trended upwards recently. We recommend checking again later and consulting your doctor if it remains high."
                                    "IMPROVING" -> "Your readings are trending downwards towards a healthier range. Keep it up!"
                                    else -> "Your health readings are steady. Consistency is key!"
                                },
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Health Calendar
            Text("HEALTH CALENDAR", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val calendar = Calendar.getInstance()
                    val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentMonth.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = HealthBlue)
                        Row {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(HealthGreen))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Healthy", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(HealthRed))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alert", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Calendar Grid (7 columns)
                    Column {
                        val days = (1..daysInMonth).toList()
                        val rows = days.chunked(7)
                        
                        rows.forEach { rowDays ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowDays.forEach { day ->
                                    val dayVitals = vitals.filter { 
                                        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                                        cal.get(Calendar.DAY_OF_MONTH) == day
                                    }
                                    
                                    val isLogged = dayVitals.isNotEmpty()
                                    val hasAlert = dayVitals.any { it.systolic > 140 || it.diastolic > 90 || (it.sugar != null && it.sugar!! > 180) }
                                    val hasMedium = dayVitals.any { it.systolic in 130..140 || it.diastolic in 85..90 || (it.sugar != null && it.sugar!! in 140..180) }
                                    
                                    val dayColor = when {
                                        hasAlert -> HealthRed
                                        hasMedium -> HealthOrange
                                        isLogged -> HealthGreen
                                        else -> Color.Transparent
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(dayColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isLogged) FontWeight.Black else FontWeight.Normal,
                                            color = if (isLogged) dayColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                                // Fill empty spaces for incomplete rows
                                repeat(7 - rowDays.size) {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Insights
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HealthOrangeLight.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthOrange.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(HealthOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.insight), fontWeight = FontWeight.Black, color = HealthOrange, fontSize = 16.sp)
                        Text(
                            stringResource(R.string.bp_normal_insight),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Sharing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        PdfReportGenerator.generateReport(
                            context = context,
                            patientName = firstName,
                            medicines = medicines,
                            vitals = vitals,
                            symptoms = symptoms
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.export_pdf), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        val reportFor = context.getString(R.string.health_report_for, firstName)
                        val shareText = "$reportFor\n${context.getString(R.string.avg_bp)}: ${avgSys}/${avgDia}\n${context.getString(R.string.avg_hr)}: $avgHr bpm\n${context.getString(R.string.avg_sugar)}: $avgSugar\nGenerated by Arogya Sahaya"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_report)))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.share_whatsapp), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                val icon = when(label) {
                    stringResource(R.string.avg_bp) -> Icons.Default.Favorite
                    stringResource(R.string.avg_hr) -> Icons.Default.Timeline
                    else -> Icons.Default.Bloodtype
                }
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
            }
        }
    }
}

fun <T> List<T>.mapNotNullIndexed(transform: (index: Int, T) -> Entry?): List<Entry> {
    val destination = mutableListOf<Entry>()
    forEachIndexed { index, element ->
        transform(index, element)?.let { destination.add(it) }
    }
    return destination
}
