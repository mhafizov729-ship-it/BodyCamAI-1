package com.example.bodycamai.sync

import com.example.bodycamai.core.SensorFrame
import com.example.bodycamai.core.SensorSource
import kotlin.math.abs

/**
 * Builds a timestamp-coherent snapshot from phone, USB, thermal and remote sources.
 * It never invents frames: a source is present only when a real frame was submitted.
 */
class MultiSourceSynchronizer(private val maxSkewNs: Long = 120_000_000L) {
    data class Snapshot(
        val referenceTimestampNs: Long,
        val frames: Map<SensorSource, SensorFrame>,
        val maxSkewNs: Long,
        val synchronized: Boolean
    )

    private val latest = linkedMapOf<SensorSource, SensorFrame>()

    @Synchronized
    fun offer(frame: SensorFrame): Snapshot {
        latest[frame.source] = frame
        return snapshot(frame.timestampNs)
    }

    @Synchronized
    fun snapshot(referenceTimestampNs: Long? = null): Snapshot {
        val reference = referenceTimestampNs ?: latest.values.maxOfOrNull { it.timestampNs } ?: 0L
        val selected = latest.filterValues { abs(it.timestampNs - reference) <= maxSkewNs }
        val skew = if (selected.isEmpty()) 0L else selected.values.maxOf { abs(it.timestampNs - reference) }
        return Snapshot(
            referenceTimestampNs = reference,
            frames = selected.toMap(),
            maxSkewNs = skew,
            synchronized = selected.size >= 2 && skew <= maxSkewNs
        )
    }

    @Synchronized
    fun clear(source: SensorSource) { latest.remove(source) }

    @Synchronized
    fun clearAll() { latest.clear() }
}
