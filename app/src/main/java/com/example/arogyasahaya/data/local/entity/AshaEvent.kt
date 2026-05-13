package com.example.arogyasahaya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asha_events")
data class AshaEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: Long, // timestamp
    val location: String,
    val description: String,
    val type: String, // "CAMP", "VISIT", "VACCINATION", "EMERGENCY"
    val time: String? = null,
    val organizer: String? = null,
    val reminderEnabled: Boolean = false,
    val isCompleted: Boolean = false,
    val status: String = "PENDING",
    val latitude: Double? = null,
    val longitude: Double? = null
)
