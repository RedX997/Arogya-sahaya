package com.example.arogyasahaya.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.arogyasahaya.data.local.entity.FamilyMember

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY dateAdded DESC")
    fun getAllFamilyMembers(): LiveData<List<FamilyMember>>

    @Query("SELECT * FROM family_members ORDER BY dateAdded DESC")
    suspend fun getAllFamilyMembersList(): List<FamilyMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: FamilyMember)

    @Delete
    suspend fun delete(member: FamilyMember)

    @Query("DELETE FROM family_members")
    suspend fun deleteAll()
}
