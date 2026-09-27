package com.example.bodycamai.thermal

import com.example.bodycamai.core.SensorFrame
import com.example.bodycamai.core.SensorFusionPipeline
import com.example.bodycamai.core.SensorSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges real thermal frames into the existing Sensor Fusion timestamp pipeline.
 * It never marks thermal as connected until a real frame is submitted.
 */
class ThermalFusionBridge(private val fusion: SensorFusionPipeline) : ThermalFrameSink {
    data class State(
        val streaming: Boolean = false,
        val width: Int = 0,
        val height: Int = 0,
        val minTemperatureC: Float? = null,
        val maxTemperatureC: Float? = null,
        val lastFrameTimestampNs: Long? = null,
        val confidence: Float = 0f,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    override fun onFrame(frame: ThermalFrame) {
        if (frame.source != SensorSource.THERMAL_CAMERA) return
        fusion.offer(
            SensorFrame(
                source = SensorSource.THERMAL_CAMERA,
                timestampNs = frame.timestampNs,
                width = frame.width,
                height = frame.height,
                confidence = frame.confidence
            )
        )
        _state.value = State(
            streaming = true,
            width = frame.width,
            height = frame.height,
            minTemperatureC = frame.minTemperatureC,
            maxTemperatureC = frame.maxTemperatureC,
            lastFrameTimestampNs = frame.timestampNs,
            confidence = frame.confidence.coerceIn(0f, 1f)
        )
    }

    fun reportError(message: String) {
        _state.value = _state.value.copy(streaming = false, error = message)
        fusion.clear(SensorSource.THERMAL_CAMERA)
    }

    fun stop() {
        _state.value = State()
        fusion.clear(SensorSource.THERMAL_CAMERA)
    }
}
