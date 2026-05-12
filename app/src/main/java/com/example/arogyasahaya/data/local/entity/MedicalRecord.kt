package com.example.arogyasahaya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // Lab Report, Prescription, Scan, Discharge Summary
    val date: Long,
    val fileUri: String,
    val notes: String? = null
)
