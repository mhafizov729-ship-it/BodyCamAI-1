package com.example.bodycamai.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

/** Runtime-only battery/thermal telemetry. No background service is started here. */
data class DevicePowerState(
    val batteryPercent: Int? = null,
    val charging: Boolean = false,
    val powerSave: Boolean = false,
    val thermalStatus: Int = PowerManager.THERMAL_STATUS_NONE,
    val thermalThrottling: Boolean = false
)

class DevicePowerMonitor(private val context: Context) {
    fun snapshot(): DevicePowerState {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermal = if (Build.VERSION.SDK_INT >= 29) pm.currentThermalStatus else PowerManager.THERMAL_STATUS_NONE
        return DevicePowerState(
            batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null,
            charging = charging,
            powerSave = pm.isPowerSaveMode,
            thermalStatus = thermal,
            thermalThrottling = thermal >= PowerManager.THERMAL_STATUS_MODERATE
        )
    }
}
