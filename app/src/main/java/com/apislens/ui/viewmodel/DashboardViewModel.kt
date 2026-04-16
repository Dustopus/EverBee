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
import com.apislens.ui.components.PieChartData
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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val chargeRepo: ChargeRecordRepository,
    private val eventBus: DataSyncEventBus
) : ViewModel() {

    private val _cachedStats = MutableStateFlow(DashboardStats())
    private val _isRefreshing = MutableStateFlow(false)
    private val _pieChartData = MutableStateFlow<List<PieChartData>>(emptyList())

    val devices = deviceRepo.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<DashboardStats> = _cachedStats.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val pieChartData: StateFlow<List<PieChartData>> = _pieChartData.asStateFlow()

    init {
        viewModelScope.launch {
            devices.collectLatest { deviceList ->
                refreshStats(deviceList)
                refreshPieChart(deviceList)
            }
        }
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DataSyncEvent.ChargeRecordUpdated,
                    is DataSyncEvent.ChargeRecordDeleted,
                    is DataSyncEvent.ChargeRecordsChanged -> {
                        val currentDevices = devices.value
                        refreshStats(currentDevices)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun refreshPieChart(deviceList: List<Device>) {
        _pieChartData.value = deviceList.map { device ->
            val label = device.name.ifEmpty { device.model.ifEmpty { "未命名设备" } }
            val purchase = device.purchasePrice
            val depreciation = RustCore.totalDepreciation(device.purchasePriceCents, device.purchaseDate, device.lifecycleDays)
            PieChartData(
                label = label,
                purchaseValue = purchase,
                depreciationValue = depreciation
            )
        }.sortedByDescending { it.purchaseValue }
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
