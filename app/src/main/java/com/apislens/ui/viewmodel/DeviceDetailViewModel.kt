package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.ChargeRecordRepository
import com.apislens.data.repository.DeviceRepository
import com.apislens.event.DataSyncEvent
import com.apislens.event.DataSyncEventBus
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
    val batteryHealthScore: Double? = null,
    val batteryConfidence: String = "",
    val batteryReferenceSegment: String? = null,
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
    private val chargeRepo: ChargeRecordRepository,
    private val eventBus: DataSyncEventBus
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceDetailState())
    val state: StateFlow<DeviceDetailState> = _state.asStateFlow()

    private var currentDeviceId: Long = 0L
    private val _refreshTrigger = MutableStateFlow(0L)

    fun loadDevice(id: Long) {
        currentDeviceId = id
        _refreshTrigger.value = System.currentTimeMillis()
    }

    init {
        viewModelScope.launch {
            _refreshTrigger
                .collectLatest { _ ->
                    if (currentDeviceId == 0L) return@collectLatest
                    reloadData()
                }
        }
        viewModelScope.launch {
            eventBus.events.collect { event ->
                val relevant = when (event) {
                    is DataSyncEvent.DeviceUpdated -> event.deviceId == currentDeviceId
                    is DataSyncEvent.ChargeRecordUpdated -> event.deviceId == currentDeviceId
                    is DataSyncEvent.ChargeRecordDeleted -> event.deviceId == currentDeviceId
                    is DataSyncEvent.ChargeRecordsChanged -> event.deviceId == currentDeviceId
                    else -> false
                }
                if (relevant && currentDeviceId != 0L) {
                    reloadData()
                }
            }
        }
    }

    private suspend fun reloadData() {
        _state.value = _state.value.copy(isLoading = true)
        try {
            val device = deviceRepo.getDeviceById(currentDeviceId).first()
            val records = chargeRepo.getRecordsByDevice(currentDeviceId).first()

            val dailyCost = device?.let { RustCore.calculateDailyCost(it.purchasePriceCents, it.purchaseDate) } ?: 0.0
            val daysUsed = device?.let { RustCore.daysSince(it.purchaseDate).toInt().coerceAtLeast(1) } ?: 0
            val totalDepreciation = device?.let { RustCore.totalDepreciation(it.purchasePriceCents, it.purchaseDate, it.lifecycleDays) } ?: 0.0
            val batteryHealthResult = CostCalculator.calculateBatteryHealthV3(records)

            val newState = DeviceDetailState(
                device = device,
                chargeRecords = records,
                batteryHealthData = batteryHealthResult.historyData,
                batteryHealthScore = batteryHealthResult.healthScore,
                batteryConfidence = batteryHealthResult.confidence,
                batteryReferenceSegment = batteryHealthResult.referenceSegment,
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
            eventBus.tryEmit(DataSyncEvent.ChargeRecordDeleted(record.deviceId, record.id))
            eventBus.tryEmit(DataSyncEvent.ChargeRecordsChanged(record.deviceId))
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            deviceRepo.delete(device)
            eventBus.tryEmit(DataSyncEvent.DeviceDeleted(device.id))
            eventBus.tryEmit(DataSyncEvent.DevicesListChanged)
        }
    }
}
