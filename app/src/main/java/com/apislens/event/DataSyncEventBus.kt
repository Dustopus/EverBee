package com.apislens.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class DataSyncEvent {
    data class DeviceUpdated(val deviceId: Long) : DataSyncEvent()
    data class DeviceDeleted(val deviceId: Long) : DataSyncEvent()
    object DevicesListChanged : DataSyncEvent()
    
    data class ChargeRecordUpdated(val deviceId: Long, val recordId: Long) : DataSyncEvent()
    data class ChargeRecordDeleted(val deviceId: Long, val recordId: Long) : DataSyncEvent()
    data class ChargeRecordsChanged(val deviceId: Long) : DataSyncEvent()
    
    object GlobalRefresh : DataSyncEvent()
}

@Singleton
class DataSyncEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<DataSyncEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    
    val events: SharedFlow<DataSyncEvent> = _events.asSharedFlow()
    
    suspend fun emit(event: DataSyncEvent) {
        _events.emit(event)
    }
    
    fun tryEmit(event: DataSyncEvent): Boolean {
        return _events.tryEmit(event)
    }
}
