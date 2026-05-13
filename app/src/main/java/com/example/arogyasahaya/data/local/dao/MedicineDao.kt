package com.example.arogyasahaya.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.arogyasahaya.data.local.entity.Medicine

@Dao
interface MedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: Medicine): Long

    @Update
    suspend fun update(medicine: Medicine)

    @Query("SELECT * FROM medicine_table ORDER BY time ASC")
    fun getAllMedicines(): LiveData<List<Medicine>>

    @Query("SELECT * FROM medicine_table")
    suspend fun getAllMedicinesList(): List<Medicine>

    @Query("SELECT * FROM medicine_table WHERE id = :id")
    suspend fun getMedicineById(id: Int): Medicine?

    @Delete
    suspend fun delete(medicine: Medicine)

    @Query("UPDATE medicine_table SET isTaken = :isTaken, lastTakenDate = :date, remainingTablets = remainingTablets - 1 WHERE id = :id")
    suspend fun markAsTaken(id: Int, isTaken: Boolean, date: Long)

    @Query("UPDATE medicine_table SET isTaken = 0 WHERE frequency = 'Daily'")
    suspend fun resetDailyStatus()

    @Query("DELETE FROM medicine_table")
    suspend fun deleteAll()
}
