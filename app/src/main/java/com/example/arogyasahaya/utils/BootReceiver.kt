package com.example.arogyasahaya.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.arogyasahaya.data.local.AppDatabase
import com.example.arogyasahaya.data.local.entity.Medicine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                val medicines = db.medicineDao().getAllMedicinesList()
                medicines.forEach { medicine ->
                    if (!medicine.isTaken) {
                        scheduleReminder(context, medicine)
                    }
                }
            }
        }
    }

    private fun scheduleReminder(context: Context, medicine: Medicine) {
        val data = Data.Builder()
            .putInt("id", medicine.id)
            .putString("name", medicine.name)
            .build()

        val delay = medicine.time - System.currentTimeMillis()
        if (delay > 0) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("med_${medicine.id}")
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
