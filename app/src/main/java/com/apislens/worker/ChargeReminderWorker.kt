package com.apislens.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apislens.R
import com.apislens.data.local.dao.ChargeRecordDao
import com.apislens.data.local.dao.DeviceDao
import com.apislens.ui.theme.ReminderSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ChargeReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val deviceDao: DeviceDao,
    private val chargeRecordDao: ChargeRecordDao,
    private val reminderSettings: ReminderSettings
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = reminderSettings.isReminderEnabled()
        if (!enabled) return Result.success()

        val firstThresholdDays = reminderSettings.getFirstThresholdDays()
        val repeatIntervalDays = reminderSettings.getRepeatIntervalDays()

        val devices = deviceDao.getAllDevicesOnce()
        val now = System.currentTimeMillis()

        val devicesNeedingCharge = mutableListOf<String>()

        for (device in devices) {
            val records = chargeRecordDao.getRecordsByDeviceOnce(device.id)
            val lastChargeTime: Long? = if (records.isEmpty()) {
                try {
                    java.time.LocalDate.parse(device.purchaseDate)
                        .toEpochDay() * 86400000L
                } catch (_: Exception) { null }
            } else {
                records.maxOf { it.endTime ?: it.startTime }
            }

            if (lastChargeTime == null) continue

            val daysSinceLastCharge = (now - lastChargeTime) / 86_400_000L

            if (daysSinceLastCharge >= firstThresholdDays) {
                val daysAfterFirst = daysSinceLastCharge - firstThresholdDays
                if (daysAfterFirst == 0L || daysAfterFirst % repeatIntervalDays == 0L) {
                    devicesNeedingCharge.add(device.name)
                }
            }
        }

        if (devicesNeedingCharge.isNotEmpty()) {
            showNotification(devicesNeedingCharge, firstThresholdDays)
        }

        return Result.success()
    }

    private fun showNotification(deviceNames: List<String>, thresholdDays: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "充电提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "设备长时间未充电时提醒"
            }
            manager.createNotificationChannel(channel)
        }

        val text = if (deviceNames.size == 1) {
            "${deviceNames[0]} 已超过 $thresholdDays 天未充电，记得充电哦 ⚡"
        } else {
            "以下设备已超过 $thresholdDays 天未充电：${deviceNames.joinToString("、")} ⚡"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("🐝 ApisLens 充电提醒")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "charge_reminder"
        const val CHANNEL_ID = "charge_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }
}
