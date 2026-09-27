package com.example.bodycamai.sensors.adapters

import com.example.bodycamai.core.SensorFrame
import com.example.bodycamai.core.SensorSource
import kotlinx.coroutines.flow.StateFlow

/** Common contract for real hardware/SDK adapters. No adapter may report connected without a live source. */
interface SensorAdapter {
    val source: SensorSource
    val status: StateFlow<AdapterStatus>
    fun start(onFrame: (SensorFrame) -> Unit): Boolean
    fun stop()
}

data class AdapterStatus(
    val connected: Boolean = false,
    val fps: Float? = null,
    val latencyMs: Long? = null,
    val message: String? = null
)
