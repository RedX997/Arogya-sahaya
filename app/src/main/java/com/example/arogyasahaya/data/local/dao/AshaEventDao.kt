package com.example.arogyasahaya.data.local.dao

import androidx.room.*
import com.example.arogyasahaya.data.local.entity.AshaEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AshaEventDao {
    @Query("SELECT * FROM asha_events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<AshaEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AshaEvent)

    @Delete
    suspend fun deleteEvent(event: AshaEvent)

    @Query("DELETE FROM asha_events")
    suspend fun deleteAll()
}
