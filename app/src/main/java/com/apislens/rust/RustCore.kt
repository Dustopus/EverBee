package com.apislens.rust

import android.util.Log
import com.apislens.utils.CostCalculator

object RustCore {

    private const val TAG = "RustCore"
    private var nativeLoaded = false

    init {
        try {
            System.loadLibrary("apislens_core")
            nativeLoaded = true
            Log.i(TAG, "Rust native library loaded successfully, version: ${nativeGetVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            nativeLoaded = false
            Log.w(TAG, "Rust native library not available, using Kotlin fallback", e)
        }
    }

    fun calculateDailyCost(purchasePriceCents: Long, purchaseDate: String): Double {
        return if (nativeLoaded) {
            try {
                nativeCalculateDailyCost(purchasePriceCents, purchaseDate)
            } catch (e: Exception) {
                Log.w(TAG, "Rust calculateDailyCost failed, fallback to Kotlin", e)
                CostCalculator.calculateDailyCost(purchasePriceCents, purchaseDate)
            }
        } else {
            CostCalculator.calculateDailyCost(purchasePriceCents, purchaseDate)
        }
    }

    fun daysSince(purchaseDate: String): Long {
        return if (nativeLoaded) {
            try {
                nativeDaysSince(purchaseDate)
            } catch (e: Exception) {
                Log.w(TAG, "Rust daysSince failed, fallback to Kotlin", e)
                CostCalculator.daysSince(purchaseDate)
            }
        } else {
            CostCalculator.daysSince(purchaseDate)
        }
    }

    fun totalDepreciation(purchasePriceCents: Long, purchaseDate: String): Double {
        return if (nativeLoaded) {
            try {
                nativeTotalDepreciation(purchasePriceCents, purchaseDate)
            } catch (e: Exception) {
                Log.w(TAG, "Rust totalDepreciation failed, fallback to Kotlin", e)
                CostCalculator.totalDepreciation(purchasePriceCents, purchaseDate)
            }
        } else {
            CostCalculator.totalDepreciation(purchasePriceCents, purchaseDate)
        }
    }

    fun needsChargeReminder(lastChargeTime: Long): Boolean {
        return if (nativeLoaded) {
            try {
                nativeNeedsChargeReminder(lastChargeTime)
            } catch (e: Exception) {
                Log.w(TAG, "Rust needsChargeReminder failed, fallback to Kotlin", e)
                CostCalculator.needsChargeReminder(if (lastChargeTime == 0L) null else lastChargeTime)
            }
        } else {
            CostCalculator.needsChargeReminder(if (lastChargeTime == 0L) null else lastChargeTime)
        }
    }

    fun getVersion(): String {
        return if (nativeLoaded) {
            try { nativeGetVersion() } catch (_: Exception) { "unknown" }
        } else {
            "kotlin-fallback"
        }
    }

    fun isNativeAvailable(): Boolean = nativeLoaded

    private external fun nativeCalculateDailyCost(purchasePriceCents: Long, purchaseDate: String): Double
    private external fun nativeDaysSince(purchaseDate: String): Long
    private external fun nativeTotalDepreciation(purchasePriceCents: Long, purchaseDate: String): Double
    private external fun nativeNeedsChargeReminder(lastChargeTime: Long): Boolean
    private external fun nativeGetVersion(): String
}
