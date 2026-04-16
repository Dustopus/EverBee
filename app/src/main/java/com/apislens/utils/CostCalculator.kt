package com.apislens.utils

import com.apislens.data.local.entity.ChargeRecord
import java.text.SimpleDateFormat
import java.util.*

data class BatteryHealthResult(
    val healthScore: Double?,
    val confidence: String,
    val referenceSegment: String?,
    val historyData: List<Pair<String, Double>>
) {
    companion object {
        const val CONFIDENCE_HIGH = "HIGH"
        const val CONFIDENCE_LOW = "LOW"
        const val CONFIDENCE_INITIAL = "INITIAL"
        const val CONFIDENCE_INSUFFICIENT = "INSUFFICIENT_DATA"
    }
}

object CostCalculator {

    private const val BASE_RATE = 0.55

    fun calculateBatteryHealthV3(
        records: List<ChargeRecord>,
        lastHealthScore: Double? = null,
        lastTimestamp: Long? = null
    ): BatteryHealthResult {
        if (records.isEmpty()) {
            return BatteryHealthResult(
                healthScore = null,
                confidence = BatteryHealthResult.CONFIDENCE_INSUFFICIENT,
                referenceSegment = null,
                historyData = emptyList()
            )
        }

        val validRecords = records.filter { record ->
            val endLvl = record.endLevel ?: return@filter false
            val delta = endLvl - record.startLevel
            val duration = record.durationMinutes ?: return@filter false
            delta >= 5 && duration >= 2
        }

        if (validRecords.isEmpty()) {
            return BatteryHealthResult(
                healthScore = lastHealthScore,
                confidence = if (lastHealthScore != null) BatteryHealthResult.CONFIDENCE_LOW else BatteryHealthResult.CONFIDENCE_INSUFFICIENT,
                referenceSegment = null,
                historyData = emptyList()
            )
        }

        var bestRecord: ChargeRecord? = null
        var maxScore = 0.0

        for (record in validRecords) {
            val endLvl = record.endLevel ?: continue
            val startLvl = record.startLevel.toDouble()
            val endScore = endLvl.toDouble()
            val spanScore = (endLvl - startLvl)
            val durScore = calculateDurationScore(record.durationMinutes ?: 0, spanScore)
            val totalScore = endScore * 0.5 + spanScore * 0.4 + durScore * 0.1

            if (totalScore > maxScore) {
                maxScore = totalScore
                bestRecord = record
            }
        }

        val best = bestRecord!!
        val confidence = if ((best.endLevel ?: 0) >= 80) {
            BatteryHealthResult.CONFIDENCE_HIGH
        } else {
            BatteryHealthResult.CONFIDENCE_LOW
        }

        val startSoc = best.startLevel.toDouble()
        val endSoc = (best.endLevel ?: best.startLevel).toDouble()
        val deltaSoc = endSoc - startSoc
        val duration = (best.durationMinutes ?: 0).toDouble()

        if (deltaSoc == 0.0 || duration == 0.0) {
            return BatteryHealthResult(
                healthScore = lastHealthScore,
                confidence = BatteryHealthResult.CONFIDENCE_LOW,
                referenceSegment = null,
                historyData = emptyList()
            )
        }

        val rate = duration / deltaSoc
        val zoneAdjustment = getZoneAdjustment(startSoc, endSoc)
        val adjustedRate = rate * zoneAdjustment
        val rawSoH = (BASE_RATE / adjustedRate) * 100.0
        val clampedSoH = rawSoH.coerceIn(50.0, 105.0)

        var finalConfidence = confidence
        val dailyAvgCharge = calculateDailyAverage(validRecords)
        if (dailyAvgCharge > 3 && finalConfidence == BatteryHealthResult.CONFIDENCE_HIGH) {
            finalConfidence = BatteryHealthResult.CONFIDENCE_LOW
        }

        val smoothFactor = 0.2
        val currentTime = System.currentTimeMillis()
        val finalSoH: Double

        if (lastHealthScore != null && lastTimestamp != null) {
            val daysSinceLast = (currentTime - lastTimestamp) / (1000 * 60 * 60 * 24)
            if (daysSinceLast <= 7) {
                finalSoH = lastHealthScore * (1 - smoothFactor) + clampedSoH * smoothFactor
            } else {
                finalSoH = clampedSoH
                if (finalConfidence == BatteryHealthResult.CONFIDENCE_HIGH) {
                    finalConfidence = BatteryHealthResult.CONFIDENCE_LOW
                }
            }
        } else {
            finalSoH = clampedSoH
            if (finalConfidence == BatteryHealthResult.CONFIDENCE_HIGH) {
                finalConfidence = BatteryHealthResult.CONFIDENCE_INITIAL
            }
        }

        val roundedSoH = (finalSoH * 10).toInt() / 10.0
        val referenceSegment = "${startSoc.toInt()}% → ${endSoc.toInt()}% (${duration.toInt()}min)"

        val historyData = buildHistoryData(records, roundedSoH)

        return BatteryHealthResult(
            healthScore = roundedSoH,
            confidence = finalConfidence,
            referenceSegment = referenceSegment,
            historyData = historyData
        )
    }

    private fun calculateDurationScore(durationMinutes: Int, deltaSoc: Double): Double {
        if (durationMinutes <= 0 || deltaSoc <= 0) return 0.0
        val rate = durationMinutes / deltaSoc
        return when {
            rate < 0.3 -> 20.0
            rate < 0.6 -> 50.0
            rate < 1.2 -> 80.0
            rate < 2.5 -> 100.0
            rate < 5.0 -> 85.0
            else -> 60.0
        }
    }

    private fun getZoneAdjustment(startSoc: Double, endSoc: Double): Double {
        fun adjust(soc: Double): Double {
            return when {
                soc < 20 -> 0.85
                soc > 85 -> 1.35
                else -> 1.0
            }
        }
        return (adjust(startSoc) + adjust(endSoc)) / 2.0
    }

    private fun calculateDailyAverage(records: List<ChargeRecord>): Double {
        if (records.size < 2) return 0.0
        val timestamps = records.map { it.startTime }
        val minTime = timestamps.minOrNull() ?: return 0.0
        val maxTime = timestamps.maxOrNull() ?: return 0.0
        val daysDiff = ((maxTime - minTime) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        return records.size.toDouble() / daysDiff
    }

    private fun buildHistoryData(records: List<ChargeRecord>, currentScore: Double): List<Pair<String, Double>> {
        if (records.size < 2) return emptyList()

        val sdf = SimpleDateFormat("MM/dd", Locale.CHINA)
        val results = mutableListOf<Pair<String, Double>>()
        val windowSize = minOf(5, records.size)
        val recentScores = mutableListOf<Double>()

        for ((index, record) in records.withIndex()) {
            val endLvl = (record.endLevel ?: record.startLevel).toDouble()
            val chargeGained = endLvl - record.startLevel

            if (chargeGained > 5) {
                recentScores.add(endLvl)
                if (recentScores.size > windowSize) {
                    recentScores.removeAt(0)
                }
            }

            val score = when {
                index < records.size - 1 -> {
                    if (recentScores.isNotEmpty()) {
                        recentScores.average().coerceIn(50.0, 100.0)
                    } else {
                        85.0
                    }
                }
                else -> currentScore
            }

            results.add(sdf.format(record.startTime) to score)
        }

        return results
    }

    @Deprecated("Use calculateBatteryHealthV3 instead")
    fun calculateBatteryHealthV2(
        records: List<ChargeRecord>,
        lastHealthScore: Double? = null,
        lastTimestamp: Long? = null
    ): BatteryHealthResult = calculateBatteryHealthV3(records, lastHealthScore, lastTimestamp)

    @Deprecated("Use calculateBatteryHealthV3 instead")
    fun calculateBatteryHealth(records: List<ChargeRecord>): List<Pair<String, Double>> {
        return calculateBatteryHealthV3(records).historyData
    }

    fun needsChargeReminder(lastChargeTime: Long?): Boolean {
        if (lastChargeTime == null) return true
        val daysSinceCharge = (System.currentTimeMillis() - lastChargeTime) / 86400000.0
        return daysSinceCharge >= 7
    }

    fun calculateDailyCost(purchasePriceCents: Long, purchaseDate: String): Double {
        val price = purchasePriceCents / 100.0
        val days = daysSince(purchaseDate).coerceAtLeast(1)
        return price / days
    }

    fun daysSince(purchaseDate: String): Long {
        return try {
            val cal = Calendar.getInstance()
            val parts = purchaseDate.split("-")
            if (parts.size == 3) {
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                val today = Calendar.getInstance()
                val diffMillis = today.timeInMillis - cal.timeInMillis
                diffMillis / (1000 * 60 * 60 * 24)
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun totalDepreciation(purchasePriceCents: Long, purchaseDate: String, lifecycleDays: Long = 1095L): Double {
        val price = purchasePriceCents / 100.0
        val days = daysSince(purchaseDate).coerceAtLeast(1)
        val lifetime = lifecycleDays.coerceAtLeast(1)
        val depreciationPerDay = price / lifetime
        return (depreciationPerDay * days).coerceAtMost(price)
    }
}
