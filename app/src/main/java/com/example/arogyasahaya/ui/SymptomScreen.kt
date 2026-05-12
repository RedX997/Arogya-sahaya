package com.example.arogyasahaya.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.data.local.entity.Symptom
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.example.arogyasahaya.R
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val symptoms by viewModel.allSymptoms.observeAsState(initial = emptyList())
    var noteText by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    
    val tags = listOf("Headache", "Dizzy", "Fatigue", "Chest pain", "Nausea")
    val context = LocalContext.current
    
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            noteText = if (noteText.isEmpty()) spokenText else "$noteText $spokenText"
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe how you're feeling...")
        }
        speechLauncher.launch(intent)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.symptom_journal_title), color = MaterialTheme.colorScheme.onBackground) },
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
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.how_feeling_label), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Interactive Body Map
            BodyMapComponent(
                onRegionSelected = { tag ->
                    if (selectedTags.contains(tag)) selectedTags.remove(tag) else selectedTags.add(tag)
                },
                selectedRegions = selectedTags.toList()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            

            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text(stringResource(R.string.add_note), color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput() }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Color(0xFF2E7D32))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (noteText.isNotEmpty() || selectedTags.isNotEmpty()) {
                        viewModel.addSymptom(noteText, selectedTags.toList())
                        noteText = ""
                        selectedTags.clear()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save_entry), fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.past_entries), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(symptoms) { symptom ->
                    SymptomItem(symptom)
                }
            }
        }
    }
}

@Composable
fun SymptomItem(symptom: Symptom) {
    val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(sdf.format(Date(symptom.date)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (symptom.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        symptom.tags.split(",").forEach { tag ->
                            Surface(color = Color.DarkGray, shape = RoundedCornerShape(4.dp)) {
                                Text(tag, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (symptom.notes.isNotEmpty()) {
                    Text(symptom.notes, color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun BodyMapComponent(
    onRegionSelected: (String) -> Unit,
    selectedRegions: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tap the area of pain", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val x = offset.x / w
                    val y = offset.y / h
                    
                    when {
                        y < 0.15 -> onRegionSelected("Headache")
                        y in 0.15..0.35 && x in 0.3..0.7 -> onRegionSelected("Chest pain")
                        y in 0.35..0.6 && x in 0.3..0.7 -> onRegionSelected("Stomach pain")
                        y > 0.6 && (x < 0.4 || x > 0.6) -> onRegionSelected("Fatigue")
                        else -> onRegionSelected("General")
                    }
                }
            }) {
                val w = size.width
                val h = size.height
                
                // Draw Silhoutte
                val bodyPath = Path().apply {
                    // Head
                    addOval(androidx.compose.ui.geometry.Rect(w*0.4f, 0f, w*0.6f, h*0.15f))
                    // Body
                    moveTo(w*0.35f, h*0.15f)
                    lineTo(w*0.65f, h*0.15f)
                    lineTo(w*0.7f, h*0.4f)
                    lineTo(w*0.3f, h*0.4f)
                    close()
                    // Torso
                    addRect(androidx.compose.ui.geometry.Rect(w*0.35f, h*0.15f, w*0.65f, h*0.6f))
                    // Legs
                    addRect(androidx.compose.ui.geometry.Rect(w*0.35f, h*0.6f, w*0.45f, h))
                    addRect(androidx.compose.ui.geometry.Rect(w*0.55f, h*0.6f, w*0.65f, h))
                }
                
                drawPath(
                    path = bodyPath,
                    color = Color.LightGray.copy(alpha = 0.3f)
                )
                
                drawPath(
                    path = bodyPath,
                    color = primaryColor.copy(alpha = 0.5f),
                    style = Stroke(width = 4f)
                )

                // Highlight Selected
                if (selectedRegions.contains("Headache")) {
                    drawCircle(Color.Red.copy(alpha = 0.3f), radius = w*0.08f, center = Offset(w*0.5f, h*0.075f))
                }
                if (selectedRegions.contains("Chest pain")) {
                    drawCircle(Color.Red.copy(alpha = 0.3f), radius = w*0.1f, center = Offset(w*0.5f, h*0.25f))
                }
                if (selectedRegions.contains("Stomach pain")) {
                    drawCircle(Color.Red.copy(alpha = 0.3f), radius = w*0.1f, center = Offset(w*0.5f, h*0.45f))
                }
                if (selectedRegions.contains("Fatigue")) {
                    drawCircle(Color.Red.copy(alpha = 0.3f), radius = w*0.08f, center = Offset(w*0.4f, h*0.8f))
                    drawCircle(Color.Red.copy(alpha = 0.3f), radius = w*0.08f, center = Offset(w*0.6f, h*0.8f))
                }
            }
        }
    }
}
