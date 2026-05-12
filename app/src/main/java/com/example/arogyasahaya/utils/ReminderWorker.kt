package com.example.arogyasahaya.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.arogyasahaya.R
import java.util.*

class ReminderWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private var tts: TextToSpeech? = null

    override fun doWork(): Result {
        val medicineId = inputData.getInt("id", -1)
        val medicineName = inputData.getString("name") ?: "Medicine"
        
        val prefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        // 1. Check Quiet Hours
        if (isInQuietHours(prefs)) {
            // Only vibrate if in quiet hours (or handle as preferred)
            // For now, we'll still post but maybe with lower importance or just vibrate
        }

        // 2. Build Notification
        val takenIntent = Intent(applicationContext, MedicineActionReceiver::class.java).apply {
            action = "ACTION_MARK_TAKEN"
            putExtra("MEDICINE_ID", medicineId)
        }
        
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val takenPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            medicineId,
            takenIntent,
            flags
        )

        val snoozeIntent = Intent(applicationContext, MedicineActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("MEDICINE_ID", medicineId)
            putExtra("MEDICINE_NAME", medicineName)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            medicineId + 1000,
            snoozeIntent,
            flags
        )

        val builder = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText("${applicationContext.getString(R.string.medicine_reminder)}: $medicineName")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(null, true) // Important for showing up on lock screen
            .setAutoCancel(false) // Don't auto-cancel so it keeps beeping
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_edit, applicationContext.getString(R.string.taken), takenPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze (10 min)", snoozePendingIntent)

        val notification = builder.build()
        // Make it beep continuously until dismissed
        notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33
        ) {
            NotificationManagerCompat.from(applicationContext).notify(medicineId, notification)
        }

        // 3. Voice Reminder
        if (prefs.getBoolean("voice_reminder", true)) {
            speakReminder(medicineName)
        }

        return Result.success()
    }

    private fun isInQuietHours(prefs: android.content.SharedPreferences): Boolean {
        // Simple implementation: 10PM to 7AM
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 7
    }

    private fun speakReminder(name: String) {
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.speak("$name lene ka waqt ho gaya", TextToSpeech.QUEUE_FLUSH, null, "med_tts")
            }
        }
    }

    override fun onStopped() {
        super.onStopped()
        tts?.stop()
        tts?.shutdown()
    }
}
