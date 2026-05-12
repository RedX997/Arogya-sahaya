package com.example.arogyasahaya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val relation: String = "Family",
    val dateAdded: Long = System.currentTimeMillis()
)
