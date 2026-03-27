package com.apislens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.apislens.data.local.dao.ChargeRecordDao
import com.apislens.data.local.dao.DeviceDao
import com.apislens.data.local.dao.UsageRecordDao
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.local.entity.UsageRecord

/**
 * Room 数据库定义。
 *
 * 包含三张核心表：
 * - [Device]         设备信息
 * - [ChargeRecord]   充电记录
 * - [UsageRecord]    使用记录
 *
 * 版本迁移策略：每次 schema 变更递增 version，
 * 并提供 Migration 对象。
 */
@Database(
    entities = [
        Device::class,
        ChargeRecord::class,
        UsageRecord::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun chargeRecordDao(): ChargeRecordDao
    abstract fun usageRecordDao(): UsageRecordDao

    companion object {
        const val DATABASE_NAME = "apislens.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE devices ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
