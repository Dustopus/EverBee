package com.apislens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "model")
    val model: String = "",

    @ColumnInfo(name = "icon_uri")
    val iconUri: String? = null,

    @ColumnInfo(name = "category", defaultValue = "")
    val category: String = "",

    @ColumnInfo(name = "purchase_date")
    val purchaseDate: String,

    @ColumnInfo(name = "purchase_price_cents")
    val purchasePriceCents: Long,

    @ColumnInfo(name = "lifecycle_months", defaultValue = "36")
    val lifecycleMonths: Int = DEFAULT_LIFECYCLE_MONTHS,

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val purchasePrice: Double
        get() = purchasePriceCents / 100.0

    val lifecycleDays: Long
        get() = lifecycleMonths.toLong() * 30

    companion object {
        const val DEFAULT_LIFECYCLE_MONTHS = 36

        val CATEGORIES = listOf(
            "手机", "平板", "笔记本", "智能手表", "耳机",
            "充电宝", "电池", "游戏机", "相机", "音箱", "其他"
        )
    }
}
