package com.example.bodycamai.core

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import java.util.concurrent.TimeUnit

/** Timestamped metadata for one frame. Pixel buffers remain owned by their camera/decoder. */
data class SensorFrame(
    val source: SensorSource,
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
    val confidence: Float = 1f
)

data class FusedFrameState(
    val primary: SensorFrame? = null,
    val secondary: SensorFrame? = null,
    val deltaMs: Long? = null,
    val synchronized: Boolean = false,
    val sourceCount: Int = 0,
    val updatedAtMs: Long = SystemClock.elapsedRealtime(),
    val health: List<SensorHealth> = emptyList()
)

/**
 * Lightweight synchronization core. It pairs frames by timestamp and exposes health to the HUD.
 * Actual external pixel decoding is deliberately delegated to an adapter/SDK.
 */
private fun nowElapsedNs(): Long = SystemClock.elapsedRealtime() * 1_000_000L

class SensorFusionPipeline(private val maxDeltaMs: Long = 120L) {
    private val latest = ConcurrentHashMap<SensorSource, SensorFrame>()
    private val previousTimestamp = ConcurrentHashMap<SensorSource, Long>()
    private val frameIntervalsMs = ConcurrentHashMap<SensorSource, Double>()
    private val _state = MutableStateFlow(FusedFrameState())
    val state: StateFlow<FusedFrameState> = _state.asStateFlow()

    fun offer(frame: SensorFrame) {
        val previous = previousTimestamp.put(frame.source, frame.timestampNs)
        if (previous != null && frame.timestampNs > previous) {
            val intervalMs = (frame.timestampNs - previous).toDouble() / 1_000_000.0
            frameIntervalsMs.merge(frame.source, intervalMs) { old, next -> old * 0.8 + next * 0.2 }
        }
        latest[frame.source] = frame
        val primary = latest[SensorSource.PHONE_CAMERA] ?: frame
        val secondary = latest.entries
            .asSequence()
            .filter { it.key != primary.source }
            .map { it.value }
            .minByOrNull { abs(it.timestampNs - primary.timestampNs) }
        val delta = secondary?.let { abs(it.timestampNs - primary.timestampNs) / 1_000_000L }
        val nowNs = nowElapsedNs()
        val health = latest.values.map { frame ->
            val ageMs = TimeUnit.NANOSECONDS.toMillis((nowNs - frame.timestampNs).coerceAtLeast(0L))
            val fps = frameIntervalsMs[frame.source]?.let { interval -> if (interval > 0.0) (1000.0 / interval).toFloat() else null }
            SensorHealth(
                source = frame.source,
                connected = ageMs < 2_000L,
                lastFrameAgeMs = ageMs,
                fps = fps,
                confidence = frame.confidence,
                latencyMs = delta
            )
        }.sortedBy { it.source.ordinal }
        _state.value = FusedFrameState(
            primary = primary,
            secondary = secondary,
            deltaMs = delta,
            synchronized = secondary != null && delta != null && delta <= maxDeltaMs,
            sourceCount = health.count { it.connected },
            health = health
        )
    }

    fun clear(source: SensorSource) {
        latest.remove(source)
        offerOrReset()
    }

    fun clearAll() {
        latest.clear()
        previousTimestamp.clear()
        frameIntervalsMs.clear()
        _state.value = FusedFrameState()
    }

    private fun offerOrReset() {
        val phone = latest[SensorSource.PHONE_CAMERA]
        if (phone == null) _state.value = FusedFrameState()
        else offer(phone)
    }
}
