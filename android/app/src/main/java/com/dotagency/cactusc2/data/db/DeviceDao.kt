package com.dotagency.cactusc2.data.db

import androidx.room.*
import com.dotagency.cactusc2.data.model.Device
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the device registry.
 * Default devices sort first; the rest alphabetically by name.
 */
@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY isDefault DESC, name ASC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDevice(id: Long): Device?

    @Insert
    suspend fun insert(device: Device): Long

    @Update
    suspend fun update(device: Device)

    @Delete
    suspend fun delete(device: Device)

    @Query("UPDATE devices SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE devices SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}
