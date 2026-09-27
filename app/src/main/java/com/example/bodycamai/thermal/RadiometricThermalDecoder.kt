package com.example.bodycamai.thermal

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Decodes a common little-endian 16-bit radiometric payload.
 * It does not assume a specific manufacturer: calibration must be supplied by the adapter.
 * Formula: temperatureC = raw * scale + offset.
 */
class RadiometricThermalDecoder(
    private val scaleCPerUnit: Float,
    private val offsetC: Float
) {
    fun decode(
        payload: ByteArray,
        width: Int,
        height: Int,
        timestampNs: Long,
        confidence: Float = 1f
    ): ThermalFrame? {
        if (width <= 0 || height <= 0 || payload.size < width * height * 2) return null
        val values = FloatArray(width * height)
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        var minTemp = Float.POSITIVE_INFINITY
        var maxTemp = Float.NEGATIVE_INFINITY
        for (i in values.indices) {
            val raw = buffer.short.toInt() and 0xFFFF
            val temp = raw * scaleCPerUnit + offsetC
            values[i] = temp
            minTemp = min(minTemp, temp)
            maxTemp = max(maxTemp, temp)
        }
        return ThermalFrame(
            timestampNs = timestampNs,
            width = width,
            height = height,
            pixels = payload.copyOf(),
            bytesPerPixel = 2,
            minTemperatureC = minTemp.takeIf { it.isFinite() },
            maxTemperatureC = maxTemp.takeIf { it.isFinite() },
            confidence = confidence.coerceIn(0f, 1f)
        )
    }
}
