package com.example.arogyasahaya.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class AshaReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Health Event"
        val location = inputData.getString("location") ?: "Nearby"
        val time = inputData.getString("time") ?: ""
        
        NotificationHelper.showNotification(
            applicationContext,
            "Upcoming Event: $title",
            "Starting at $time in $location. Don't forget to attend!"
        )
        return Result.success()
    }

    companion object {
        fun scheduleReminders(context: Context, eventId: Int, title: String, location: String, time: String, eventTimestamp: Long) {
            val workManager = WorkManager.getInstance(context)
            
            // 1 Day Before
            val delay1Day = (eventTimestamp - System.currentTimeMillis()) - TimeUnit.DAYS.toMillis(1)
            if (delay1Day > 0) {
                val data = workDataOf("title" to title, "location" to location, "time" to time)
                val request = OneTimeWorkRequestBuilder<AshaReminderWorker>()
                    .setInitialDelay(delay1Day, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("asha_reminder_$eventId")
                    .build()
                workManager.enqueueUniqueWork("asha_1day_$eventId", ExistingWorkPolicy.REPLACE, request)
            }

            // 2 Hours Before
            val delay2Hours = (eventTimestamp - System.currentTimeMillis()) - TimeUnit.HOURS.toMillis(2)
            if (delay2Hours > 0) {
                val data = workDataOf("title" to title, "location" to location, "time" to time)
                val request = OneTimeWorkRequestBuilder<AshaReminderWorker>()
                    .setInitialDelay(delay2Hours, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("asha_reminder_$eventId")
                    .build()
                workManager.enqueueUniqueWork("asha_2hours_$eventId", ExistingWorkPolicy.REPLACE, request)
            }
        }
        
        fun cancelReminders(context: Context, eventId: Int) {
            WorkManager.getInstance(context).cancelAllWorkByTag("asha_reminder_$eventId")
        }
    }
}
