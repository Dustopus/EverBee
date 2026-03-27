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

/**
 * JSON 数据导入/导出处理器。
 *
 * 使用 Android 存储访问框架 (SAF) 进行文件读写，
 * 调用方通过 onActivityResult 拿到 Uri 后传入。
 */
@Singleton
class ExportImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceDao: DeviceDao,
    private val chargeRecordDao: ChargeRecordDao,
    private val usageRecordDao: UsageRecordDao
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 将全部数据导出到指定 Uri。
     * @return 导出的设备数和记录数
     */
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

    /**
     * 从指定 Uri 导入数据（合并策略：新增为主，跳过冲突）。
     * @return 导入的设备数和记录数
     */
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

            // 导入设备（智能去重：按名称+型号+购买日期匹配，匹配到则跳过）
            val idMapping = mutableMapOf<Long, Long>()
            val existingDevices = deviceDao.getAllDevicesOnce()

            for (device in data.devices) {
                val oldId = device.id
                // 检查是否已存在相同设备
                val duplicate = existingDevices.find { existing ->
                    existing.name == device.name
                        && existing.purchaseDate == device.purchaseDate
                        && (existing.model == device.model || device.model.isEmpty())
                }
                if (duplicate != null) {
                    // 已存在，复用已有 ID
                    idMapping[oldId] = duplicate.id
                } else {
                    // 新设备，插入
                    val newId = deviceDao.insert(device.copy(id = 0))
                    idMapping[oldId] = newId
                    deviceCount++
                }
            }

            // 导入充电记录（更新 deviceId 映射）
            for (record in data.chargeRecords) {
                val newDeviceId = idMapping[record.deviceId] ?: record.deviceId
                chargeRecordDao.insert(record.copy(id = 0, deviceId = newDeviceId))
                chargeCount++
            }

            // 导入使用记录
            for (record in data.usageRecords) {
                val newDeviceId = idMapping[record.deviceId] ?: record.deviceId
                usageRecordDao.insert(record.copy(id = 0, deviceId = newDeviceId))
                usageCount++
            }

            ImportResult.Success(deviceCount, chargeCount, usageCount)
        } catch (e: Exception) {
            ImportResult.Error("导入失败: ${e.localizedMessage}")
        }
    }

    private suspend fun getAllChargeRecords(): List<ChargeRecord> {
        // Room Flow 不能在 suspend 中直接 collect，用简单查询
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
