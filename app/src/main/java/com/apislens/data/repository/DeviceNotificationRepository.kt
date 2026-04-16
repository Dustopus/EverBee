package com.apislens.data.repository

import com.apislens.data.local.dao.DeviceDao
import com.apislens.data.local.dao.DeviceNotificationSettingDao
import com.apislens.data.local.entity.Device
import com.apislens.data.local.entity.DeviceNotificationSetting
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceNotificationRepository @Inject constructor(
    private val deviceNotificationSettingDao: DeviceNotificationSettingDao,
    private val deviceDao: DeviceDao
) {
    fun getAllDevices(): Flow<List<Device>> = deviceDao.getAllDevices()

    fun getDeviceSetting(deviceId: Long): Flow<DeviceNotificationSetting?> =
        deviceNotificationSettingDao.observeByDeviceId(deviceId)

    fun getAllDeviceSettings(): Flow<List<DeviceNotificationSetting>> =
        deviceNotificationSettingDao.observeAll()

    suspend fun getDeviceSettingSync(deviceId: Long): DeviceNotificationSetting? =
        deviceNotificationSettingDao.getByDeviceId(deviceId)

    suspend fun saveDeviceSetting(setting: DeviceNotificationSetting) {
        deviceNotificationSettingDao.insert(setting)
    }

    suspend fun deleteDeviceSetting(deviceId: Long) {
        deviceNotificationSettingDao.deleteByDeviceId(deviceId)
    }
}
