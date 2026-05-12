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
        messages.add(ChatMessage("Me", text, System.currentTimeMillis(), true))
        
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CARE CIRCLE", fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 2.sp)
                            Text(if (familyMembers.isEmpty()) "Add family to start" else "${familyMembers.size} MEMBERS SYNCED", fontSize = 10.sp, color = HealthGreen, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Call Group */ }) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(HealthGreen.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VideoCall, contentDescription = "Video Call", tint = HealthGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth().imePadding(),
                    tonalElevation = 0.dp,
                    color = Color.Transparent
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* Attachment */ }) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.Gray)
                            }
                            TextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = { Text("Update your family...") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            IconButton(
                                onClick = { sendMessage(messageText) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (messageText.isNotEmpty()) HealthGreen else Color.Gray.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    val bubbleColor = if (message.isMe) HealthBlue else MaterialTheme.colorScheme.surface
    val contentColor = if (message.isMe) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isMe) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(HealthBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(message.sender.take(1), fontWeight = FontWeight.Black, color = HealthBlue, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start) {
            if (!message.isMe) {
                Text(message.sender, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isMe) 20.dp else 4.dp,
                    bottomEnd = if (message.isMe) 4.dp else 20.dp
                ),
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(message.message, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        sdf.format(Date(message.time)),
                        fontSize = 9.sp,
                        color = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }
        
        if (message.isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(HealthBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("ME", fontWeight = FontWeight.Black, color = Color.White, fontSize = 10.sp)
            }
        }
    }
}
