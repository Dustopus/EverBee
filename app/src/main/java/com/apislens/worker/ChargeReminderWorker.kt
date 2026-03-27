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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 充电提醒 Worker — 定期检查设备充电状态。
 *
 * 如果某设备超过 [CHARGE_REMIND_DAYS] 天没有充电记录，
 * 则推送通知提醒用户。
 */
@HiltWorker
class ChargeReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val deviceDao: DeviceDao,
    private val chargeRecordDao: ChargeRecordDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val devices = deviceDao.getAllDevicesOnce()
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(CHARGE_REMIND_DAYS)

        val devicesNeedingCharge = mutableListOf<String>()

        for (device in devices) {
            val records = chargeRecordDao.getRecordsByDeviceOnce(device.id)
            if (records.isEmpty()) {
                // 从未充过电的设备，如果购买超过阈值天数则提醒
                try {
                    val purchaseMillis = java.time.LocalDate.parse(device.purchaseDate)
                        .toEpochDay() * 86400000L
                    if (System.currentTimeMillis() - purchaseMillis > TimeUnit.DAYS.toMillis(CHARGE_REMIND_DAYS)) {
                        devicesNeedingCharge.add(device.name)
                    }
                } catch (_: Exception) {}
            } else {
                // 检查最近一次充电是否超过阈值
                val lastCharge = records.maxOf { it.endTime ?: it.startTime }
                if (lastCharge < threshold) {
                    devicesNeedingCharge.add(device.name)
                }
            }
        }

        if (devicesNeedingCharge.isNotEmpty()) {
            showNotification(devicesNeedingCharge)
        }

        return Result.success()
    }

    private fun showNotification(deviceNames: List<String>) {
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
            "${deviceNames[0]} 已超过 $CHARGE_REMIND_DAYS 天未充电，记得充电哦 ⚡"
        } else {
            "以下设备已超过 $CHARGE_REMIND_DAYS 天未充电：${deviceNames.joinToString("、")} ⚡"
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
        const val CHARGE_REMIND_DAYS = 7L // 超过 7 天未充电则提醒
    }
}
