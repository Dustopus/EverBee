package com.apislens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apislens.data.local.entity.DeviceNotificationSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceNotificationSettingDao {

    @Query("SELECT * FROM device_notification_settings WHERE deviceId = :deviceId")
    suspend fun getByDeviceId(deviceId: Long): DeviceNotificationSetting?

    @Query("SELECT * FROM device_notification_settings WHERE deviceId = :deviceId")
    fun observeByDeviceId(deviceId: Long): Flow<DeviceNotificationSetting?>

    @Query("SELECT * FROM device_notification_settings")
    fun observeAll(): Flow<List<DeviceNotificationSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: DeviceNotificationSetting)

    @Update
    suspend fun update(setting: DeviceNotificationSetting)

    @Query("DELETE FROM device_notification_settings WHERE deviceId = :deviceId")
    suspend fun deleteByDeviceId(deviceId: Long)

    @Query("DELETE FROM device_notification_settings")
    suspend fun deleteAll()
}
