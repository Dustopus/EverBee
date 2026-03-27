package com.apislens.utils

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 成本计算器 — 封装设备折旧相关计算逻辑。
 */
object CostCalculator {

    /**
     * 计算电池健康度。
     *
     * 使用加权方法评估电池状态：
     * 1. 充满电的比例（能充到 100% 的次数占总次数）
     * 2. 充电效率（平均每次充入的电量百分比）
     * 3. 滑动窗口衰减检测（近期 vs 早期的充到最高值对比）
     *
     * 返回 0-100 的健康百分比。
     */
    fun calculateBatteryHealth(records: List<com.apislens.data.local.entity.ChargeRecord>): List<Pair<String, Double>> {
        if (records.isEmpty()) return emptyList()

        val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.CHINA)
        val results = mutableListOf<Pair<String, Double>>()

        // 累积计算
        var totalCycles = 0.0  // 累积完整充电周期数
        var fullChargeCount = 0  // 充到 >=95% 的次数
        var totalChargeGained = 0.0  // 累积充入电量
        val windowSize = 5  // 滑动窗口大小
        val maxLevels = java.util.ArrayDeque<Double>()  // 近期充到的最高电量

        for ((index, record) in records.withIndex()) {
            val endLvl = record.endLevel?.toDouble() ?: record.startLevel.toDouble()
            val chargeGained = endLvl - record.startLevel
            if (chargeGained > 0) {
                totalChargeGained += chargeGained
                totalCycles += chargeGained / 100.0
            }
            if (endLvl >= 95) fullChargeCount++

            maxLevels.addLast(endLvl)
            if (maxLevels.size > windowSize) maxLevels.removeFirst()

            // 健康度计算
            val fullChargeRatio = if (index + 1 > 0) fullChargeCount.toDouble() / (index + 1) else 1.0
            val recentMaxAvg = maxLevels.average()
            val healthScore = (fullChargeRatio * 40.0) + (recentMaxAvg * 0.6)
            val clampedHealth = healthScore.coerceIn(0.0, 100.0)

            results.add(sdf.format(java.util.Date(record.startTime)) to clampedHealth)
        }

        return results
    }

    /** 是否需要充电提醒 */
    fun needsChargeReminder(lastChargeTime: Long?): Boolean {
        if (lastChargeTime == null) return true
        val daysSinceCharge = (System.currentTimeMillis() - lastChargeTime) / 86400000.0
        return daysSinceCharge >= 7
    }

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
