package com.apislens.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

@Singleton
class ThemeSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val dynamicColor: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }
}

@Composable
fun ThemeSettingsProvider(
    settings: ThemeSettings,
    content: @Composable (ThemeMode, Boolean) -> Unit
) {
    val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColor by settings.dynamicColor.collectAsState(initial = true)
    content(themeMode, dynamicColor)
}
