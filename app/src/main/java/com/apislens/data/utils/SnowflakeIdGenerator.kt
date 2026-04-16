package com.apislens.data.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnowflakeIdGenerator @Inject constructor() {

    private var sequence = 0L
    private var lastTimestamp = -1L

    @Volatile
    var workerId: Long = 1L

    @Synchronized
    fun nextId(): Long {
        var timestamp = System.currentTimeMillis() - EPOCH

        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) and SEQUENCE_MASK
            if (sequence == 0L) {
                timestamp = waitNextMillis(timestamp)
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = timestamp

        return ((timestamp and TIMESTAMP_MASK) shl TIMESTAMP_SHIFT) or
                (workerId and WORKER_ID_MASK shl WORKER_ID_SHIFT) or
                (sequence and SEQUENCE_MASK)
    }

    private fun waitNextMillis(currentTimestamp: Long): Long {
        var timestamp = currentTimestamp
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH
        }
        return timestamp
    }

    companion object {
        private const val EPOCH = 1704067200000L
        private const val WORKER_ID_BITS = 5
        private const val SEQUENCE_BITS = 12
        private const val WORKER_ID_MASK = (1L shl WORKER_ID_BITS) - 1L
        private const val SEQUENCE_MASK = (1L shl SEQUENCE_BITS) - 1L
        private const val TIMESTAMP_MASK = (1L shl 41) - 1L
        private const val WORKER_ID_SHIFT = SEQUENCE_BITS
        private const val TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS
    }
}

object IdPrefix {
    private const val DEVICE_PREFIX = 0L
    private const val USAGE_PREFIX = 2L
    private const val CHARGE_PREFIX = 3L
    private const val PREFIX_SHIFT = 62

    fun generateDeviceId(generator: SnowflakeIdGenerator): Long {
        val snowflakeId = generator.nextId()
        return (DEVICE_PREFIX shl PREFIX_SHIFT) or (snowflakeId and ((1L shl PREFIX_SHIFT) - 1L))
    }

    fun generateUsageId(generator: SnowflakeIdGenerator): Long {
        val snowflakeId = generator.nextId()
        return (USAGE_PREFIX shl PREFIX_SHIFT) or (snowflakeId and ((1L shl PREFIX_SHIFT) - 1L))
    }

    fun generateChargeId(generator: SnowflakeIdGenerator): Long {
        val snowflakeId = generator.nextId()
        return (CHARGE_PREFIX shl PREFIX_SHIFT) or (snowflakeId and ((1L shl PREFIX_SHIFT) - 1L))
    }
}
