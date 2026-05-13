package com.example.arogyasahaya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.graphicsLayer
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
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var monthOffset by remember { mutableStateOf(0) }
    var maxRange by remember { mutableStateOf(50.0) }
    var selectedEventForDetails by remember { mutableStateOf<AshaEvent?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var userLocation by remember { mutableStateOf(Pair(28.6139, 77.2090)) } // Default to Delhi
    
    // Request location once
    LaunchedEffect(Unit) {
        // In a real app, we'd use fusedLocationProviderClient
        // For now, we'll keep the simulated default but make it mutable for future real GPS integration
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, date, loc, desc, type, time, organizer, lat, lon ->
                viewModel.addAshaEvent(title, date, loc, desc, type, time, organizer, lat, lon)
                showAddDialog = false
            },
            currentLat = userLocation.first,
            currentLon = userLocation.second
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Groups, 
                            contentDescription = null, 
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.asha_connect), fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
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
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        if (showBottomSheet && selectedEventForDetails != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                EventDetailsContent(
                    event = selectedEventForDetails!!,
                    onSetReminder = { 
                        viewModel.toggleAshaEventReminder(selectedEventForDetails!!)
                        showBottomSheet = false
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Upcoming in Next 10 Days
            Text(
                "Upcoming in Next 10 Days",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val tenDaysLater = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 10) }.timeInMillis
            val upcomingEvents = events.filter { it.date in System.currentTimeMillis()..tenDaysLater }
            
            if (upcomingEvents.isEmpty()) {
                Text("No camps scheduled soon.", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp))
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(upcomingEvents) { event ->
                        val daysLeft = ((event.date - System.currentTimeMillis()) / 86400000).toInt()
                        UpcomingEventCard(event, daysLeft)
                    }
                }
            }

            // Range Filter
            Text("Distance Range", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(5.0, 10.0, 50.0).forEach { range ->
                    FilterChip(
                        selected = maxRange == range,
                        onClick = { maxRange = range },
                        label = { Text("${range.toInt()}km", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            val filteredEvents = remember(events, selectedDate, maxRange, userLocation) {
                events.filter { 
                    val eventCal = Calendar.getInstance().apply { 
                        timeInMillis = it.date
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val targetDayCal = (selectedDate.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val sameDay = eventCal.get(Calendar.YEAR) == targetDayCal.get(Calendar.YEAR) &&
                                 eventCal.get(Calendar.DAY_OF_YEAR) == targetDayCal.get(Calendar.DAY_OF_YEAR)
                    
                    val distance = if (it.latitude != null && it.longitude != null) {
                        com.example.arogyasahaya.utils.LocationHelper.calculateDistance(userLocation.first, userLocation.second, it.latitude, it.longitude)
                    } else 0.0
                    
                    val isNational = it.type == "ONLINE" || it.type == "GOVERNMENT"
                    sameDay && (isNational || (distance <= maxRange))
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    DynamicCalendarView(
                        events = events,
                        monthOffset = monthOffset,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onMonthChange = { monthOffset += it }
                    )
                }

                // Section: News/Campaign Feed (Newspaper Style)
                val onlineEvents = events.filter { it.type == "ONLINE" || it.type == "GOVERNMENT" }
                if (onlineEvents.isNotEmpty()) {
                    item {
                        Text(
                            "Government Health Feed",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(onlineEvents) { news ->
                        HealthNewsCard(news)
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        "Events for ${SimpleDateFormat("MMM d").format(selectedDate.time)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (filteredEvents.isEmpty() && !isSyncing) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No events for this date.", color = Color.Gray)
                        }
                    }
                }
                
                items(filteredEvents) { event ->
                    val dist = if (event.latitude != null && event.longitude != null) {
                        com.example.arogyasahaya.utils.LocationHelper.calculateDistance(userLocation.first, userLocation.second, event.latitude, event.longitude)
                    } else 0.0

                    EventCard(
                        event = event,
                        distance = dist,
                        onClick = {
                            selectedEventForDetails = event
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicCalendarView(
    events: List<AshaEvent>,
    monthOffset: Int,
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    onMonthChange: (Int) -> Unit
) {
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
            val calendar = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
            val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Get first day of month and max days
            val firstDayCalendar = calendar.clone() as Calendar
            firstDayCalendar.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = firstDayCalendar.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMonthChange(-1) }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text(
                        "${currentMonth.uppercase()} $currentYear", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 16.sp, 
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { onMonthChange(1) }) { Icon(Icons.Default.ChevronRight, null) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LegendItem("C", Color(0xFF1976D2))
                    LegendItem("V", Color(0xFFFFA000))
                    LegendItem("X", Color(0xFF2E7D32)) // Vaccination
                    LegendItem("E", Color.Red) // Emergency
                    LegendItem("O", Color(0xFFFF6D00)) // Online
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Days of Week Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(day, fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.5f), fontWeight = FontWeight.Black, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val days = mutableListOf<Int?>()
            for (i in 0 until firstDayOfWeek) days.add(null)
            for (i in 1..daysInMonth) days.add(i)
            
            val rows = days.chunked(7)
            
            rows.forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowDays.forEach { day ->
                        if (day == null) {
                            Spacer(modifier = Modifier.size(36.dp))
                        } else {
                            val dayCal = calendar.clone() as Calendar
                            dayCal.set(Calendar.DAY_OF_MONTH, day)
                            
                            val dayEvents = events.filter { 
                                val eventCal = Calendar.getInstance().apply { 
                                    timeInMillis = it.date
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }
                                val targetDayCal = (dayCal.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }
                                eventCal.get(Calendar.YEAR) == targetDayCal.get(Calendar.YEAR) &&
                                eventCal.get(Calendar.DAY_OF_YEAR) == targetDayCal.get(Calendar.DAY_OF_YEAR)
                            }
                            
                            val isCamp = dayEvents.any { it.type == "CAMP" }
                            val isVisit = dayEvents.any { it.type == "VISIT" }
                            val isVaccination = dayEvents.any { it.type == "VACCINATION" }
                            val isEmergency = dayEvents.any { it.type == "EMERGENCY" }
                            
                            val isSelected = selectedDate.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                           selectedDate.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                            
                            val indicatorColor = when {
                                isEmergency -> Color.Red
                                isVaccination -> Color(0xFF2E7D32) // Green
                                isCamp -> Color(0xFF1976D2) // Blue
                                isVisit -> Color(0xFFFFA000) // Yellow
                                else -> Color.Transparent
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if(isSelected) Color(0xFF1976D2).copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { onDateSelected(dayCal) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = if (dayEvents.isNotEmpty() || isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if(isSelected) Color(0xFF1976D2) else Color.Black
                                    )
                                    // Stacked Indicators
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (isEmergency) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.Red))
                                        if (isVaccination) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                                        if (isCamp) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF1976D2)))
                                        if (isVisit) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFFA000)))
                                        if (dayEvents.any { it.type == "ONLINE" }) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFF6D00)))
                                    }
                                }
                            }
                        }
                    }
                    repeat(7 - rowDays.size) {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
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
fun UpcomingEventCard(event: AshaEvent, daysLeft: Int) {
    val color = when(event.type) {
        "VACCINATION" -> Color(0xFF2E7D32)
        "EMERGENCY" -> Color.Red
        "VISIT" -> Color(0xFFFFA000)
        "ONLINE" -> Color(0xFFFF6D00)
        "GOVERNMENT" -> Color(0xFF7B1FA2)
        else -> Color(0xFF1976D2)
    }
    
    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                if(event.type == "VACCINATION") Icons.Default.Info else Icons.Default.Event,
                contentDescription = null,
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(event.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(if(daysLeft == 0) "Today" else "In $daysLeft days", fontSize = 14.sp, color = color, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    event: AshaEvent,
    distance: Double,
    onClick: () -> Unit
) {
    val color = when(event.type) {
        "VACCINATION" -> Color(0xFF2E7D32)
        "EMERGENCY" -> Color.Red
        "VISIT" -> Color(0xFFFFA000)
        "ONLINE" -> Color(0xFFFF6D00)
        "GOVERNMENT" -> Color(0xFF7B1FA2)
        else -> Color(0xFF1976D2)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if(event.type == "VACCINATION") Icons.Default.Info else Icons.Default.Favorite,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                val isNational = event.type == "ONLINE" || event.type == "GOVERNMENT"
                Text(event.title, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    if(isNational) event.location else "${event.location} (${String.format("%.1f", distance)} km)", 
                    fontSize = 14.sp, 
                    color = Color.Gray
                )
                Text(event.time ?: "Morning", fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EventDetailsContent(event: AshaEvent, onSetReminder: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
        Text(event.title, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(event.type, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        DetailRow(Icons.Default.Schedule, "${SimpleDateFormat("MMM d, yyyy").format(Date(event.date))} at ${event.time ?: "Time TBA"}")
        DetailRow(Icons.Default.LocationOn, event.location)
        DetailRow(Icons.Default.Person, "Organizer: ${event.organizer ?: "ASHA Team"}")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(event.description, fontSize = 18.sp, color = Color.Gray, lineHeight = 24.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onSetReminder,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.NotificationsActive, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if(event.reminderEnabled) "Reminder Set" else "Set Reminder", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 18.sp, color = Color.DarkGray)
    }
}

@Composable
fun HealthNewsCard(event: AshaEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if(event.type == "ONLINE") Color(0xFFFF6D00) else Color(0xFF7B1FA2),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        event.type,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    SimpleDateFormat("MMM d, yyyy").format(Date(event.date)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(event.title, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 26.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(event.description, fontSize = 16.sp, color = Color.DarkGray, maxLines = 3)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Source: ${event.organizer ?: "Public Health Dept"}", fontSize = 12.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String, String, String, String, Double?, Double?) -> Unit,
    currentLat: Double,
    currentLon: Double
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00 AM") }
    var organizer by remember { mutableStateOf("ASHA Unit") }
    var type by remember { mutableStateOf("CAMP") }
    var eventLat by remember { mutableStateOf<Double?>(null) }
    var eventLon by remember { mutableStateOf<Double?>(null) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Health Event", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.1f), contentColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Date: ${SimpleDateFormat("MMM d, yyyy").format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))}")
                }

                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { 
                        eventLat = currentLat
                        eventLon = currentLon
                        location = "Current GPS Location"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.MyLocation, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (eventLat != null) "GPS Captured: ${String.format("%.4f", eventLat)}" else "Use My Current Location")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = organizer, onValueChange = { organizer = it }, label = { Text("Organizer") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                
                Text("Event Type", fontWeight = FontWeight.Bold)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(100.dp)
                ) {
                    val types = listOf("CAMP", "VISIT", "VACCINATION", "EMERGENCY")
                    items(types) { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = type == t, onClick = { type = t })
                            Text(t.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val selectedMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val normalizedDate = Calendar.getInstance().apply {
                    timeInMillis = selectedMillis
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onSave(title, normalizedDate, location, description, type, time, organizer, eventLat, eventLon)
            }) { Text("Post Event") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
