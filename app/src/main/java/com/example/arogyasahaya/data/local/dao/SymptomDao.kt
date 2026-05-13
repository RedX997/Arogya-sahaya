package com.example.arogyasahaya.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.arogyasahaya.data.local.entity.Symptom

@Dao
interface SymptomDao {
    @Insert
    suspend fun insert(symptom: Symptom): Long

    @androidx.room.Delete
    suspend fun delete(symptom: Symptom)

    @Query("SELECT * FROM symptom_table ORDER BY date DESC")
    fun getAllSymptoms(): LiveData<List<Symptom>>

    @Query("SELECT * FROM symptom_table ORDER BY date DESC")
    suspend fun getAllSymptomsList(): List<Symptom>

    @Query("SELECT * FROM symptom_table WHERE date >= :since ORDER BY date DESC")
    fun getRecentSymptoms(since: Long): LiveData<List<Symptom>>

    @Query("DELETE FROM symptom_table")
    suspend fun deleteAll()
}
