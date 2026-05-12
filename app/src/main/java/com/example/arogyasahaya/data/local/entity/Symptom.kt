package com.example.arogyasahaya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom_table")
data class Symptom(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val notes: String,
    val tags: String // comma separated tags: Headache, Dizzy, etc.
)
