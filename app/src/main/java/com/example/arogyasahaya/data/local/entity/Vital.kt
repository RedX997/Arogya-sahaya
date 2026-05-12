package com.example.arogyasahaya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vital_table")
data class Vital(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val sugar: Int?,
    val notes: String? = null
) {
    val bpString: String get() = "$systolic/$diastolic"
}
