package com.apislens.data.repository

import com.apislens.data.local.dao.UsageRecordDao
import com.apislens.data.local.entity.UsageRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRecordRepository @Inject constructor(
    private val usageRecordDao: UsageRecordDao
) {
    fun getRecordsByDevice(deviceId: Long): Flow<List<UsageRecord>> =
        usageRecordDao.getRecordsByDevice(deviceId)

    fun getRecordsByDeviceAsc(deviceId: Long): Flow<List<UsageRecord>> =
        usageRecordDao.getRecordsByDeviceAsc(deviceId)

    fun getRecordsByDeviceAndDateRange(
        deviceId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<UsageRecord>> =
        usageRecordDao.getRecordsByDeviceAndDateRange(deviceId, startDate, endDate)

    suspend fun getRecordByDeviceAndDate(deviceId: Long, date: String): UsageRecord? =
        usageRecordDao.getRecordByDeviceAndDate(deviceId, date)

    suspend fun getTotalUsageMinutesInRange(
        deviceId: Long,
        startDate: String,
        endDate: String
    ): Int? = usageRecordDao.getTotalUsageMinutesInRange(deviceId, startDate, endDate)

    suspend fun insert(record: UsageRecord): Long = usageRecordDao.insert(record)

    suspend fun insertAll(records: List<UsageRecord>) = usageRecordDao.insertAll(records)

    suspend fun update(record: UsageRecord) = usageRecordDao.update(record)

    suspend fun delete(record: UsageRecord) = usageRecordDao.delete(record)
}
