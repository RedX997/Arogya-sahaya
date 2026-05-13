package com.example.arogyasahaya.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: HealthViewModel,
    onLoginSuccess: () -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var showGooglePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Mock "Saved" Google Passwords for the demo
    val mockGoogleAccounts = remember { mutableMapOf("arunpattar13503@gmail.com" to "arun@123") }

    if (showGooglePicker) {
        GoogleSignInDialog(
            onDismiss = { showGooglePicker = false },
            savedPasswords = mockGoogleAccounts,
            onAccountSelected = { selectedEmail, selectedPassword ->
                showGooglePicker = false
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    // Simulate processing and verification
                    kotlinx.coroutines.delay(1500)
                    
                    val savedPassword = mockGoogleAccounts[selectedEmail]
                    if (savedPassword != null && selectedPassword != savedPassword) {
                        errorMessage = "Google verification failed: Incorrect password for $selectedEmail"
                    } else if (selectedPassword.length < 6) {
                        errorMessage = "Google verification failed: Password too short"
                    } else {
                        viewModel.guestLogin() 
                        onLoginSuccess()
                    }
                    isLoading = false
                }
            },
            onPasswordChanged = { email, newPassword ->
                mockGoogleAccounts[email] = newPassword
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLogin) "Welcome Back" else "Create Account",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isLogin) "Sign in to your health portal" else "Start your health journey with us",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    val result = if (isLogin) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(name, email, password)
                    }
                    
                    if (result == null) {
                        onLoginSuccess()
                    } else {
                        errorMessage = result
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && (isLogin || name.isNotEmpty())
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(if (isLogin) "Sign In" else "Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(" OR ", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In (More realistic picker)
        OutlinedButton(
            onClick = { showGooglePicker = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign in with Google", color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Guest Login
        Text(
            text = "Continue as Guest",
            modifier = Modifier.clickable {
                viewModel.guestLogin()
                onLoginSuccess()
            }.padding(8.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Sign In")
        }
    }
}

@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    savedPasswords: Map<String, String>,
    onAccountSelected: (String, String) -> Unit,
    onPasswordChanged: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 32.sp)
                Text(if (isChangingPassword) "Change Password" else "Sign in with Google", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                if (!isChangingPassword) {
                    Text("to continue to Arogya Sahaya", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isChangingPassword) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Setting new password for $email", fontSize = 14.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Text("Use at least 8 characters for a secure password.", fontSize = 11.sp, color = Color.Gray)
                    }
                } else if (!showPassword) {
                    Text("Choose an account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoogleAccountItem("Arun Pattar", "arunpattar13503@gmail.com") {
                            email = "arunpattar13503@gmail.com"
                            showPassword = true
                        }
                        GoogleAccountItem("Use another account", "") {
                            // In real app, show input
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Welcome", fontSize = 20.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(email, fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Enter your password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Forgot password?", 
                                color = Color(0xFF4285F4), 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isChangingPassword = true }
                            )
                            Text(
                                "Change password", 
                                color = Color.Gray, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isChangingPassword = true }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isChangingPassword) {
                Button(
                    onClick = { 
                        if (newPassword.length >= 6) {
                            onPasswordChanged(email, newPassword)
                            isChangingPassword = false
                            password = "" // Reset entered password to force re-entry
                        }
                    },
                    enabled = newPassword.length >= 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("Save New Password")
                }
            } else if (showPassword) {
                Button(
                    onClick = { 
                        if (password.isNotEmpty()) {
                            onAccountSelected(email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("Verify & Sign In")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isChangingPassword) isChangingPassword = false else onDismiss()
            }) { Text(if (isChangingPassword) "Back" else "Cancel") }
        }
    )
}

@Composable
fun GoogleAccountItem(name: String, email: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
            if (email.isNotEmpty()) {
                Text(name.take(1), fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (email.isNotEmpty()) {
                Text(email, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
