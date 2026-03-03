package com.dotagency.cactusc2.data.db

import androidx.room.*
import com.dotagency.cactusc2.data.model.Payload
import kotlinx.coroutines.flow.Flow

/** Room DAO for locally-stored DuckyScript payloads. Most-recently-updated first. */
@Dao
interface PayloadDao {
    @Query("SELECT * FROM payloads ORDER BY updatedAt DESC")
    fun getAllPayloads(): Flow<List<Payload>>

    @Query("SELECT * FROM payloads WHERE id = :id")
    suspend fun getPayload(id: Long): Payload?

    @Insert
    suspend fun insert(payload: Payload): Long

    @Update
    suspend fun update(payload: Payload)

    @Delete
    suspend fun delete(payload: Payload)
}
