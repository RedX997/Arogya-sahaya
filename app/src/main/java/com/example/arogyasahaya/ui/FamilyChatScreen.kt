package com.example.arogyasahaya.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.arogyasahaya.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String,
    val message: String,
    val time: Long,
    val isMe: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyChatScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val familyMembers by viewModel.allFamilyMembers.observeAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        }
    }

    // Messages state
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Arogya Assistant", "Welcome to your Family Circle! This chat works over SMS, so it works even without Internet.", System.currentTimeMillis() - 86400000, false)
        )
    }

    // Receiver for incoming SMS chat updates
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val sender = intent.getStringExtra("sender") ?: "Family"
                val message = intent.getStringExtra("message") ?: ""
                
                // Find matching member name
                val memberName = familyMembers.find { it.phoneNumber.contains(sender.takeLast(10)) }?.name ?: sender
                messages.add(ChatMessage(memberName, message, System.currentTimeMillis(), false))
            }
        }
        
        val filter = IntentFilter("COM_AROGYA_SAHAYA_CHAT_UPDATE")
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    fun sendMessage(text: String) {
        if (text.isEmpty()) return
        
        // Add to local UI
        messages.add(ChatMessage("Me", text, System.currentTimeMillis(), true))
        
        // Send SMS to all family members (or selected)
        coroutineScope.launch {
            try {
                val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
                val chatPayload = "[AS_CHAT] $text"
                
                familyMembers.forEach { member ->
                    smsManager.sendTextMessage(member.phoneNumber, null, chatPayload, null, null)
                }
            } catch (e: Exception) {
                Log.e("Chat", "Failed to send SMS", e)
            }
        }
        
        messageText = ""
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("FAMILY CIRCLE CHAT", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(if (familyMembers.isEmpty()) "Add family to start syncing" else "${familyMembers.size} members active", fontSize = 11.sp, color = HealthGreen)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Call Group */ }) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Video Call", tint = HealthGreen)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().imePadding(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    if (familyMembers.isEmpty()) {
                        Text(
                            "Note: Add family members to sync chat across phones.",
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            fontSize = 10.sp,
                            color = HealthOrange,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Share a health update...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HealthGreen,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { sendMessage(messageText) },
                            modifier = Modifier.size(48.dp).background(HealthGreen, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            items(messages) { message ->
                ChatBubble(message)
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
    ) {
        if (!message.isMe) {
            Text(message.sender, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        }
        Surface(
            color = if (message.isMe) HealthGreen else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMe) 16.dp else 0.dp,
                bottomEnd = if (message.isMe) 0.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    message.message,
                    color = if (message.isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
                Text(
                    sdf.format(Date(message.time)),
                    fontSize = 10.sp,
                    color = if (message.isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
