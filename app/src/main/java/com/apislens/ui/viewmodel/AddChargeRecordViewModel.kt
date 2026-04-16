package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.repository.ChargeRecordRepository
import com.apislens.data.utils.IdPrefix
import com.apislens.data.utils.SnowflakeIdGenerator
import com.apislens.event.DataSyncEvent
import com.apislens.event.DataSyncEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddChargeRecordViewModel @Inject constructor(
    private val repo: ChargeRecordRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator,
    private val eventBus: DataSyncEventBus
) : ViewModel() {
    val startLevel = MutableStateFlow("")
    val endLevel = MutableStateFlow("")
    val note = MutableStateFlow("")
    
    val startDate = MutableStateFlow("")
    val startTime = MutableStateFlow("")
    val endDate = MutableStateFlow("")
    val endTime = MutableStateFlow("")
    
    val showStartDatePicker = MutableStateFlow(false)
    val showStartTimePicker = MutableStateFlow(false)
    val showEndDatePicker = MutableStateFlow(false)
    val showEndTimePicker = MutableStateFlow(false)
    
    val timeValidationError = MutableStateFlow<String?>(null)
    val isOvernightCharge = MutableStateFlow(false)

    init {
        initializeDefaultTimes()
    }

    fun initializeDefaultTimes() {
        val now = LocalDateTime.now()
        val oneHourAgo = now.minusHours(1)

        startDate.value = now.toLocalDate().toString()
        startTime.value = oneHourAgo.toLocalTime().withSecond(0).withNano(0).toString()
        endDate.value = now.toLocalDate().toString()
        endTime.value = now.toLocalTime().withSecond(0).withNano(0).toString()
        
        checkAndUpdateOvernightStatus()
    }

    fun onStartTimeChanged(newTime: String) {
        startTime.value = newTime
        autoAdjustEndDateIfNeeded()
        validateTimeLogic()
    }

    fun onEndTimeChanged(newTime: String) {
        endTime.value = newTime
        autoAdjustEndDateIfNeeded()
        validateTimeLogic()
    }

    fun onStartDateChanged(newDate: String) {
        startDate.value = newDate
        if (endDate.value.isEmpty()) {
            endDate.value = newDate
        }
        validateTimeLogic()
    }

    fun onEndDateChanged(newDate: String) {
        endDate.value = newDate
        validateTimeLogic()
    }

    private fun autoAdjustEndDateIfNeeded() {
        if (startTime.value.isEmpty() || endTime.value.isEmpty() || startDate.value.isEmpty()) return
        
        try {
            val startLocalTime = LocalTime.parse(startTime.value)
            val endLocalTime = LocalTime.parse(endTime.value)
            
            if (endLocalTime.isBefore(startLocalTime)) {
                val startLocalDate = LocalDate.parse(startDate.value)
                endDate.value = startLocalDate.plusDays(1).toString()
                isOvernightCharge.value = true
            } else if (endLocalTime.isAfter(startLocalTime)) {
                if (!isOvernightCharge.value || endDate.value != startDate.value) {
                    endDate.value = startDate.value
                    isOvernightCharge.value = false
                }
            }
        } catch (_: Exception) { }
    }

    private fun checkAndUpdateOvernightStatus() {
        try {
            if (startDate.value.isEmpty() || endDate.value.isEmpty() || 
                startTime.value.isEmpty() || endTime.value.isEmpty()) return
                
            val startDateTime = parseDateTime(startDate.value, startTime.value)
            val endDateTime = parseDateTime(endDate.value, endTime.value)
            
            isOvernightCharge.value = !startDateTime.toLocalDate().isEqual(endDateTime.toLocalDate())
        } catch (_: Exception) { }
    }

    fun validateTimeLogic(): Boolean {
        if (startTime.value.isEmpty() || endTime.value.isEmpty()) {
            timeValidationError.value = "请填写完整的时间信息"
            return false
        }
        
        if (startDate.value.isEmpty()) {
            timeValidationError.value = "请选择开始日期"
            return false
        }

        return try {
            val startDateTime = parseDateTime(startDate.value, startTime.value)
            val endDateTime = parseDateTime(
                endDate.value.ifEmpty { startDate.value }, 
                endTime.value
            )

            when {
                endDateTime.isBefore(startDateTime) -> {
                    timeValidationError.value = "结束时间不能早于开始时间"
                    false
                }
                endDateTime == startDateTime -> {
                    timeValidationError.value = "结束时间不能等于开始时间"
                    false
                }
                else -> {
                    timeValidationError.value = null
                    true
                }
            }
        } catch (e: Exception) {
            timeValidationError.value = "时间格式无效"
            false
        }
    }

    private fun parseDateTime(date: String, time: String): LocalDateTime {
        return try {
            val dateTimeStr = "${date}T${time}"
            LocalDateTime.parse(dateTimeStr)
        } catch (_: Exception) {
            LocalDateTime.now()
        }
    }

    fun save(deviceId: Long) {
        if (!validateTimeLogic()) return

        viewModelScope.launch {
            val start = startLevel.value.toIntOrNull() ?: 0
            val end = endLevel.value.toIntOrNull() ?: 0

            val effectiveEndDate = endDate.value.ifEmpty { startDate.value }
            
            val startMillis = try {
                parseDateTime(startDate.value, startTime.value)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val endMillis = try {
                parseDateTime(effectiveEndDate, endTime.value)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val durationMinutes = ((endMillis - startMillis) / (1000 * 60)).toInt().coerceAtLeast(1)

            val record = ChargeRecord(
                id = IdPrefix.generateChargeId(snowflakeIdGenerator),
                deviceId = deviceId,
                startTime = startMillis,
                endTime = endMillis,
                startLevel = start,
                endLevel = end,
                durationMinutes = durationMinutes,
                note = note.value
            )
            repo.insert(record)
            eventBus.tryEmit(DataSyncEvent.ChargeRecordUpdated(deviceId, record.id))
            eventBus.tryEmit(DataSyncEvent.ChargeRecordsChanged(deviceId))
        }
    }
}
