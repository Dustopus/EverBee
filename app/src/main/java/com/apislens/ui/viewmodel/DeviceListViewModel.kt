package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.ChargeRecordRepository
import com.apislens.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val repo: DeviceRepository,
    private val chargeRepo: ChargeRecordRepository
) : ViewModel() {
    val allDevices = repo.getAllDevices().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _lastChargeTimes = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val lastChargeTimes: StateFlow<Map<Long, Long>> = _lastChargeTimes

    val devices: StateFlow<List<Device>> = combine(allDevices, _selectedCategories, _searchQuery) { list, categories, query ->
        var filtered = list
        if (categories.isNotEmpty()) {
            filtered = filtered.filter { it.category in categories }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) || it.model.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val availableCategories: StateFlow<List<String>> = allDevices.map { list ->
        list.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            allDevices.collectLatest { deviceList ->
                val times = mutableMapOf<Long, Long>()
                for (device in deviceList) {
                    try {
                        val time = chargeRepo.getLatestChargeTimeByDevice(device.id)
                        if (time != null) times[device.id] = time
                    } catch (_: Exception) {}
                }
                _lastChargeTimes.value = times
            }
        }
    }

    fun toggleCategory(category: String) {
        _selectedCategories.update { current ->
            if (category in current) current - category else current + category
        }
    }

    fun clearCategories() {
        _selectedCategories.value = emptySet()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch { repo.delete(device) }
    }
}
