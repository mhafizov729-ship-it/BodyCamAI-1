package com.example.bodycamai.external

/** A validated frame delivered by a real thermal camera SDK/UVC decoder. */
data class ThermalFrame(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val fps: Float,
    val temperatureDataAvailable: Boolean = false,
    val minCelsius: Float? = null,
    val maxCelsius: Float? = null
) {
    fun isValid(): Boolean =
        timestampNs > 0L && width > 0 && height > 0 && fps > 0f &&
            (!temperatureDataAvailable || (minCelsius != null && maxCelsius != null))
}
