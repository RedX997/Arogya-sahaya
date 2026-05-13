package com.example.arogyasahaya.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.arogyasahaya.R
import com.example.arogyasahaya.data.local.entity.Symptom
import com.example.arogyasahaya.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val symptoms by viewModel.allSymptoms.observeAsState(initial = emptyList())
    var noteText by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    val context = LocalContext.current

    val allTags = listOf("Headache", "Chest pain", "Stomach pain", "Dizzy", "Fatigue", "Nausea", "Back pain", "General")

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            noteText = if (noteText.isEmpty()) spokenText else "$noteText $spokenText"
        }
    }

    fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe how you're feeling...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input not available", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.symptom_journal_title),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Section: Body Map ---
            item {
                Text("TAP YOUR PAIN AREA", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    BodyMapComponent(
                        onRegionSelected = { tag ->
                            if (selectedTags.contains(tag)) selectedTags.remove(tag) else selectedTags.add(tag)
                        },
                        selectedRegions = selectedTags.toList(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(16.dp)
                    )
                }
            }

            // --- Section: Tag Chips ---
            item {
                Text("QUICK TAGS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allTags) { tag ->
                        val isSelected = selectedTags.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                            },
                            label = { Text(tag, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HealthGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // --- Section: Text Input ---
            item {
                Text("DESCRIBE YOUR SYMPTOMS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(stringResource(R.string.add_note), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput() }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = HealthGreen)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = HealthGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // --- Section: Save Button ---
            item {
                Button(
                    onClick = {
                        if (noteText.isNotEmpty() || selectedTags.isNotEmpty()) {
                            viewModel.addSymptom(noteText, selectedTags.toList())
                            noteText = ""
                            selectedTags.clear()
                            Toast.makeText(context, "Symptom saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please add a note or tap an area/tag", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_entry), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            }

            // --- Section: Past Entries Header ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.past_entries), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (symptoms.isNotEmpty()) {
                        Text("${symptoms.size} entries", color = HealthGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Section: Empty State ---
            if (symptoms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No symptoms logged yet", color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Tap an area or add a note above", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- Section: Past Entries List ---
            items(symptoms, key = { it.id }) { symptom ->
                SymptomItem(symptom = symptom, onDelete = { viewModel.deleteSymptom(symptom) })
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SymptomItem(symptom: Symptom, onDelete: () -> Unit = {}) {
    val sdf = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            // Date badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(HealthGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val dayStr = SimpleDateFormat("d", Locale.getDefault()).format(Date(symptom.date))
                val monStr = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(symptom.date))
                Text(dayStr, color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(monStr, color = HealthGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Tags chips
                if (symptom.tags.isNotEmpty()) {
                    val tagList = symptom.tags.split(",").filter { it.isNotBlank() }
                    if (tagList.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(tagList) { tag ->
                                Surface(
                                    color = HealthGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        tag.trim(),
                                        color = HealthGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Notes
                if (symptom.notes.isNotEmpty()) {
                    Text(symptom.notes, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(sdf.format(Date(symptom.date)), color = Color.Gray, fontSize = 11.sp)
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun BodyMapComponent(
    onRegionSelected: (String) -> Unit,
    selectedRegions: List<String>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val x = offset.x / w
            val y = offset.y / h

            when {
                y < 0.18 -> onRegionSelected("Headache")
                y in 0.18..0.38 && x in 0.28..0.72 -> onRegionSelected("Chest pain")
                y in 0.38..0.62 && x in 0.28..0.72 -> onRegionSelected("Stomach pain")
                y > 0.62 && (x < 0.42 || x > 0.58) -> onRegionSelected("Fatigue")
                else -> onRegionSelected("General")
            }
        }
    }) {
        val w = size.width
        val h = size.height

        // --- Head ---
        drawCircle(
            color = if (selectedRegions.contains("Headache")) Color.Red.copy(0.3f) else primaryColor.copy(0.12f),
            radius = w * 0.1f,
            center = Offset(w * 0.5f, h * 0.09f)
        )
        drawCircle(
            color = primaryColor.copy(0.6f),
            radius = w * 0.1f,
            center = Offset(w * 0.5f, h * 0.09f),
            style = Stroke(width = 3f)
        )

        // --- Neck ---
        drawRect(
            color = primaryColor.copy(0.15f),
            topLeft = Offset(w * 0.46f, h * 0.18f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.05f)
        )

        // --- Torso ---
        val torsoColor = when {
            selectedRegions.contains("Chest pain") -> Color.Red.copy(0.25f)
            else -> primaryColor.copy(0.12f)
        }
        drawRoundRect(
            color = torsoColor,
            topLeft = Offset(w * 0.32f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
        )
        drawRoundRect(
            color = primaryColor.copy(0.5f),
            topLeft = Offset(w * 0.32f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            style = Stroke(width = 2.5f)
        )

        // --- Abdomen ---
        val abdColor = if (selectedRegions.contains("Stomach pain")) Color.Red.copy(0.25f) else primaryColor.copy(0.1f)
        drawRoundRect(
            color = abdColor,
            topLeft = Offset(w * 0.34f, h * 0.42f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
        )
        drawRoundRect(
            color = primaryColor.copy(0.4f),
            topLeft = Offset(w * 0.34f, h * 0.42f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            style = Stroke(width = 2.5f)
        )

        // --- Arms ---
        // Left arm
        drawRoundRect(
            color = primaryColor.copy(0.12f),
            topLeft = Offset(w * 0.18f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.13f, h * 0.3f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
        )
        drawRoundRect(
            color = primaryColor.copy(0.4f),
            topLeft = Offset(w * 0.18f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.13f, h * 0.3f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
            style = Stroke(width = 2.5f)
        )
        // Right arm
        drawRoundRect(
            color = primaryColor.copy(0.12f),
            topLeft = Offset(w * 0.69f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.13f, h * 0.3f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
        )
        drawRoundRect(
            color = primaryColor.copy(0.4f),
            topLeft = Offset(w * 0.69f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.13f, h * 0.3f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
            style = Stroke(width = 2.5f)
        )

        // --- Legs ---
        val legColor = if (selectedRegions.contains("Fatigue")) Color.Red.copy(0.25f) else primaryColor.copy(0.12f)
        // Left leg
        drawRoundRect(
            color = legColor,
            topLeft = Offset(w * 0.34f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
        )
        drawRoundRect(
            color = primaryColor.copy(0.4f),
            topLeft = Offset(w * 0.34f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
            style = Stroke(width = 2.5f)
        )
        // Right leg
        drawRoundRect(
            color = legColor,
            topLeft = Offset(w * 0.52f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
        )
        drawRoundRect(
            color = primaryColor.copy(0.4f),
            topLeft = Offset(w * 0.52f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f),
            style = Stroke(width = 2.5f)
        )
    }
}
