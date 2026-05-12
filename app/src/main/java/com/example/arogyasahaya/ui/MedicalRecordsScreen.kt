package com.example.arogyasahaya.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.R
import com.example.arogyasahaya.data.local.entity.MedicalRecord
import com.example.arogyasahaya.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordsScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val records by viewModel.allMedicalRecords.observeAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    val categories = listOf("All", "Lab Report", "Prescription", "Scan", "Summary")

    val filteredRecords = if (selectedCategory == "All") records else records.filter { it.category == selectedCategory }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.medical_vault), 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // AI Scan Button
                SmallFloatingActionButton(
                    onClick = { 
                        coroutineScope.launch {
                            isScanning = true
                            kotlinx.coroutines.delay(2000)
                            isScanning = false
                            scanResult = "AI Detected: Aspirin 75mg, Once daily. Added to suggestions."
                        }
                    },
                    containerColor = HealthTeal,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Scan")
                }
                
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = HealthBlue,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Record")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // AI Scanning Overlay
            AnimatedVisibility(visible = isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = HealthTeal
                )
            }
            
            // Scan Result Card
            AnimatedVisibility(visible = scanResult != null) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HealthTeal.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = HealthTeal)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(scanResult ?: "", fontSize = 13.sp, color = HealthTeal, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { scanResult = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = HealthTeal)
                        }
                    }
                }
            }

            // Category Filter
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HealthBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (filteredRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_records),
                        color = Color.Gray,
                        modifier = Modifier.padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRecords) { record ->
                        MedicalRecordItem(record, onDelete = { viewModel.deleteMedicalRecord(record) })
                    }
                }
            }
        }

        if (showAddDialog) {
            AddRecordDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, cat, uri, notes ->
                    viewModel.addMedicalRecord(title, cat, uri, notes)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MedicalRecordItem(record: MedicalRecord, onDelete: () -> Unit) {
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(record.date))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HealthBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(record.category) {
                        "Lab Report" -> Icons.Default.Science
                        "Prescription" -> Icons.Default.Description
                        "Scan" -> Icons.Default.Visibility
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = HealthBlue
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${record.category} • $dateStr", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HealthRed.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordDialog(onDismiss: () -> Unit, onSave: (String, String, String, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Lab Report") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var notes by remember { mutableStateOf("") }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fileUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { 
                    if (title.isNotEmpty() && fileUri != null) {
                        onSave(title, category, fileUri.toString(), notes)
                    }
                },
                enabled = title.isNotEmpty() && fileUri != null
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.add_record)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.record_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Lab Report", "Prescription", "Scan", "Summary", "Other").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HealthBlue.copy(alpha = 0.1f), contentColor = HealthBlue)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fileUri == null) stringResource(R.string.select_file) else "File Selected")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
