package com.example.arogyasahaya.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log

class ChatSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>
                for (pdu in pdus) {
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = smsMessage.displayOriginatingAddress
                    val body = smsMessage.displayMessageBody
                    
                    if (body.startsWith("[AS_CHAT]")) {
                        val realMessage = body.removePrefix("[AS_CHAT]").trim()
                        
                        // Broadcast locally to the Chat Screen
                        val chatIntent = Intent("COM_AROGYA_SAHAYA_CHAT_UPDATE").apply {
                            putExtra("sender", sender)
                            putExtra("message", realMessage)
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(chatIntent)
                        
                        // If we want to prevent the SMS from showing in the system app:
                        // abortBroadcast() // Note: Only works for ordered broadcasts
                    }
                }
            }
        }
    }
}
