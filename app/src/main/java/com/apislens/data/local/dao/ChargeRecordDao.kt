package com.apislens.data.local.dao

import androidx.room.*
import com.apislens.data.local.entity.ChargeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargeRecordDao {

    @Query("SELECT * FROM charge_records WHERE device_id = :deviceId ORDER BY start_time DESC")
    fun getRecordsByDevice(deviceId: Long): Flow<List<ChargeRecord>>

    @Query("SELECT * FROM charge_records WHERE device_id = :deviceId ORDER BY start_time ASC")
    fun getRecordsByDeviceAsc(deviceId: Long): Flow<List<ChargeRecord>>

    @Query("SELECT * FROM charge_records WHERE id = :id")
    suspend fun getRecordById(id: Long): ChargeRecord?

    @Query("""
        SELECT * FROM charge_records 
        WHERE device_id = :deviceId 
        AND start_time >= :startTime 
        AND start_time <= :endTime 
        ORDER BY start_time ASC
    """)
    fun getRecordsByDeviceAndDateRange(
        deviceId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<List<ChargeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ChargeRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ChargeRecord>)

    @Update
    suspend fun update(record: ChargeRecord)

    @Delete
    suspend fun delete(record: ChargeRecord)

    @Query("DELETE FROM charge_records WHERE device_id = :deviceId")
    suspend fun deleteByDevice(deviceId: Long)

    @Query("SELECT COUNT(*) FROM charge_records WHERE device_id = :deviceId")
    suspend fun getChargeCountByDevice(deviceId: Long): Int

    @Query("SELECT * FROM charge_records WHERE device_id = :deviceId")
    suspend fun getRecordsByDeviceOnce(deviceId: Long): List<ChargeRecord>

    @Query("SELECT * FROM charge_records ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestChargeRecord(): ChargeRecord?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT device_id FROM charge_records GROUP BY device_id ORDER BY MAX(start_time) ASC LIMIT 1")
    suspend fun getLongestUnchargedDeviceId(): Long?

    @Query("SELECT * FROM charge_records ORDER BY start_time DESC")
    fun getAllChargeRecords(): Flow<List<ChargeRecord>>

    @Query("SELECT start_time FROM charge_records WHERE device_id = :deviceId ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestChargeTimeByDevice(deviceId: Long): Long?

    @Query("SELECT start_time FROM charge_records WHERE device_id = :deviceId AND end_level IS NOT NULL AND (end_level - start_level) >= 20 ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestValidChargeTimeByDevice(deviceId: Long): Long?
}
