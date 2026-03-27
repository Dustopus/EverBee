package com.apislens.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.UsageRecord
import com.apislens.data.repository.UsageRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class UsageFormState(
    val date: String = LocalDate.now().toString(),
    val hours: Int = 0,
    val minutes: Int = 0,
    val note: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddUsageRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usageRecordRepository: UsageRecordRepository
) : ViewModel() {

    private val deviceId: Long = savedStateHandle.get<Long>("deviceId") ?: 0L

    private val _formState = MutableStateFlow(UsageFormState())
    val formState: StateFlow<UsageFormState> = _formState.asStateFlow()

    fun updateDate(date: String) { _formState.update { it.copy(date = date) } }
    fun updateHours(hours: Int) { _formState.update { it.copy(hours = hours) } }
    fun updateMinutes(minutes: Int) { _formState.update { it.copy(minutes = minutes) } }
    fun updateNote(note: String) { _formState.update { it.copy(note = note) } }

    fun save() {
        val state = _formState.value
        val totalMinutes = state.hours * 60 + state.minutes
        if (totalMinutes <= 0) {
            _formState.update { it.copy(error = "使用时长必须大于0") }
            return
        }
        if (state.date.isBlank()) {
            _formState.update { it.copy(error = "请选择日期") }
            return
        }

        viewModelScope.launch {
            val existing = usageRecordRepository.getRecordByDeviceAndDate(deviceId, state.date)
            if (existing != null) {
                // 更新已有记录（累加时长）
                usageRecordRepository.update(
                    existing.copy(
                        usageMinutes = existing.usageMinutes + totalMinutes,
                        note = state.note.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                usageRecordRepository.insert(
                    UsageRecord(
                        deviceId = deviceId,
                        date = state.date,
                        usageMinutes = totalMinutes,
                        note = state.note.trim()
                    )
                )
            }
            _formState.update { it.copy(isSaved = true, error = null) }
        }
    }

    fun clearError() { _formState.update { it.copy(error = null) } }
}
