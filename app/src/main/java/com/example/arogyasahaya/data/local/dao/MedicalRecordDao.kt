package com.example.arogyasahaya.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.arogyasahaya.data.local.entity.MedicalRecord

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    fun getAllRecords(): LiveData<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    suspend fun getAllRecordsList(): List<MedicalRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MedicalRecord): Long

    @Delete
    suspend fun deleteRecord(record: MedicalRecord)

    @Query("DELETE FROM medical_records")
    suspend fun deleteAll()
}
