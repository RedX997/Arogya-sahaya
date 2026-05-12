package com.example.arogyasahaya.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.arogyasahaya.data.local.AppDatabase
import com.example.arogyasahaya.data.repository.HealthRepository

class DailyResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = HealthRepository(
            database.medicineDao(),
            database.vitalDao(),
            database.symptomDao(),
            database.appointmentDao(),
            database.medicalRecordDao(),
            database.familyMemberDao(),
            database.ashaEventDao()
        )
        repository.resetDailyMedicines()
        return Result.success()
    }
}
