package com.apislens.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.Device
import com.apislens.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceFormState(
    val name: String = "",
    val model: String = "",
    val iconUri: String? = null,
    val category: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val note: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val deviceId: Long = savedStateHandle.get<Long>("deviceId") ?: 0L
    val isEditing: Boolean = deviceId > 0

    private val _formState = MutableStateFlow(DeviceFormState())
    val formState: StateFlow<DeviceFormState> = _formState.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                deviceRepository.getDeviceOnce(deviceId)?.let { device ->
                    _formState.value = DeviceFormState(
                        name = device.name,
                        model = device.model,
                        iconUri = device.iconUri,
                        category = device.category,
                        purchaseDate = device.purchaseDate,
                        purchasePrice = (device.purchasePriceCents / 100.0).toString(),
                        note = device.note
                    )
                }
            }
        }
    }

    fun updateName(name: String) { _formState.update { it.copy(name = name) } }
    fun updateModel(model: String) { _formState.update { it.copy(model = model) } }
    fun updateIconUri(uri: String?) { _formState.update { it.copy(iconUri = uri) } }
    fun updateCategory(category: String) { _formState.update { it.copy(category = category) } }
    fun updatePurchaseDate(date: String) { _formState.update { it.copy(purchaseDate = date) } }
    fun updatePurchasePrice(price: String) { _formState.update { it.copy(purchasePrice = price) } }
    fun updateNote(note: String) { _formState.update { it.copy(note = note) } }

    fun save() {
        val state = _formState.value
        if (state.name.isBlank()) {
            _formState.update { it.copy(error = "请输入设备名称") }
            return
        }
        if (state.purchaseDate.isBlank()) {
            _formState.update { it.copy(error = "请选择购买日期") }
            return
        }
        val priceCents = try {
            (state.purchasePrice.toDouble() * 100).toLong()
        } catch (e: Exception) {
            _formState.update { it.copy(error = "请输入有效价格") }
            return
        }
        if (priceCents <= 0) {
            _formState.update { it.copy(error = "价格必须大于0") }
            return
        }

        viewModelScope.launch {
            val device = Device(
                id = if (isEditing) deviceId else 0,
                name = state.name.trim(),
                model = state.model.trim(),
                iconUri = state.iconUri,
                category = state.category.trim(),
                purchaseDate = state.purchaseDate,
                purchasePriceCents = priceCents,
                note = state.note.trim()
            )
            if (isEditing) {
                deviceRepository.update(device.copy(updatedAt = System.currentTimeMillis()))
            } else {
                deviceRepository.insert(device)
            }
            _formState.update { it.copy(isSaved = true, error = null) }
        }
    }

    fun clearError() { _formState.update { it.copy(error = null) } }
}
