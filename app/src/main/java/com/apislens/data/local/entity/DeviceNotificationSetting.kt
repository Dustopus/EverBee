package com.apislens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_notification_settings")
data class DeviceNotificationSetting(
    @PrimaryKey
    val deviceId: Long,

    val reminderEnabled: Boolean? = null,

    val firstThresholdDays: Int? = null,

    val repeatIntervalDays: Int? = null
) {
    fun isUsingGlobalSettings(): Boolean =
        reminderEnabled == null && firstThresholdDays == null && repeatIntervalDays == null
}
