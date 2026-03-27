package com.apislens.utils

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 成本计算器 — 封装设备折旧相关计算逻辑。
 */
object CostCalculator {

    /**
     * 计算每日折旧成本。
     *
     * @param purchasePriceCents 购买价格（分）
     * @param purchaseDate 购买日期 (ISO-8601 "2024-06-15")
     * @return 每日成本（元），保留两位小数
     */
    fun calculateDailyCost(purchasePriceCents: Long, purchaseDate: String): Double {
        val price = purchasePriceCents / 100.0
        val days = daysSince(purchaseDate).coerceAtLeast(1)
        return price / days
    }

    /**
     * 计算从购买日期到今天的天数。
     */
    fun daysSince(purchaseDate: String): Long {
        return try {
            val start = LocalDate.parse(purchaseDate)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(start, today)
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 计算设备到目前为止的累计折旧金额。
     */
    fun totalDepreciation(purchasePriceCents: Long, purchaseDate: String): Double {
        val price = purchasePriceCents / 100.0
        val days = daysSince(purchaseDate).coerceAtLeast(1)
        // 线性折旧：假设设备生命周期 3 年（1095 天）
        val lifetime = 1095L
        val depreciationPerDay = price / lifetime
        return (depreciationPerDay * days).coerceAtMost(price)
    }
}
