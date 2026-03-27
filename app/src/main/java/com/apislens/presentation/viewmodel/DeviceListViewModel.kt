package com.apislens.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.DeviceRepository
import com.apislens.utils.CostCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceWithCost(
    val device: Device,
    val dailyCost: Double,
    val daysUsed: Long
)

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    val devicesWithCost: StateFlow<List<DeviceWithCost>> =
        deviceRepository.getAllDevices()
            .map { devices ->
                devices.map { device ->
                    DeviceWithCost(
                        device = device,
                        dailyCost = CostCalculator.calculateDailyCost(
                            device.purchasePriceCents, device.purchaseDate
                        ),
                        daysUsed = CostCalculator.daysSince(device.purchaseDate)
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.delete(device)
        }
    }
}
