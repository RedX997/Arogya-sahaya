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
    val type: String, // "CAMP" or "VISIT"
    val isCompleted: Boolean = false
)
