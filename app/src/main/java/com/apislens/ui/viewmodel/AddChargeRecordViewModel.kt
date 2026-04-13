package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.repository.ChargeRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddChargeRecordViewModel @Inject constructor(private val repo: ChargeRecordRepository) : ViewModel() {
    val startLevel = MutableStateFlow("")
    val endLevel = MutableStateFlow("")
    val note = MutableStateFlow("")
    val chargeDate = MutableStateFlow("")
    val chargeTime = MutableStateFlow("")
    val showDatePicker = MutableStateFlow(false)
    val showTimePicker = MutableStateFlow(false)

    fun save(deviceId: Long) {
        viewModelScope.launch {
            val start = startLevel.value.toIntOrNull() ?: 0
            val end = endLevel.value.toIntOrNull() ?: 0
            val startTime = try {
                val dateTimeStr = "${chargeDate.value}T${chargeTime.value}"
                java.time.LocalDateTime.parse(dateTimeStr)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
            val record = ChargeRecord(
                deviceId = deviceId,
                startTime = startTime,
                endTime = startTime,
                startLevel = start,
                endLevel = end,
                note = note.value
            )
            repo.insert(record)
        }
    }
}
