package com.apislens.data.repository

import com.apislens.data.local.dao.ChargeRecordDao
import com.apislens.data.local.entity.ChargeRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargeRecordRepository @Inject constructor(
    private val chargeRecordDao: ChargeRecordDao
) {
    fun getRecordsByDevice(deviceId: Long): Flow<List<ChargeRecord>> =
        chargeRecordDao.getRecordsByDevice(deviceId)

    fun getRecordsByDeviceAsc(deviceId: Long): Flow<List<ChargeRecord>> =
        chargeRecordDao.getRecordsByDeviceAsc(deviceId)

    fun getRecordsByDeviceAndDateRange(
        deviceId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<List<ChargeRecord>> =
        chargeRecordDao.getRecordsByDeviceAndDateRange(deviceId, startTime, endTime)

    suspend fun insert(record: ChargeRecord): Long = chargeRecordDao.insert(record)

    suspend fun insertAll(records: List<ChargeRecord>) = chargeRecordDao.insertAll(records)

    suspend fun update(record: ChargeRecord) = chargeRecordDao.update(record)

    suspend fun delete(record: ChargeRecord) = chargeRecordDao.delete(record)

    suspend fun getChargeCountByDevice(deviceId: Long): Int =
        chargeRecordDao.getChargeCountByDevice(deviceId)

    suspend fun getLatestChargeRecord(): ChargeRecord? =
        chargeRecordDao.getLatestChargeRecord()

    suspend fun getLongestUnchargedDeviceId(): Long? =
        chargeRecordDao.getLongestUnchargedDeviceId()

    fun getAllChargeRecords(): Flow<List<ChargeRecord>> =
        chargeRecordDao.getAllChargeRecords()

    suspend fun getLatestChargeTimeByDevice(deviceId: Long): Long? =
        chargeRecordDao.getLatestChargeTimeByDevice(deviceId)

    suspend fun getLatestValidChargeTimeByDevice(deviceId: Long): Long? =
        chargeRecordDao.getLatestValidChargeTimeByDevice(deviceId)
}
