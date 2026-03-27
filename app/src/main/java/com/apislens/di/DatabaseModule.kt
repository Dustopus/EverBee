package com.apislens.di

import android.content.Context
import androidx.room.Room
import com.apislens.data.local.AppDatabase
import com.apislens.data.local.dao.ChargeRecordDao
import com.apislens.data.local.dao.DeviceDao
import com.apislens.data.local.dao.UsageRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    @Provides
    fun provideDeviceDao(db: AppDatabase): DeviceDao = db.deviceDao()

    @Provides
    fun provideChargeRecordDao(db: AppDatabase): ChargeRecordDao = db.chargeRecordDao()

    @Provides
    fun provideUsageRecordDao(db: AppDatabase): UsageRecordDao = db.usageRecordDao()
}
