package com.example.arogyasahaya.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.R
import com.example.arogyasahaya.utils.PreferenceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefManager = remember { PreferenceManager(context) }
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var darkMode by remember { mutableStateOf(settingsPrefs.getBoolean("dark_mode", false)) }
    var appLanguage by remember { mutableStateOf(settingsPrefs.getString("app_language", "English") ?: "English") }
    
    var fullName by remember { mutableStateOf(prefManager.getUserName()) }
    var userEmail by remember { mutableStateOf(prefManager.getUserEmail()) }
    var age by remember { mutableStateOf(prefManager.getUserAge()) }
    var chronicConditions by remember { mutableStateOf(prefManager.getChronicConditions()) }
    var bloodGroup by remember { mutableStateOf(prefManager.getBloodGroup()) }
    var emergencyName by remember { mutableStateOf(prefManager.getEmergencyName()) }
    var emergencyPhone by remember { mutableStateOf(prefManager.getEmergencyPhone()) }
    var photoUri by remember { mutableStateOf(prefManager.getUserPhoto()) }

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        FullEditProfileDialog(
            currentName = fullName,
            currentAge = age,
            currentChronic = chronicConditions,
            currentBlood = bloodGroup,
            currentEmName = emergencyName,
            currentEmPhone = emergencyPhone,
            onDismiss = { showEditDialog = false },
            onSave = { name, a, chronic, blood, emName, emPhone ->
                fullName = name
                age = a
                chronicConditions = chronic
                bloodGroup = blood
                emergencyName = emName
                emergencyPhone = emPhone
                prefManager.saveProfileData(name, a, prefManager.getUserGender(), photoUri, chronic, blood, emName, emPhone)
                showEditDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1976D2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.my_profile), fontWeight = FontWeight.Bold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
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
                .verticalScroll(rememberScrollState())
        ) {
            // Login info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    stringResource(R.string.logged_in_as, userEmail),
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF1976D2),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dark Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dark_mode), fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = darkMode,
                    onCheckedChange = { 
                        darkMode = it
                        settingsPrefs.edit().putBoolean("dark_mode", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF1976D2)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Select Language
            Text("🌐 " + stringResource(R.string.select_language), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = appLanguage,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("English", "Kannada", "Hindi", "Marathi").forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                appLanguage = selectionOption
                                settingsPrefs.edit().putString("app_language", selectionOption).apply()
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Fields
            ProfileField(stringResource(R.string.name), fullName)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileField(stringResource(R.string.age), age)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileField(stringResource(R.string.chronic_conditions), chronicConditions)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.blood_group) + ": $bloodGroup", color = Color.Gray, fontSize = 14.sp)
            
            Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray.copy(alpha = 0.5f))

            // Emergency Contact
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚨", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.emergency_contact_label), fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileField(stringResource(R.string.contact_name_label), emergencyName)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileField(stringResource(R.string.phone_number_label), emergencyPhone)

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("✏️ " + stringResource(R.string.edit_profile_btn), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onLogout() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("🚪 " + stringResource(R.string.logout), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun FullEditProfileDialog(
    currentName: String,
    currentAge: String,
    currentChronic: String,
    currentBlood: String,
    currentEmName: String,
    currentEmPhone: String,
    onDismiss: () -> Unit,
    onSave: (name: String, age: String, chronic: String, blood: String, emName: String, emPhone: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var age by remember { mutableStateOf(currentAge) }
    var chronic by remember { mutableStateOf(currentChronic) }
    var blood by remember { mutableStateOf(currentBlood) }
    var emName by remember { mutableStateOf(currentEmName) }
    var emPhone by remember { mutableStateOf(currentEmPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Details") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = chronic, onValueChange = { chronic = it }, label = { Text("Chronic Conditions") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = blood, onValueChange = { blood = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth())
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Emergency Contact", fontWeight = FontWeight.Bold, color = Color.Red)
                OutlinedTextField(value = emName, onValueChange = { emName = it }, label = { Text("Contact Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emPhone, onValueChange = { emPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, age, chronic, blood, emName, emPhone) }) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
