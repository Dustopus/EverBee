package com.apislens.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.utils.ExportImportManager
import com.apislens.ui.theme.ThemeMode
import com.apislens.ui.theme.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImportManager: ExportImportManager,
    private val themeSettings: ThemeSettings
) : ViewModel() {
    val themeMode = themeSettings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val dynamicColor = themeSettings.dynamicColor.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeSettings.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { themeSettings.setDynamicColor(enabled) }
    }

    fun exportData(uri: Uri) { viewModelScope.launch { exportImportManager.exportTo(uri) } }
    fun importData(uri: Uri) { viewModelScope.launch { exportImportManager.importFrom(uri) } }
}
