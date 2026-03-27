package com.apislens.data.local.dao

import androidx.room.*
import com.apislens.data.local.entity.UsageRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {

    @Query("SELECT * FROM usage_records WHERE device_id = :deviceId ORDER BY date DESC")
    fun getRecordsByDevice(deviceId: Long): Flow<List<UsageRecord>>

    @Query("SELECT * FROM usage_records WHERE device_id = :deviceId ORDER BY date ASC")
    fun getRecordsByDeviceAsc(deviceId: Long): Flow<List<UsageRecord>>

    @Query("SELECT * FROM usage_records WHERE id = :id")
    suspend fun getRecordById(id: Long): UsageRecord?

    @Query("SELECT * FROM usage_records WHERE device_id = :deviceId AND date = :date LIMIT 1")
    suspend fun getRecordByDeviceAndDate(deviceId: Long, date: String): UsageRecord?

    @Query("""
        SELECT * FROM usage_records 
        WHERE device_id = :deviceId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date ASC
    """)
    fun getRecordsByDeviceAndDateRange(
        deviceId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<UsageRecord>>

    @Query("""
        SELECT SUM(usage_minutes) FROM usage_records 
        WHERE device_id = :deviceId 
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getTotalUsageMinutesInRange(
        deviceId: Long,
        startDate: String,
        endDate: String
    ): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: UsageRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<UsageRecord>)

    @Update
    suspend fun update(record: UsageRecord)

    @Delete
    suspend fun delete(record: UsageRecord)

    @Query("DELETE FROM usage_records WHERE device_id = :deviceId")
    suspend fun deleteByDevice(deviceId: Long)

    @Query("SELECT * FROM usage_records WHERE device_id = :deviceId")
    suspend fun getRecordsByDeviceOnce(deviceId: Long): List<UsageRecord>
}
