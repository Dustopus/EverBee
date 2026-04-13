package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.ChargeRecordRepository
import com.apislens.data.repository.DeviceRepository
import com.apislens.rust.RustCore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardStats(
    val totalDevices: Int = 0,
    val totalDailyCost: Double = 0.0,
    val totalPurchaseCost: Double = 0.0,
    val latestChargedDevice: Device? = null,
    val latestChargeRecord: ChargeRecord? = null,
    val longestUnchargedDevice: Device? = null,
    val longestUnchargedDays: Long = 0L,
    val averageDailyCost: Double = 0.0
)

data class ChargeHeatmapData(
    val date: String,
    val count: Int
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val chargeRepo: ChargeRecordRepository
) : ViewModel() {

    private val _cachedStats = MutableStateFlow(DashboardStats())
    private val _isRefreshing = MutableStateFlow(false)
    private val _heatmapData = MutableStateFlow<List<ChargeHeatmapData>>(emptyList())
    private val _heatmapYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    private val _allChargeRecords = MutableStateFlow<List<ChargeRecord>>(emptyList())

    val devices = deviceRepo.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<DashboardStats> = _cachedStats.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val heatmapData: StateFlow<List<ChargeHeatmapData>> = _heatmapData.asStateFlow()
    val heatmapYear: StateFlow<Int> = _heatmapYear.asStateFlow()

    init {
        viewModelScope.launch {
            devices.collectLatest { deviceList ->
                refreshStats(deviceList)
            }
        }
        viewModelScope.launch {
            chargeRepo.getAllChargeRecords().collectLatest { records ->
                _allChargeRecords.value = records
                refreshHeatmap(records, _heatmapYear.value)
            }
        }
    }

    fun setHeatmapYear(year: Int) {
        _heatmapYear.value = year
        refreshHeatmap(_allChargeRecords.value, year)
    }

    private fun refreshHeatmap(records: List<ChargeRecord>, year: Int) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val heatmap = mutableListOf<ChargeHeatmapData>()
        for (record in records) {
            cal.time = java.util.Date(record.startTime)
            if (cal.get(java.util.Calendar.YEAR) == year) {
                val dateKey = sdf.format(java.util.Date(record.startTime))
                heatmap.add(ChargeHeatmapData(date = dateKey, count = 1))
            }
        }
        val grouped = heatmap.groupBy { it.date }.map { (date, list) ->
            ChargeHeatmapData(date = date, count = list.size)
        }.sortedBy { it.date }
        _heatmapData.value = grouped
    }

    private suspend fun refreshStats(deviceList: List<Device>) {
        _isRefreshing.value = true
        try {
            val totalDevices = deviceList.size
            val totalDailyCost = deviceList.sumOf { d ->
                RustCore.calculateDailyCost(d.purchasePriceCents, d.purchaseDate)
            }
            val totalPurchaseCost = deviceList.sumOf { it.purchasePrice }
            val averageDailyCost = if (totalDevices > 0) totalDailyCost / totalDevices else 0.0

            var latestChargedDevice: Device? = null
            var latestChargeRecord: ChargeRecord? = null
            var longestUnchargedDevice: Device? = null
            var longestUnchargedDays = 0L

            try {
                latestChargeRecord = chargeRepo.getLatestChargeRecord()
                if (latestChargeRecord != null) {
                    latestChargedDevice = deviceRepo.getDeviceOnce(latestChargeRecord.deviceId)
                }
            } catch (_: Exception) {}

            try {
                var maxUnchargedDays = 0L
                var maxUnchargedDevice: Device? = null
                for (dev in deviceList) {
                    val lastValidChargeTime = chargeRepo.getLatestValidChargeTimeByDevice(dev.id)
                    val daysSinceLastCharge = if (lastValidChargeTime != null) {
                        (System.currentTimeMillis() - lastValidChargeTime) / 86_400_000L
                    } else {
                        RustCore.daysSince(dev.purchaseDate)
                    }
                    if (daysSinceLastCharge > maxUnchargedDays) {
                        maxUnchargedDays = daysSinceLastCharge
                        maxUnchargedDevice = dev
                    }
                }
                longestUnchargedDevice = maxUnchargedDevice
                longestUnchargedDays = maxUnchargedDays
            } catch (_: Exception) {}

            _cachedStats.value = DashboardStats(
                totalDevices = totalDevices,
                totalDailyCost = totalDailyCost,
                totalPurchaseCost = totalPurchaseCost,
                latestChargedDevice = latestChargedDevice,
                latestChargeRecord = latestChargeRecord,
                longestUnchargedDevice = longestUnchargedDevice,
                longestUnchargedDays = longestUnchargedDays,
                averageDailyCost = averageDailyCost
            )
        } finally {
            _isRefreshing.value = false
        }
    }
}
