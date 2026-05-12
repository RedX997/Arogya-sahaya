package com.example.arogyasahaya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointment_table")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val doctorName: String,
    val dateTime: Long,
    val notes: String
)
