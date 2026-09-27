package com.example.bodycamai.sensors.adapters

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Transport-neutral endpoint for a real remote camera/drone feed.
 * Connecting an endpoint only records configuration; a concrete decoder/SDK must
 * call markStreaming() after receiving real frames.
 */
interface RemoteVideoSource {
    val state: StateFlow<State>
    fun connect(endpoint: String): Result<Unit>
    fun markStreaming(width: Int, height: Int, fps: Int, latencyMs: Int? = null)
    fun reportError(message: String)
    fun disconnect()

    data class State(
        val connected: Boolean = false,
        val endpoint: String? = null,
        val latencyMs: Int? = null,
        val width: Int? = null,
        val height: Int? = null,
        val fps: Int? = null,
        val error: String? = null
    )
}

class ConfigurableRemoteVideoSource : RemoteVideoSource {
    private val _state = MutableStateFlow(RemoteVideoSource.State())
    override val state: StateFlow<RemoteVideoSource.State> = _state.asStateFlow()

    override fun connect(endpoint: String): Result<Unit> {
        val value = endpoint.trim()
        if (value.isBlank()) return Result.failure(IllegalArgumentException("Endpoint is empty"))
        _state.value = RemoteVideoSource.State(endpoint = value)
        return Result.success(Unit)
    }

    override fun markStreaming(width: Int, height: Int, fps: Int, latencyMs: Int?) {
        val endpoint = _state.value.endpoint ?: return
        if (width <= 0 || height <= 0 || fps <= 0) return
        _state.value = _state.value.copy(
            connected = true, width = width, height = height, fps = fps,
            latencyMs = latencyMs, error = null, endpoint = endpoint
        )
    }

    override fun reportError(message: String) {
        _state.value = _state.value.copy(connected = false, error = message)
    }

    override fun disconnect() { _state.value = RemoteVideoSource.State() }
}
