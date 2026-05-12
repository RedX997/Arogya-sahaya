package com.example.arogyasahaya.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.arogyasahaya.data.local.entity.Vital

@Dao
interface VitalDao {
    @Insert
    suspend fun insert(vital: Vital): Long

    @Query("SELECT * FROM vital_table ORDER BY date DESC")
    fun getVitals(): LiveData<List<Vital>>

    @Query("SELECT * FROM vital_table ORDER BY date DESC LIMIT 1")
    suspend fun getLatestVital(): Vital?

    @Query("DELETE FROM vital_table")
    suspend fun deleteAll()
}
