package com.apislens.data.utils

import android.content.Context
import android.net.Uri
import com.apislens.data.local.dao.ChargeRecordDao
import com.apislens.data.local.dao.DeviceDao
import com.apislens.data.local.dao.UsageRecordDao
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.local.entity.UsageRecord
import com.apislens.data.model.ExportData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceDao: DeviceDao,
    private val chargeRecordDao: ChargeRecordDao,
    private val usageRecordDao: UsageRecordDao,
    private val snowflakeIdGenerator: SnowflakeIdGenerator
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportTo(uri: Uri): ExportResult {
        val devices = deviceDao.getAllDevicesOnce()
        val chargeRecords = getAllChargeRecords()
        val usageRecords = getAllUsageRecords()

        val exportData = ExportData(
            exportedAt = Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            devices = devices,
            chargeRecords = chargeRecords,
            usageRecords = usageRecords
        )

        val json = gson.toJson(exportData)

        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(json.toByteArray(Charsets.UTF_8))
        } ?: return ExportResult.Error("无法打开输出流")

        return ExportResult.Success(
            deviceCount = devices.size,
            chargeRecordCount = chargeRecords.size,
            usageRecordCount = usageRecords.size
        )
    }

    suspend fun importFrom(uri: Uri): ImportResult {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins, Charsets.UTF_8)).readText()
            } ?: return ImportResult.Error("无法读取文件")

            val type = object : TypeToken<ExportData>() {}.type
            val data: ExportData = gson.fromJson(json, type)

            var deviceCount = 0
            var chargeCount = 0
            var usageCount = 0

            val idMapping = mutableMapOf<Long, Long>()

            for (device in data.devices) {
                val existingDevice = deviceDao.getDeviceOnce(device.id)
                if (existingDevice != null) {
                    idMapping[device.id] = existingDevice.id
                } else {
                    val newId = IdPrefix.generateDeviceId(snowflakeIdGenerator)
                    deviceDao.insert(device.copy(id = newId))
                    idMapping[device.id] = newId
                    deviceCount++
                }
            }

            for (record in data.chargeRecords) {
                val existingRecord = chargeRecordDao.getRecordById(record.id)
                if (existingRecord != null) {
                    continue
                }
                val newDeviceId = idMapping[record.deviceId] ?: record.deviceId
                val newId = IdPrefix.generateChargeId(snowflakeIdGenerator)
                chargeRecordDao.insert(record.copy(id = newId, deviceId = newDeviceId))
                chargeCount++
            }

            for (record in data.usageRecords) {
                val existingRecord = usageRecordDao.getRecordById(record.id)
                if (existingRecord != null) {
                    continue
                }
                val newDeviceId = idMapping[record.deviceId] ?: record.deviceId
                val newId = IdPrefix.generateUsageId(snowflakeIdGenerator)
                usageRecordDao.insert(record.copy(id = newId, deviceId = newDeviceId))
                usageCount++
            }

            ImportResult.Success(deviceCount, chargeCount, usageCount)
        } catch (e: Exception) {
            ImportResult.Error("导入失败: ${e.localizedMessage}")
        }
    }

    private suspend fun getAllChargeRecords(): List<ChargeRecord> {
        val devices = deviceDao.getAllDevicesOnce()
        return devices.flatMap { device ->
            chargeRecordDao.getRecordsByDeviceOnce(device.id)
        }
    }

    private suspend fun getAllUsageRecords(): List<UsageRecord> {
        val devices = deviceDao.getAllDevicesOnce()
        return devices.flatMap { device ->
            usageRecordDao.getRecordsByDeviceOnce(device.id)
        }
    }
}

sealed class ExportResult {
    data class Success(val deviceCount: Int, val chargeRecordCount: Int, val usageRecordCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(val deviceCount: Int, val chargeRecordCount: Int, val usageRecordCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
