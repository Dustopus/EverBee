package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.DeviceRepository
import com.apislens.data.utils.IdPrefix
import com.apislens.data.utils.SnowflakeIdGenerator
import com.apislens.event.DataSyncEvent
import com.apislens.event.DataSyncEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditDeviceViewModel @Inject constructor(
    private val repo: DeviceRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
    private val eventBus: DataSyncEventBus
) : ViewModel() {
    var deviceId: Long? = null
    val name = MutableStateFlow("")
    val model = MutableStateFlow("")
    val category = MutableStateFlow("")
    val purchaseDate = MutableStateFlow("")
    val purchasePrice = MutableStateFlow("")
    val lifecycleMonths = MutableStateFlow(Device.DEFAULT_LIFECYCLE_MONTHS.toString())
    val note = MutableStateFlow("")
    val showDatePicker = MutableStateFlow(false)
    val isSaving = MutableStateFlow(false)

    fun loadDevice(id: Long) {
        if (deviceId != null) return
        deviceId = id
        viewModelScope.launch {
            val device = repo.getDeviceOnce(id) ?: return@launch
            name.value = device.name
            model.value = device.model
            category.value = device.category
            purchaseDate.value = device.purchaseDate
            purchasePrice.value = String.format("%.2f", device.purchasePrice)
            lifecycleMonths.value = device.lifecycleMonths.toString()
            note.value = device.note
        }
    }

    fun validate(): Boolean {
        if (name.value.isBlank()) return false
        if (purchasePrice.value.toDoubleOrNull() == null || purchasePrice.value.toDouble() <= 0) return false
        val months = lifecycleMonths.value.toIntOrNull()
        if (months == null || months <= 0) return false
        return true
    }

    fun save() {
        viewModelScope.launch {
            isSaving.value = true
            try {
                val price = purchasePrice.value.toDoubleOrNull() ?: 0.0
                val priceCents = (price * 100).toLong()
                val date = purchaseDate.value.ifEmpty { java.time.LocalDate.now().toString() }
                val months = lifecycleMonths.value.toIntOrNull() ?: Device.DEFAULT_LIFECYCLE_MONTHS
                val now = System.currentTimeMillis()
                val device = Device(
                    id = deviceId ?: IdPrefix.generateDeviceId(snowflakeIdGenerator),
                    name = name.value.trim(),
                    model = model.value.trim(),
                    category = category.value,
                    purchaseDate = date,
                    purchasePriceCents = priceCents,
                    lifecycleMonths = months.coerceAtLeast(1),
                    note = note.value.trim(),
                    createdAt = if (deviceId == null) now else {
                        repo.getDeviceOnce(deviceId!!)?.createdAt ?: now
                    },
                    updatedAt = now
                )
                val currentDeviceId = deviceId
                if (currentDeviceId == null) {
                    val newId = repo.insert(device)
                    eventBus.tryEmit(DataSyncEvent.DeviceUpdated(newId))
                    eventBus.tryEmit(DataSyncEvent.DevicesListChanged)
                } else {
                    repo.update(device)
                    eventBus.tryEmit(DataSyncEvent.DeviceUpdated(currentDeviceId))
                    eventBus.tryEmit(DataSyncEvent.DevicesListChanged)
                }
            } finally {
                isSaving.value = false
            }
        }
    }
}
