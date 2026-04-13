package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.ChargeRecordRepository
import com.apislens.data.repository.DeviceRepository
import com.apislens.rust.RustCore
import com.apislens.utils.CostCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceDetailState(
    val device: Device? = null,
    val chargeRecords: List<ChargeRecord> = emptyList(),
    val batteryHealthData: List<Pair<String, Double>> = emptyList(),
    val dailyCost: Double = 0.0,
    val daysUsed: Int = 0,
    val totalDepreciation: Double = 0.0,
    val isLoading: Boolean = true,
    val heatmapYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val heatmapDayCounts: Map<String, Int> = emptyMap(),
    val heatmapDayRecords: Map<String, List<ChargeRecord>> = emptyMap()
)

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val chargeRepo: ChargeRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceDetailState())
    val state: StateFlow<DeviceDetailState> = _state.asStateFlow()

    private var currentDeviceId: Long = 0L

    fun loadDevice(id: Long) {
        currentDeviceId = id
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val device = deviceRepo.getDeviceById(id).first()
                val records = chargeRepo.getRecordsByDevice(id).first()

                val dailyCost = device?.let { RustCore.calculateDailyCost(it.purchasePriceCents, it.purchaseDate) } ?: 0.0
                val daysUsed = device?.let { RustCore.daysSince(it.purchaseDate).toInt().coerceAtLeast(1) } ?: 0
                val totalDepreciation = device?.let { RustCore.totalDepreciation(it.purchasePriceCents, it.purchaseDate) } ?: 0.0
                val batteryHealth = CostCalculator.calculateBatteryHealth(records)

                val newState = DeviceDetailState(
                    device = device,
                    chargeRecords = records,
                    batteryHealthData = batteryHealth,
                    dailyCost = dailyCost,
                    daysUsed = daysUsed,
                    totalDepreciation = totalDepreciation,
                    isLoading = false
                )
                _state.value = newState
                rebuildHeatmap(records, newState.heatmapYear)
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun setHeatmapYear(year: Int) {
        val currentState = _state.value
        _state.value = currentState.copy(heatmapYear = year)
        rebuildHeatmap(currentState.chargeRecords, year)
    }

    private fun rebuildHeatmap(records: List<ChargeRecord>, year: Int) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val dayCounts = mutableMapOf<String, Int>()
        val dayRecords = mutableMapOf<String, MutableList<ChargeRecord>>()
        for (record in records) {
            val dateKey = sdf.format(java.util.Date(record.startTime))
            val cal = java.util.Calendar.getInstance()
            cal.time = java.util.Date(record.startTime)
            if (cal.get(java.util.Calendar.YEAR) == year) {
                dayCounts[dateKey] = (dayCounts[dateKey] ?: 0) + 1
                dayRecords.getOrPut(dateKey) { mutableListOf() }.add(record)
            }
        }
        _state.value = _state.value.copy(
            heatmapDayCounts = dayCounts,
            heatmapDayRecords = dayRecords.mapValues { it.value.toList() }
        )
    }

    fun deleteChargeRecord(record: com.apislens.data.local.entity.ChargeRecord) {
        viewModelScope.launch {
            chargeRepo.delete(record)
            loadDevice(currentDeviceId)
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            deviceRepo.delete(device)
        }
    }
}
