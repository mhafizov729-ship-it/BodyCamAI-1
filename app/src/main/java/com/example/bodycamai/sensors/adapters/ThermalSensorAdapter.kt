package com.example.bodycamai.sensors.adapters

import kotlinx.coroutines.flow.StateFlow

/** Vendor-neutral thermal interface. Real USB/SDK drivers plug into this contract. */
interface ThermalSensorAdapter {
    val state: StateFlow<State>
    fun start()
    fun stop()

    data class State(
        val connected: Boolean = false,
        val width: Int = 0,
        val height: Int = 0,
        val fps: Int = 0,
        val minTemperatureC: Float? = null,
        val maxTemperatureC: Float? = null,
        val error: String? = null
    )
}
