package com.apislens.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.utils.ExportResult
import com.apislens.data.utils.ImportResult
import com.apislens.data.utils.ExportImportManager
import com.apislens.ui.theme.ReminderSettings
import com.apislens.ui.theme.ThemeMode
import com.apislens.ui.theme.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExportStatus {
    data object Idle : ExportStatus()
    data object Success : ExportStatus()
    data object Error : ExportStatus()
}

sealed class ImportStatus {
    data object Idle : ImportStatus()
    data class Success(val deviceCount: Int, val chargeRecordCount: Int, val usageRecordCount: Int) : ImportStatus()
    data object Error : ImportStatus()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImportManager: ExportImportManager,
    private val themeSettings: ThemeSettings,
    private val reminderSettings: ReminderSettings
) : ViewModel() {
    val themeMode = themeSettings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val dynamicColor = themeSettings.dynamicColor.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val reminderEnabled = reminderSettings.reminderEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val firstThresholdDays = reminderSettings.firstThresholdDays.stateIn(viewModelScope, SharingStarted.Eagerly, 14)
    val repeatIntervalDays = reminderSettings.repeatIntervalDays.stateIn(viewModelScope, SharingStarted.Eagerly, 7)

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus = _exportStatus.asStateFlow()

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus = _importStatus.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeSettings.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { themeSettings.setDynamicColor(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { reminderSettings.setReminderEnabled(enabled) }
    }

    fun setFirstThresholdDays(days: Int) {
        viewModelScope.launch { reminderSettings.setFirstThresholdDays(days) }
    }

    fun setRepeatIntervalDays(days: Int) {
        viewModelScope.launch { reminderSettings.setRepeatIntervalDays(days) }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val result = exportImportManager.exportTo(uri)
            _exportStatus.value = when (result) {
                is ExportResult.Success -> ExportStatus.Success
                is ExportResult.Error -> ExportStatus.Error
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val result = exportImportManager.importFrom(uri)
            _importStatus.value = when (result) {
                is ImportResult.Success -> ImportStatus.Success(result.deviceCount, result.chargeRecordCount, result.usageRecordCount)
                is ImportResult.Error -> ImportStatus.Error
            }
        }
    }

    fun clearExportStatus() { _exportStatus.value = ExportStatus.Idle }
    fun clearImportStatus() { _importStatus.value = ImportStatus.Idle }
}
