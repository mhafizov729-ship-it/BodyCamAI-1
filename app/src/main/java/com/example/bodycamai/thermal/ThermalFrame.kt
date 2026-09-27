package com.example.bodycamai.thermal

import com.example.bodycamai.core.SensorSource

/**
 * A decoded thermal frame supplied by a real thermal transport/SDK adapter.
 * temperatureC is optional; when absent the frame is still usable as a visual thermal image.
 */
data class ThermalFrame(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val pixels: ByteArray,
    val bytesPerPixel: Int = 2,
    val minTemperatureC: Float? = null,
    val maxTemperatureC: Float? = null,
    val source: SensorSource = SensorSource.THERMAL_CAMERA,
    val confidence: Float = 1f
) {
    init {
        require(width > 0 && height > 0) { "Invalid thermal dimensions" }
        require(bytesPerPixel > 0) { "Invalid pixel format" }
        require(pixels.isNotEmpty()) { "Thermal frame is empty" }
    }
}
