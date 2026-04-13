package com.apislens.utils

import java.text.SimpleDateFormat
import java.util.*

object RelativeTimeFormatter {

    private const val HOUR_MS = 3600000L
    private const val DAY_MS = 86400000L
    private const val WEEK_MS = 604800000L
    private const val MONTH_MS = 2592000000L

    fun format(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (diff < 0) return "刚刚"

        return when {
            diff < HOUR_MS -> {
                val hours = (diff / HOUR_MS).toInt().coerceAtLeast(1)
                "${hours}小时前"
            }
            diff < DAY_MS -> {
                val hours = (diff / HOUR_MS).toInt()
                "${hours}小时前"
            }
            diff < WEEK_MS -> {
                val days = (diff / DAY_MS).toInt()
                "${days}天前"
            }
            diff < MONTH_MS -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            else -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
