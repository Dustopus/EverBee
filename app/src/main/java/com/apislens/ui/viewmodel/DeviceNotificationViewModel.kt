package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.Device
import com.apislens.data.local.entity.DeviceNotificationSetting
import com.apislens.data.repository.DeviceNotificationRepository
import com.apislens.ui.theme.ReminderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceNotificationState(
    val device: Device,
    val setting: DeviceNotificationSetting?,
    val effectiveReminderEnabled: Boolean,
    val effectiveFirstThresholdDays: Int,
    val effectiveRepeatIntervalDays: Int
)

@HiltViewModel
class DeviceNotificationViewModel @Inject constructor(
    private val repository: DeviceNotificationRepository,
    private val reminderSettings: ReminderSettings
) : ViewModel() {

    private val _globalReminderEnabled = MutableStateFlow(true)
    private val _globalFirstThresholdDays = MutableStateFlow(14)
    private val _globalRepeatIntervalDays = MutableStateFlow(7)

    val globalReminderEnabled: StateFlow<Boolean> = _globalReminderEnabled.asStateFlow()
    val globalFirstThresholdDays: StateFlow<Int> = _globalFirstThresholdDays.asStateFlow()
    val globalRepeatIntervalDays: StateFlow<Int> = _globalRepeatIntervalDays.asStateFlow()

    val devicesWithSettings: StateFlow<List<DeviceNotificationState>> =
        combine(
            repository.getAllDevices(),
            repository.getAllDeviceSettings(),
            _globalReminderEnabled,
            _globalFirstThresholdDays,
            _globalRepeatIntervalDays
        ) { devices, settings, globalEnabled, globalThreshold, globalInterval ->
            settings.associateBy { it.deviceId }
            devices.map { device ->
                val setting = settings.find { it.deviceId == device.id }
                DeviceNotificationState(
                    device = device,
                    setting = setting,
                    effectiveReminderEnabled = setting?.reminderEnabled ?: globalEnabled,
                    effectiveFirstThresholdDays = setting?.firstThresholdDays ?: globalThreshold,
                    effectiveRepeatIntervalDays = setting?.repeatIntervalDays ?: globalInterval
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            reminderSettings.reminderEnabled.collect { enabled ->
                _globalReminderEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            reminderSettings.firstThresholdDays.collect { days ->
                _globalFirstThresholdDays.value = days
            }
        }
        viewModelScope.launch {
            reminderSettings.repeatIntervalDays.collect { days ->
                _globalRepeatIntervalDays.value = days
            }
        }
    }

    fun setGlobalReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderSettings.setReminderEnabled(enabled)
        }
    }

    fun setGlobalFirstThresholdDays(days: Int) {
        viewModelScope.launch {
            reminderSettings.setFirstThresholdDays(days)
        }
    }

    fun setGlobalRepeatIntervalDays(days: Int) {
        viewModelScope.launch {
            reminderSettings.setRepeatIntervalDays(days)
        }
    }

    fun saveDeviceSetting(deviceId: Long, setting: DeviceNotificationSetting) {
        viewModelScope.launch {
            repository.saveDeviceSetting(setting)
        }
    }

    fun resetDeviceToGlobal(deviceId: Long) {
        viewModelScope.launch {
            repository.deleteDeviceSetting(deviceId)
        }
    }
}
