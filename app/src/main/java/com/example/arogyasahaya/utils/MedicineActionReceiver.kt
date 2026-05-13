package com.example.arogyasahaya.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.arogyasahaya.data.local.AppDatabase
import com.example.arogyasahaya.data.repository.HealthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicineActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra("MEDICINE_ID", -1)
        if (medicineId == -1) return

        when (intent.action) {
            "ACTION_MARK_TAKEN" -> {
                val database = AppDatabase.getDatabase(context)
                val repository = HealthRepository(
                    database.medicineDao(),
                    database.vitalDao(),
                    database.symptomDao(),
                    database.appointmentDao(),
                    database.medicalRecordDao(),
                    database.familyMemberDao(),
                    database.ashaEventDao(),
                    com.example.arogyasahaya.data.remote.NetworkModule.apiService
                )
                
                CoroutineScope(Dispatchers.IO).launch {
                    repository.markMedicineAsTaken(medicineId)
                    NotificationManagerCompat.from(context).cancel(medicineId)
                }
            }
            "ACTION_SNOOZE" -> {
                // Cancel current notification
                NotificationManagerCompat.from(context).cancel(medicineId)
                
                // Reschedule for 10 minutes later
                val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
                val workManager = androidx.work.WorkManager.getInstance(context)
                val inputData = androidx.work.Data.Builder()
                    .putInt("id", medicineId)
                    .putString("name", medicineName)
                    .build()

                val snoozeRequest = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(10, java.util.concurrent.TimeUnit.MINUTES)
                    .setInputData(inputData)
                    .build()

                workManager.enqueue(snoozeRequest)
            }
        }
    }
}
