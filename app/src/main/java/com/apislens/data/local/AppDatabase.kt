package com.apislens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.apislens.data.local.dao.ChargeRecordDao
import com.apislens.data.local.dao.DeviceDao
import com.apislens.data.local.dao.DeviceNotificationSettingDao
import com.apislens.data.local.dao.UsageRecordDao
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.local.entity.DeviceNotificationSetting
import com.apislens.data.local.entity.UsageRecord

@Database(
    entities = [
        Device::class,
        ChargeRecord::class,
        UsageRecord::class,
        DeviceNotificationSetting::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun chargeRecordDao(): ChargeRecordDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun deviceNotificationSettingDao(): DeviceNotificationSettingDao

    companion object {
        const val DATABASE_NAME = "apislens.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE devices ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE devices ADD COLUMN lifecycle_months INTEGER NOT NULL DEFAULT 36")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS device_notification_settings (
                        deviceId INTEGER PRIMARY KEY NOT NULL,
                        reminderEnabled INTEGER,
                        firstThresholdDays INTEGER,
                        repeatIntervalDays INTEGER
                    )
                """.trimIndent())
            }
        }
    }
}
