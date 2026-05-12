package com.example.arogyasahaya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.R
import com.example.arogyasahaya.data.local.entity.AshaEvent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshaConnectScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.allAshaEvents.observeAsState(initial = emptyList())
    val isSyncing by viewModel.isSyncing.observeAsState(initial = false)
    val sdf = SimpleDateFormat("d MMMM yyyy, EEEE", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Groups, 
                            contentDescription = null, 
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.asha_connect), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF1976D2)
                        )
                    } else {
                        IconButton(onClick = { viewModel.syncAshaEvents() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                if (isSyncing) "Syncing live health events..." else stringResource(R.string.asha_desc),
                color = if (isSyncing) Color(0xFF1976D2) else Color.Gray,
                fontSize = 14.sp,
                fontWeight = if (isSyncing) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            DynamicCalendarView(events)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.upcoming_events),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (isSyncing) {
                    Text("UPDATING...", color = Color(0xFF1976D2), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (events.isEmpty() && !isSyncing) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No upcoming events found.", color = Color.Gray)
                        }
                    }
                }
                items(events) { event ->
                    val isToday = Calendar.getInstance().apply { timeInMillis = event.date }.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                    
                    EventCard(
                        title = event.title,
                        date = sdf.format(Date(event.date)),
                        location = event.location,
                        description = event.description,
                        icon = if (event.type == "CAMP") Icons.Default.Business else Icons.Default.Face,
                        iconColor = if (event.type == "CAMP") Color(0xFF1976D2) else Color(0xFFFFA000),
                        isLive = isToday
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicCalendarView(events: List<AshaEvent>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(32.dp)
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            val currentYear = calendar.get(Calendar.YEAR)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${currentMonth.uppercase()} $currentYear", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 18.sp, 
                    color = Color(0xFF1976D2),
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem("Camp", Color(0xFF1976D2))
                    LegendItem("Visit", Color(0xFFFFA000))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Days of Week Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(day, fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Grid
            val days = (1..daysInMonth).toList()
            val rows = days.chunked(7)
            
            rows.forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowDays.forEach { day ->
                        val dayEvents = events.filter { 
                            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                            cal.get(Calendar.DAY_OF_MONTH) == day
                        }
                        
                        val isCamp = dayEvents.any { it.type == "CAMP" }
                        val isVisit = dayEvents.any { it.type == "VISIT" }
                        val isToday = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == day
                        
                        val bgColor = when {
                            isCamp -> Color(0xFF1976D2)
                            isVisit -> Color(0xFFFFA000)
                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }
                        
                        val textColor = when {
                            isCamp || isVisit -> Color.White
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                fontSize = 13.sp,
                                fontWeight = if (dayEvents.isNotEmpty() || isToday) FontWeight.Black else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                    repeat(7 - rowDays.size) {
                        Spacer(modifier = Modifier.size(38.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EventCard(
    title: String,
    date: String,
    location: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    isLive: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (isLive) {
                        Surface(
                            color = Color.Red,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                "LIVE", 
                                color = Color.White, 
                                fontSize = 8.sp, 
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(date, fontSize = 12.sp, color = iconColor)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(location, fontSize = 12.sp, color = Color.Gray)
                }
                Text(description, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

fun size(dp: androidx.compose.ui.unit.Dp): Modifier = Modifier.size(dp)
