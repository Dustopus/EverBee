package com.apislens.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.ChargeRecord
import com.apislens.data.repository.ChargeRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChargeFormState(
    val startLevel: Int = 20,
    val endLevel: Int = 100,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis(),
    val note: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddChargeRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chargeRecordRepository: ChargeRecordRepository
) : ViewModel() {

    private val deviceId: Long = savedStateHandle.get<Long>("deviceId") ?: 0L

    private val _formState = MutableStateFlow(ChargeFormState())
    val formState: StateFlow<ChargeFormState> = _formState.asStateFlow()

    fun updateStartLevel(level: Int) { _formState.update { it.copy(startLevel = level) } }
    fun updateEndLevel(level: Int) { _formState.update { it.copy(endLevel = level) } }
    fun updateStartTime(time: Long) { _formState.update { it.copy(startTime = time) } }
    fun updateEndTime(time: Long) { _formState.update { it.copy(endTime = time) } }
    fun updateNote(note: String) { _formState.update { it.copy(note = note) } }

    fun save() {
        val state = _formState.value
        if (state.startTime >= state.endTime) {
            _formState.update { it.copy(error = "结束时间必须晚于开始时间") }
            return
        }
        if (state.startLevel > state.endLevel) {
            _formState.update { it.copy(error = "结束电量不能低于开始电量") }
            return
        }

        viewModelScope.launch {
            val durationMin = ((state.endTime - state.startTime) / 60000).toInt()
            val record = ChargeRecord(
                deviceId = deviceId,
                startTime = state.startTime,
                endTime = state.endTime,
                startLevel = state.startLevel,
                endLevel = state.endLevel,
                durationMinutes = durationMin,
                note = state.note.trim()
            )
            chargeRecordRepository.insert(record)
            _formState.update { it.copy(isSaved = true, error = null) }
        }
    }

    fun clearError() { _formState.update { it.copy(error = null) } }
}
