package com.apislens.data.repository

import com.apislens.data.local.dao.DeviceDao
import com.apislens.data.local.entity.Device
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao
) {
    fun getAllDevices(): Flow<List<Device>> = deviceDao.getAllDevices()

    fun getDeviceById(id: Long): Flow<Device?> = deviceDao.getDeviceById(id)

    suspend fun getDeviceOnce(id: Long): Device? = deviceDao.getDeviceOnce(id)

    suspend fun insert(device: Device): Long = deviceDao.insert(device)

    suspend fun update(device: Device) = deviceDao.update(device)

    suspend fun delete(device: Device) = deviceDao.delete(device)

    suspend fun deleteById(id: Long) = deviceDao.deleteById(id)

    suspend fun getDeviceCount(): Int = deviceDao.getDeviceCount()

    suspend fun insertAll(devices: List<Device>): List<Long> {
        return devices.map { deviceDao.insert(it) }
    }
}
