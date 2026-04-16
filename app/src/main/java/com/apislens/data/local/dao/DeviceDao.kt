package com.apislens.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.apislens.data.local.entity.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY updated_at DESC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id")
    fun getDeviceById(id: Long): Flow<Device?>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDeviceOnce(id: Long): Device?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(device: Device): Long

    @Update
    suspend fun update(device: Device)

    @Delete
    suspend fun delete(device: Device)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getDeviceCount(): Int

    @Query("SELECT * FROM devices")
    suspend fun getAllDevicesOnce(): List<Device>
}
