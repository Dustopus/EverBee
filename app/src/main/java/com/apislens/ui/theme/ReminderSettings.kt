package com.apislens.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

@Singleton
class ReminderSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_FIRST_THRESHOLD_DAYS = intPreferencesKey("first_threshold_days")
        val KEY_REPEAT_INTERVAL_DAYS = intPreferencesKey("repeat_interval_days")
    }

    val reminderEnabled: Flow<Boolean> = context.reminderDataStore.data.map { prefs ->
        prefs[KEY_REMINDER_ENABLED] ?: true
    }

    val firstThresholdDays: Flow<Int> = context.reminderDataStore.data.map { prefs ->
        prefs[KEY_FIRST_THRESHOLD_DAYS] ?: 14
    }

    val repeatIntervalDays: Flow<Int> = context.reminderDataStore.data.map { prefs ->
        prefs[KEY_REPEAT_INTERVAL_DAYS] ?: 7
    }

    suspend fun isReminderEnabled(): Boolean {
        return context.reminderDataStore.data.map { prefs ->
            prefs[KEY_REMINDER_ENABLED] ?: true
        }.first()
    }

    suspend fun getFirstThresholdDays(): Long {
        return context.reminderDataStore.data.map { prefs ->
            prefs[KEY_FIRST_THRESHOLD_DAYS] ?: 14
        }.first().toLong()
    }

    suspend fun getRepeatIntervalDays(): Long {
        return context.reminderDataStore.data.map { prefs ->
            prefs[KEY_REPEAT_INTERVAL_DAYS] ?: 7
        }.first().toLong()
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setFirstThresholdDays(days: Int) {
        context.reminderDataStore.edit { it[KEY_FIRST_THRESHOLD_DAYS] = days }
    }

    suspend fun setRepeatIntervalDays(days: Int) {
        context.reminderDataStore.edit { it[KEY_REPEAT_INTERVAL_DAYS] = days }
    }

    private suspend fun <T> Flow<T>.first(): T {
        var result: T? = null
        collect { result = it; return@collect }
        return result!!
    }
}
