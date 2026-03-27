package com.apislens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * 设备实体 — 代表用户管理的一台电子设备。
 *
 * 每日折旧成本 = purchasePrice / max(1, 已使用天数)
 * 在 ViewModel 层通过 [calculateDailyCost] 统一计算，
 * UI 层读取即可，避免在数据库层做业务逻辑。
 */
@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 设备名称，如 "iPhone 15 Pro" */
    @ColumnInfo(name = "name")
    val name: String,

    /** 设备型号，如 "A2849" */
    @ColumnInfo(name = "model")
    val model: String = "",

    /** 设备图标 URI（可为本地 content:// 或相对路径） */
    @ColumnInfo(name = "icon_uri")
    val iconUri: String? = null,

    /** 设备分类，如 "手机", "平板", "充电宝", "电池", "笔记本" 等 */
    @ColumnInfo(name = "category", defaultValue = "")
    val category: String = "",

    /** 购买日期，ISO-8601 字符串 "2024-06-15" */
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: String,

    /** 购买价格（分/cent 为单位，避免浮点精度问题） */
    @ColumnInfo(name = "purchase_price_cents")
    val purchasePriceCents: Long,

    /** 设备备注 */
    @ColumnInfo(name = "note")
    val note: String = "",

    /** 记录创建时间戳（毫秒） */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** 记录最后更新时间戳（毫秒） */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 购买价格（元） */
    val purchasePrice: Double
        get() = purchasePriceCents / 100.0

    companion object {
        /** 预设设备分类 */
        val CATEGORIES = listOf(
            "手机", "平板", "笔记本", "智能手表", "耳机",
            "充电宝", "电池", "游戏机", "相机", "音箱", "其他"
        )
    }
}
