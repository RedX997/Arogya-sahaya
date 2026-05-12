package com.example.arogyasahaya.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import java.text.SimpleDateFormat
import androidx.compose.ui.res.stringResource
import com.example.arogyasahaya.R
import com.example.arogyasahaya.utils.*
import androidx.compose.animation.*
import androidx.compose.material.icons.filled.Warning
import com.example.arogyasahaya.ui.theme.*
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.material.icons.filled.PhotoCamera
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var totalTablets by remember { mutableStateOf("30") }
    var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }
    var frequency by remember { mutableStateOf("Daily") }
    var timeSlot by remember { mutableStateOf("Morning") } // Morning, Noon, Night, Custom
    var pillImageUri by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val file = File(context.filesDir, "pill_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            pillImageUri = file.absolutePath
        }
    }

    val allMedicines by viewModel.allMedicines.observeAsState(initial = emptyList())
    val activeMedicineNames = allMedicines.map { it.name }
    
    var interaction by remember { mutableStateOf<Interaction?>(null) }
    
    LaunchedEffect(name) {
        interaction = DrugInteractionHelper.checkInteraction(name, activeMedicineNames)
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun updateTime(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        selectedTime = calendar
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_medicine_title), color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Medicine Name
            Text(stringResource(R.string.medicine_name_label), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Interaction Warning Banner
            AnimatedVisibility(
                visible = interaction != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                interaction?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (it.riskLevel == RiskLevel.HIGH) HealthRedLight else HealthOrangeLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp, 
                            if (it.riskLevel == RiskLevel.HIGH) HealthRed else HealthOrange
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning, 
                                contentDescription = null, 
                                tint = if (it.riskLevel == RiskLevel.HIGH) HealthRed else HealthOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "MEDICAL ALERT: ${it.riskLevel} RISK",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (it.riskLevel == RiskLevel.HIGH) HealthRed else HealthOrange
                                )
                                Text(
                                    text = it.description,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Dosage
            Text(stringResource(R.string.dosage_label), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                placeholder = { Text(stringResource(R.string.dosage_placeholder), color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Reminder Time
            Text(stringResource(R.string.reminder_time_label), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    stringResource(R.string.morning) to "Morning",
                    stringResource(R.string.noon) to "Noon",
                    stringResource(R.string.night) to "Night"
                ).forEach { (label, slot) ->
                    val isSelected = timeSlot == slot
                    Button(
                        onClick = {
                            timeSlot = slot
                            when(slot) {
                                "Morning" -> updateTime(8, 0)
                                "Noon" -> updateTime(14, 0)
                                "Night" -> updateTime(21, 0)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text(stringResource(R.string.set_exact_time), color = Color.Gray, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .clickable {
                        TimePickerDialog(
                            context,
                            { _, hour, min ->
                                updateTime(hour, min)
                                timeSlot = "Custom"
                            },
                            selectedTime.get(Calendar.HOUR_OF_DAY),
                            selectedTime.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeFormat.format(selectedTime.time),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Total Tablets
            Text(stringResource(R.string.total_tablets_label), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = totalTablets,
                onValueChange = { totalTablets = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Frequency
            Text(stringResource(R.string.frequency_label), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    stringResource(R.string.daily) to "Daily",
                    stringResource(R.string.one_time) to "One-time"
                ).forEach { (label, freq) ->
                    val isSelected = frequency == freq
                    Button(
                        onClick = { frequency = freq },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Pill Photo Section
            Text("PILL IDENTIFICATION (Optional)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .clickable { cameraLauncher.launch() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pillImageUri != null) {
                    AsyncImage(
                        model = pillImageUri,
                        contentDescription = "Pill Photo",
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Pill photo captured ✅", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Take a photo of the pill", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank() && dosage.isNotBlank()) {
                        viewModel.addMedicine(
                            name = name,
                            dosage = dosage,
                            time = selectedTime.timeInMillis,
                            totalTablets = totalTablets.toIntOrNull() ?: 30,
                            frequency = frequency,
                            pillImageUri = pillImageUri
                        )
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save_set_reminder), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
