package com.apislens.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.DeviceRepository
import com.apislens.utils.CostCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val totalDevices: Int = 0,
    val totalInvestmentCents: Long = 0,
    val totalDailyCost: Double = 0.0,
    val totalDaysUsed: Long = 0,
    val devices: List<DeviceWithCost> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> =
        deviceRepository.getAllDevices()
            .map { devices ->
                val devicesWithCost = devices.map { device ->
                    DeviceWithCost(
                        device = device,
                        dailyCost = CostCalculator.calculateDailyCost(
                            device.purchasePriceCents, device.purchaseDate
                        ),
                        daysUsed = CostCalculator.daysSince(device.purchaseDate)
                    )
                }
                DashboardUiState(
                    totalDevices = devices.size,
                    totalInvestmentCents = devices.sumOf { it.purchasePriceCents },
                    totalDailyCost = devicesWithCost.sumOf { it.dailyCost },
                    totalDaysUsed = devicesWithCost.maxOfOrNull { it.daysUsed } ?: 0,
                    devices = devicesWithCost
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
