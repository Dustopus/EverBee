package com.apislens.data.model

import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.local.entity.UsageRecord

/**
 * JSON 导出/导入的根数据结构。
 *
 * 导出格式：
 * ```json
 * {
 *   "version": 1,
 *   "exportedAt": "2024-06-15T10:30:00Z",
 *   "devices": [...],
 *   "chargeRecords": [...],
 *   "usageRecords": [...]
 * }
 * ```
 */
data class ExportData(
    val version: Int = 1,
    val exportedAt: String,
    val devices: List<Device>,
    val chargeRecords: List<ChargeRecord>,
    val usageRecords: List<UsageRecord>
)
