package com.example.arogyasahaya.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.arogyasahaya.data.local.entity.*
import com.example.arogyasahaya.data.local.dao.*
import com.example.arogyasahaya.data.local.dao.AshaEventDao

@Database(entities = [Medicine::class, Vital::class, Symptom::class, Appointment::class, MedicalRecord::class, FamilyMember::class, AshaEvent::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun vitalDao(): VitalDao
    abstract fun symptomDao(): SymptomDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun ashaEventDao(): AshaEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
