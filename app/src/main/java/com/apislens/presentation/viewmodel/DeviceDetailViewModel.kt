package com.apislens.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.local.entity.Device
import com.apislens.data.local.entity.UsageRecord
import com.apislens.data.repository.ChargeRecordRepository
import com.apislens.data.repository.DeviceRepository
import com.apislens.data.repository.UsageRecordRepository
import com.apislens.utils.CostCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceDetailUiState(
    val device: Device? = null,
    val dailyCost: Double = 0.0,
    val totalDepreciation: Double = 0.0,
    val daysUsed: Long = 0,
    val chargeRecords: List<ChargeRecord> = emptyList(),
    val usageRecords: List<UsageRecord> = emptyList()
)

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val chargeRecordRepository: ChargeRecordRepository,
    private val usageRecordRepository: UsageRecordRepository
) : ViewModel() {

    private val deviceId: Long = savedStateHandle.get<Long>("deviceId") ?: 0L

    val uiState: StateFlow<DeviceDetailUiState> = combine(
        deviceRepository.getDeviceById(deviceId),
        chargeRecordRepository.getRecordsByDeviceAsc(deviceId),
        usageRecordRepository.getRecordsByDeviceAsc(deviceId)
    ) { device, chargeRecords, usageRecords ->
        DeviceDetailUiState(
            device = device,
            dailyCost = device?.let {
                CostCalculator.calculateDailyCost(it.purchasePriceCents, it.purchaseDate)
            } ?: 0.0,
            totalDepreciation = device?.let {
                CostCalculator.totalDepreciation(it.purchasePriceCents, it.purchaseDate)
            } ?: 0.0,
            daysUsed = device?.let { CostCalculator.daysSince(it.purchaseDate) } ?: 0,
            chargeRecords = chargeRecords,
            usageRecords = usageRecords
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeviceDetailUiState())

    fun deleteDevice() {
        viewModelScope.launch {
            deviceId.let { deviceRepository.deleteById(it) }
        }
    }
}
