package com.example.arogyasahaya.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareHelper {
    // Update this to your Render URL after deploying (e.g., https://arogyasahaya-backend.onrender.com/download)
    private const val APP_DOWNLOAD_LINK = "https://arogyasahaya-backend.onrender.com/download"

    fun shareAppInvite(context: Context) {
        val inviteMsg = "I'm using Arogya Sahaya to manage my health and medicines. Download the app here to join my Care Circle: $APP_DOWNLOAD_LINK"
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, inviteMsg)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Invite Family")
        context.startActivity(shareIntent)
    }

    fun shareHealthSummary(context: Context, name: String, score: Int) {
        val summaryMsg = "Health Update for $name:\n" +
                "Health Score: $score/100\n" +
                "I'm keeping track of my vitals and medicines using Arogya Sahaya.\n" +
                "Download the app: $APP_DOWNLOAD_LINK"
        
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summaryMsg)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Health Summary")
        context.startActivity(shareIntent)
    }

    fun sendSmsInvite(context: Context, phoneNumber: String) {
        val inviteMsg = "Join my Care Circle on Arogya Sahaya! Download the app: $APP_DOWNLOAD_LINK"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phoneNumber")).apply {
            putExtra("sms_body", inviteMsg)
        }
        context.startActivity(intent)
    }
}
