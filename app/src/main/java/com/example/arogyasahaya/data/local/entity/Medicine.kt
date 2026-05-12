package com.example.arogyasahaya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_table")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val dosage: String,
    val time: Long,
    val isTaken: Boolean = false,
    val totalTablets: Int = 30,
    val remainingTablets: Int = 30,
    val frequency: String = "Daily", // Daily, One-time
    val lastTakenDate: Long = 0,
    val pillImageUri: String? = null
)
