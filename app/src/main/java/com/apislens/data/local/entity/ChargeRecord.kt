package com.apislens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * 充电记录实体 — 记录单次充电事件。
 *
 * 关联到具体设备，用于：
 * 1. 电池健康趋势推算（对比历次充放电效率）
 * 2. 充电习惯时间分布分析
 * 3. 充电周期数统计
 */
@Entity(
    tableName = "charge_records",
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
        Index(value = ["start_time"])
    ]
)
data class ChargeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 关联设备 ID */
    @ColumnInfo(name = "device_id")
    val deviceId: Long,

    /** 充电开始时间戳（毫秒） */
    @ColumnInfo(name = "start_time")
    val startTime: Long,

    /** 充电结束时间戳（毫秒），null 表示仍在充电中 */
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    /** 充电开始时电量百分比（0-100） */
    @ColumnInfo(name = "start_level")
    val startLevel: Int,

    /** 充电结束时电量百分比（0-100） */
    @ColumnInfo(name = "end_level")
    val endLevel: Int? = null,

    /** 充电时长（分钟），方便查询，不必每次重新计算 */
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int? = null,

    /** 充电备注（如 "快充 / 慢充 / 无线充"） */
    @ColumnInfo(name = "note")
    val note: String = "",

    /** 记录创建时间戳 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 本次充电净增电量百分比 */
    val chargeGained: Int?
        get() = if (endLevel != null) endLevel - startLevel else null
}
