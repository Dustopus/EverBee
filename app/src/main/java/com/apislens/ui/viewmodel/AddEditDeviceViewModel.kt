package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditDeviceViewModel @Inject constructor(private val repo: DeviceRepository) : ViewModel() {
    var deviceId: Long? = null
    val name = MutableStateFlow("")
    val model = MutableStateFlow("")
    val category = MutableStateFlow("")
    val purchaseDate = MutableStateFlow("")
    val purchasePrice = MutableStateFlow("")
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
            note.value = device.note
        }
    }

    fun validate(): Boolean {
        if (name.value.isBlank()) return false
        if (purchasePrice.value.toDoubleOrNull() == null || purchasePrice.value.toDouble() <= 0) return false
        return true
    }

    fun save() {
        viewModelScope.launch {
            isSaving.value = true
            try {
                val price = purchasePrice.value.toDoubleOrNull() ?: 0.0
                val priceCents = (price * 100).toLong()
                val date = purchaseDate.value.ifEmpty { java.time.LocalDate.now().toString() }
                val now = System.currentTimeMillis()
                val device = Device(
                    id = deviceId ?: 0,
                    name = name.value.trim(),
                    model = model.value.trim(),
                    category = category.value,
                    purchaseDate = date,
                    purchasePriceCents = priceCents,
                    note = note.value.trim(),
                    createdAt = if (deviceId == null) now else {
                        repo.getDeviceOnce(deviceId!!)?.createdAt ?: now
                    },
                    updatedAt = now
                )
                if (deviceId == null) repo.insert(device) else repo.update(device)
            } finally {
                isSaving.value = false
            }
        }
    }
}
