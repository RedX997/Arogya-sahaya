package com.example.arogyasahaya.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.example.arogyasahaya.data.local.entity.Appointment
import java.util.*
import androidx.compose.ui.res.stringResource
import com.example.arogyasahaya.R
import com.example.arogyasahaya.ui.theme.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val appointments by viewModel.allAppointments.observeAsState(initial = emptyList())
    val vitals by viewModel.allVitals.observeAsState(initial = emptyList())
    var showAddView by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appointments_title), color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddView = !showAddView }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Appointment", tint = Color(0xFF2E7D32))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            if (showAddView) {
                item {
                    AddAppointmentCard(onSave = { name, time, notes ->
                        viewModel.addAppointment(name, time, notes)
                        showAddView = false
                    })
                }
            }

            val currentTime = System.currentTimeMillis()
            val upcoming = appointments.filter { it.dateTime >= currentTime }
            val past = appointments.filter { it.dateTime < currentTime }

            if (upcoming.isNotEmpty()) {
                item { Text(stringResource(R.string.upcoming), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                
                items(upcoming) { appointment ->
                    AppointmentItem(appointment, onDelete = { viewModel.deleteAppointment(appointment) })
                    
                    // Summary button logic from mockup
                    if (appointment == upcoming.first()) {
                        Button(
                            onClick = {
                                val latest = vitals.firstOrNull()
                                val summary = context.getString(R.string.pre_visit_summary, latest?.bpString ?: "N/A", latest?.heartRate?.toString() ?: "N/A", appointment.notes)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                }
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_summary)))
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.generate_summary), color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (past.isNotEmpty()) {
                item { Text(stringResource(R.string.past), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                items(past) { appointment ->
                    AppointmentItem(appointment, onDelete = { viewModel.deleteAppointment(appointment) })
                }
            }
            
            if (appointments.isEmpty() && !showAddView) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_appointments), color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentItem(appointment: Appointment, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault())
    val diff = appointment.dateTime - System.currentTimeMillis()
    val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(appointment.doctorName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.hospital_phc), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                IconButton(onClick = onDelete) {
                    Text("X", color = Color.Red.copy(alpha = 0.5f))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sdf.format(Date(appointment.dateTime)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (diff > 0) {
                    Surface(color = Color(0xFF2E7D32).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = if (daysLeft == 0) stringResource(R.string.today) else stringResource(R.string.in_days, daysLeft),
                            color = Color(0xFFA5D6A7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            if (appointment.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.notes_display, appointment.notes), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun AddAppointmentCard(onSave: (String, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val stf = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.add_appointment_label), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.doctor_name_label)) },
                placeholder = { Text(stringResource(R.string.doctor_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        DatePickerDialog(context, { _, y, m, d ->
                            val newDate = selectedDate.clone() as Calendar
                            newDate.set(Calendar.YEAR, y)
                            newDate.set(Calendar.MONTH, m)
                            newDate.set(Calendar.DAY_OF_MONTH, d)
                            selectedDate = newDate
                        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(stringResource(R.string.pick_date), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Button(
                    onClick = {
                        TimePickerDialog(context, { _, h, min ->
                            val newTime = selectedDate.clone() as Calendar
                            newTime.set(Calendar.HOUR_OF_DAY, h)
                            newTime.set(Calendar.MINUTE, min)
                            selectedDate = newTime
                        }, selectedDate.get(Calendar.HOUR_OF_DAY), selectedDate.get(Calendar.MINUTE), false).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(stringResource(R.string.pick_time), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            
            Text(stringResource(R.string.selected_date_time, sdf.format(selectedDate.time), stf.format(selectedDate.time)), color = Color(0xFFA5D6A7), fontSize = 14.sp)

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.doctor_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Button(
                onClick = { if(name.isNotEmpty()) onSave(name, selectedDate.timeInMillis, notes) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text(stringResource(R.string.save_appointment), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
