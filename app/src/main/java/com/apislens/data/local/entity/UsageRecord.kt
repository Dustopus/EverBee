package com.apislens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * 使用记录实体 — 记录设备每日使用时长。
 *
 * 一条记录对应一台设备在某一天的总使用时长。
 * 用于：
 * 1. 每日/每周使用时间分布图
 * 2. 设备使用强度评估
 * 3. 成本效率分析（使用时长 vs 折旧成本）
 */
@Entity(
    tableName = "usage_records",
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["device_id"]),
        Index(value = ["device_id", "date"], unique = true) // 每台设备每天一条记录
    ]
)
data class UsageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 关联设备 ID */
    @ColumnInfo(name = "device_id")
    val deviceId: Long,

    /** 日期，ISO-8601 格式 "2024-06-15" */
    @ColumnInfo(name = "date")
    val date: String,

    /** 当日总使用时长（分钟） */
    @ColumnInfo(name = "usage_minutes")
    val usageMinutes: Int,

    /** 使用备注 */
    @ColumnInfo(name = "note")
    val note: String = "",

    /** 记录创建时间戳 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** 记录最后更新时间戳 */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 使用时长（小时），保留一位小数 */
    val usageHours: Double
        get() = usageMinutes / 60.0
}
