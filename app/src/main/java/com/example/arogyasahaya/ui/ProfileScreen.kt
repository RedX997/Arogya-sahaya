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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var appLanguage by remember { mutableStateOf(prefs.getString("app_language", "English") ?: "English") }
    
    var fullName by remember { mutableStateOf(prefs.getString("profile_name", "Nikita R") ?: "Nikita R") }
    var age by remember { mutableStateOf(prefs.getString("profile_age", "65") ?: "65") }
    var chronicConditions by remember { mutableStateOf(prefs.getString("chronic_conditions", "Cold & Cough") ?: "Cold & Cough") }
    var bloodGroup by remember { mutableStateOf(prefs.getString("blood_group", "—") ?: "—") }
    
    var emergencyName by remember { mutableStateOf(prefs.getString("emergency_name", "Papa") ?: "Papa") }
    var emergencyPhone by remember { mutableStateOf(prefs.getString("emergency_phone", "9972259921") ?: "9972259921") }

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
                    IconButton(onClick = onBack) {
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
                    stringResource(R.string.logged_in_as, "xyz@gmail.com"),
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
                        prefs.edit().putBoolean("dark_mode", it).apply()
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
                                prefs.edit().putString("app_language", selectionOption).apply()
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
                onClick = { /* Handle Edit */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("✏️ " + stringResource(R.string.edit_profile_btn), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLogout,
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
