package com.example.bodycamai.sync

import com.example.bodycamai.core.SensorFrame
import com.example.bodycamai.core.SensorSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Runtime coordinator for simultaneous real camera/sensor sources. */
class MultiCameraFusionSession(
    private val synchronizer: MultiSourceSynchronizer = MultiSourceSynchronizer()
) {
    data class State(
        val activeSources: Set<SensorSource> = emptySet(),
        val synchronizedSources: Set<SensorSource> = emptySet(),
        val referenceTimestampNs: Long? = null,
        val maxSkewMs: Long? = null,
        val ready: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    @Synchronized
    fun submit(frame: SensorFrame) {
        val snapshot = synchronizer.offer(frame)
        val skewMs = snapshot.maxSkewNs / 1_000_000L
        _state.value = State(
            activeSources = snapshot.frames.keys,
            synchronizedSources = if (snapshot.synchronized) snapshot.frames.keys else emptySet(),
            referenceTimestampNs = snapshot.referenceTimestampNs,
            maxSkewMs = skewMs,
            ready = snapshot.synchronized
        )
    }

    fun clear(source: SensorSource) {
        synchronizer.clear(source)
        refresh()
    }

    fun clearAll() {
        synchronizer.clearAll()
        _state.value = State()
    }

    @Synchronized
    private fun refresh() {
        val snapshot = synchronizer.snapshot()
        _state.value = State(
            activeSources = snapshot.frames.keys,
            synchronizedSources = if (snapshot.synchronized) snapshot.frames.keys else emptySet(),
            referenceTimestampNs = snapshot.referenceTimestampNs.takeIf { it != 0L },
            maxSkewMs = snapshot.maxSkewNs / 1_000_000L,
            ready = snapshot.synchronized
        )
    }
}
