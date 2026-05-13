package com.example.arogyasahaya.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.arogyasahaya.utils.ReminderWorker
import com.example.arogyasahaya.utils.PreferenceManager
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.arogyasahaya.R
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToCaregiver: () -> Unit,
    onNavigateToTargets: () -> Unit
) {
    val context = LocalContext.current
    val prefManager = remember { PreferenceManager(context) }
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var voiceReminder by remember { mutableStateOf(prefs.getBoolean("voice_reminder", true)) }
    var rescheduleOnRestart by remember { mutableStateOf(prefs.getBoolean("reschedule_restart", true)) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var dailyReport by remember { mutableStateOf(prefs.getBoolean("daily_report", false)) }
    
    var reminderType by remember { mutableStateOf(prefs.getString("reminder_type", "Sound + vibrate") ?: "Sound + vibrate") }
    var snoozeDuration by remember { mutableStateOf(prefs.getString("snooze_duration", "10 minutes") ?: "10 minutes") }
    var quietHours by remember { mutableStateOf(prefs.getString("quiet_hours", "10 PM - 7 AM") ?: "10 PM - 7 AM") }
    var appLanguage by remember { mutableStateOf(prefs.getString("app_language", "English") ?: "English") }
    var voiceLanguage by remember { mutableStateOf(prefs.getString("voice_language", "Hindi") ?: "Hindi") }
    var textSize by remember { mutableStateOf(prefs.getString("text_size", "Large") ?: "Large") }
    var highContrast by remember { mutableStateOf(prefs.getBoolean("high_contrast", false)) }
    var sosNumber by remember { mutableStateOf(prefs.getString("sos_number", "112") ?: "112") }
    var emergencyName by remember { mutableStateOf(prefs.getString("emergency_name", "") ?: "") }
    var emergencyPhone by remember { mutableStateOf(prefs.getString("emergency_phone", "") ?: "") }

    var profileName by remember { mutableStateOf(prefManager.getUserName()) }
    var profileAge by remember { mutableStateOf(prefManager.getUserAge()) }
    var profileGender by remember { mutableStateOf(prefManager.getUserGender()) }
    var profilePhotoUri by remember { mutableStateOf(prefManager.getUserPhoto()) }

    var showEditProfile by remember { mutableStateOf(false) }

    if (showEditProfile) {
        EditProfileDialog(
            currentName = profileName,
            currentAge = profileAge,
            currentGender = profileGender,
            currentPhotoUri = profilePhotoUri,
            onDismiss = { showEditProfile = false },
            onSave = { name, age, gender, photoUri ->
                profileName = name
                profileAge = age
                profileGender = gender
                profilePhotoUri = photoUri
                prefManager.saveProfileData(name, age, gender, photoUri)
                showEditProfile = false
            }
        )
    }

    var showSelectionDialog by remember { mutableStateOf<SelectionDialogData?>(null) }
    var showInputDialog by remember { mutableStateOf<InputDialogData?>(null) }

    showSelectionDialog?.let { data ->
        SelectionDialog(
            title = data.title,
            options = data.options,
            currentSelection = data.currentSelection,
            onDismiss = { showSelectionDialog = null },
            onSelect = {
                data.onSelect(it)
                showSelectionDialog = null
            }
        )
    }

    showInputDialog?.let { data ->
        InputDialog(
            title = data.title,
            currentValue = data.currentValue,
            label = data.label,
            onDismiss = { showInputDialog = null },
            onSave = {
                data.onSave(it)
                showInputDialog = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), color = MaterialTheme.colorScheme.onBackground) },
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
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showEditProfile = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (profilePhotoUri.isNotEmpty()) {
                            AsyncImage(
                                model = profilePhotoUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF2E7D32).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = profileName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                                Text(if (initials.isNotEmpty()) initials else "NU", color = Color(0xFF81C784), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profileName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${stringResource(R.string.age)} $profileAge · $profileGender · ${stringResource(R.string.edit_profile)}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            // Reminder Settings
            item {
                SettingsSection(title = stringResource(R.string.reminder_settings)) {
                    SettingsRow(
                        title = stringResource(R.string.reminder_type), 
                        value = reminderType,
                        onClick = {
                            showSelectionDialog = SelectionDialogData(
                                title = context.getString(R.string.reminder_type_dialog_title),
                                options = listOf("Sound + vibrate", "Sound only", "Vibrate only", "Silent"),
                                currentSelection = reminderType,
                                onSelect = {
                                    reminderType = it
                                    prefs.edit().putString("reminder_type", it).apply()
                                }
                            )
                        }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.voice_reminder),
                        subtitle = stringResource(R.string.voice_reminder_desc),
                        checked = voiceReminder,
                        onCheckedChange = { 
                            voiceReminder = it
                            prefs.edit().putBoolean("voice_reminder", it).apply()
                        }
                    )
                    SettingsRow(
                        title = stringResource(R.string.snooze_duration), 
                        value = snoozeDuration,
                        onClick = {
                            showSelectionDialog = SelectionDialogData(
                                title = context.getString(R.string.snooze_duration_dialog_title),
                                options = listOf("5 minutes", "10 minutes", "15 minutes", "20 minutes"),
                                currentSelection = snoozeDuration,
                                onSelect = {
                                    snoozeDuration = it
                                    prefs.edit().putString("snooze_duration", it).apply()
                                }
                            )
                        }
                    )
                    SettingsRow(
                        title = stringResource(R.string.quiet_hours), 
                        value = quietHours, 
                        subtitle = stringResource(R.string.quiet_hours_desc),
                        onClick = {
                            showSelectionDialog = SelectionDialogData(
                                title = context.getString(R.string.quiet_hours_dialog_title),
                                options = listOf("10 PM - 7 AM", "11 PM - 6 AM", "None"),
                                currentSelection = quietHours,
                                onSelect = {
                                    quietHours = it
                                    prefs.edit().putString("quiet_hours", it).apply()
                                }
                            )
                        }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.reschedule_restart),
                        subtitle = stringResource(R.string.reschedule_restart_desc),
                        checked = rescheduleOnRestart,
                        onCheckedChange = { 
                            rescheduleOnRestart = it
                            prefs.edit().putBoolean("reschedule_restart", it).apply()
                        }
                    )
                }
            }

            // Language & Accessibility
            item {
                SettingsSection(title = stringResource(R.string.lang_accessibility)) {
                    SettingsRow(
                        title = stringResource(R.string.test_notification),
                        value = "Send now",
                        onClick = {
                            val data = Data.Builder()
                                .putInt("id", 999)
                                .putString("name", "Test Medicine")
                                .build()
                            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                                .setInputData(data)
                                .build()
                            WorkManager.getInstance(context).enqueue(request)
                            Toast.makeText(context, context.getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsRow(
                        title = stringResource(R.string.app_language), 
                        value = appLanguage,
                        onClick = {
                            showSelectionDialog = SelectionDialogData(
                                title = context.getString(R.string.app_language_dialog_title),
                                options = listOf("English", "Hindi", "Marathi", "Kannada"),
                                currentSelection = appLanguage,
                                onSelect = {
                                    appLanguage = it
                                    prefs.edit().putString("app_language", it).apply()
                                }
                            )
                        }
                    )
                    SettingsRow(
                        title = stringResource(R.string.voice_language), 
                        value = voiceLanguage,
                        onClick = {
                            showSelectionDialog = SelectionDialogData(
                                title = context.getString(R.string.voice_language_dialog_title),
                                options = listOf("English", "Hindi", "Marathi", "Kannada"),
                                currentSelection = voiceLanguage,
                                onSelect = {
                                    voiceLanguage = it
                                    prefs.edit().putString("voice_language", it).apply()
                                }
                            )
                        }
                    )
                    SettingsRow(
                        title = stringResource(R.string.text_size), 
                        value = textSize,
                        onClick = {
                            showSelectionDialog = SelectionDialogData(
                                title = context.getString(R.string.text_size_dialog_title),
                                options = listOf("Small", "Medium", "Large", "Extra Large"),
                                currentSelection = textSize,
                                onSelect = {
                                    textSize = it
                                    prefs.edit().putString("text_size", it).apply()
                                }
                            )
                        }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.dark_mode),
                        checked = darkMode,
                        onCheckedChange = { 
                            darkMode = it
                            prefs.edit().putBoolean("dark_mode", it).apply()
                        }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.high_contrast), 
                        checked = highContrast, 
                        onCheckedChange = {
                            highContrast = it
                            prefs.edit().putBoolean("high_contrast", it).apply()
                        }
                    )
                }
            }

            // Caregiver & Emergency
            item {
                SettingsSection(title = stringResource(R.string.caregiver_emergency)) {
                    SettingsRow(title = stringResource(R.string.caregiver_view), value = stringResource(R.string.open_chevron), onClick = onNavigateToCaregiver)
                    SettingsRow(title = stringResource(R.string.health_targets), value = stringResource(R.string.set_targets_chevron), onClick = onNavigateToTargets)
                    SettingsRow(
                        title = stringResource(R.string.sos_number), 
                        value = sosNumber,
                        onClick = {
                            showInputDialog = InputDialogData(
                                title = "SOS Number",
                                currentValue = sosNumber,
                                label = "Phone Number",
                                onSave = {
                                    sosNumber = it
                                    prefs.edit().putString("sos_number", it).apply()
                                }
                            )
                        }
                    )
                    SettingsRow(
                        title = "Emergency Contact Name", 
                        value = if (emergencyName.isEmpty()) "Set Name" else emergencyName,
                        onClick = {
                            showInputDialog = InputDialogData(
                                title = "Emergency Name",
                                currentValue = emergencyName,
                                label = "Name",
                                onSave = {
                                    emergencyName = it
                                    prefs.edit().putString("emergency_name", it).apply()
                                }
                            )
                        }
                    )
                    SettingsRow(
                        title = "Emergency Contact Phone", 
                        value = if (emergencyPhone.isEmpty()) "Set Phone" else emergencyPhone,
                        onClick = {
                            showInputDialog = InputDialogData(
                                title = "Emergency Phone",
                                currentValue = emergencyPhone,
                                label = "Phone Number",
                                onSave = {
                                    emergencyPhone = it
                                    prefs.edit().putString("emergency_phone", it).apply()
                                }
                            )
                        }
                    )
                    SettingsToggle(
                        title = stringResource(R.string.daily_report),
                        subtitle = stringResource(R.string.daily_report_desc),
                        checked = dailyReport,
                        onCheckedChange = { 
                            dailyReport = it
                            prefs.edit().putBoolean("daily_report", it).apply()
                        }
                    )
                }
            }

            // Data & Backup
            item {
                SettingsSection(title = "DATA & BACKUP") {
                    SettingsRow(title = "Backup health data", value = "Backup now ›")
                    SettingsRow(title = "Restore from backup", value = "")
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(title: String, value: String, subtitle: String? = null, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (value.isNotEmpty()) {
                Text(value, color = Color(0xFF2196F3), fontSize = 14.sp)
            }
        }
        if (subtitle != null) {
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SettingsToggle(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2E7D32))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    currentAge: String,
    currentGender: String,
    currentPhotoUri: String,
    onDismiss: () -> Unit,
    onSave: (name: String, age: String, gender: String, photoUri: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var age by remember { mutableStateOf(currentAge) }
    var gender by remember { mutableStateOf(currentGender) }
    var photoUri by remember { mutableStateOf(currentPhotoUri) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if it's not possible to take persistable permission
            }
            photoUri = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri.isNotEmpty()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = "Add Photo", modifier = Modifier.size(48.dp), tint = Color.Gray)
                    }
                }
                Text(stringResource(R.string.tap_change_photo), color = Color.Gray, fontSize = 12.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text(stringResource(R.string.age)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.gender)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Male", "Female", "Other").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    gender = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, age, gender, photoUri) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

data class SelectionDialogData(
    val title: String,
    val options: List<String>,
    val currentSelection: String,
    val onSelect: (String) -> Unit
)

data class InputDialogData(
    val title: String,
    val currentValue: String,
    val label: String,
    val onSave: (String) -> Unit
)

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    currentSelection: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == currentSelection,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InputDialog(
    title: String,
    currentValue: String,
    label: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
