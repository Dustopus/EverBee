package com.apislens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apislens.data.local.entity.UsageRecord
import com.apislens.data.repository.UsageRecordRepository
import com.apislens.data.utils.IdPrefix
import com.apislens.data.utils.SnowflakeIdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AddUsageRecordViewModel @Inject constructor(
    private val repo: UsageRecordRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator
) : ViewModel() {
    val usageMinutes = MutableStateFlow("")
    val note = MutableStateFlow("")
    fun save(deviceId: Long) {
        viewModelScope.launch {
            val minutes = usageMinutes.value.toIntOrNull() ?: 0
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val record = UsageRecord(
                id = IdPrefix.generateUsageId(snowflakeIdGenerator),
                deviceId = deviceId,
                date = today,
                usageMinutes = minutes,
                note = note.value
            )
            repo.insert(record)
        }
    }
}
