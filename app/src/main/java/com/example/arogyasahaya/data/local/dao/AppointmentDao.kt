package com.example.arogyasahaya.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.arogyasahaya.data.local.entity.Appointment

@Dao
interface AppointmentDao {
    @Insert
    suspend fun insert(appointment: Appointment): Long

    @Query("SELECT * FROM appointment_table ORDER BY dateTime ASC")
    fun getAllAppointments(): LiveData<List<Appointment>>

    @Query("SELECT * FROM appointment_table ORDER BY dateTime ASC")
    suspend fun getAllAppointmentsList(): List<Appointment>

    @Query("SELECT * FROM appointment_table WHERE dateTime >= :currentTime ORDER BY dateTime ASC")
    fun getUpcomingAppointments(currentTime: Long): LiveData<List<Appointment>>

    @Delete
    suspend fun delete(appointment: Appointment)

    @Query("DELETE FROM appointment_table")
    suspend fun deleteAll()
}
