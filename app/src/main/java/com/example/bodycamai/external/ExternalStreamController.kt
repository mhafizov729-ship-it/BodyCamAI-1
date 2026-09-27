package com.example.bodycamai.external

/**
 * Runtime state machine for network sources. A URL being valid is not equivalent to a live stream.
 * A media engine should call onFrame() only after decoding an actual video frame.
 */
class ExternalStreamController(
    private val descriptor: ExternalVideoSource,
    private val maxFrameAgeMs: Long = 1500L
) {
    data class Runtime(
        val source: ExternalVideoSource,
        val running: Boolean = false,
        val framesReceived: Long = 0,
        val lastFrameTimestampMs: Long? = null,
        val healthy: Boolean = false,
        val error: String? = null
    )

    private var runtime = Runtime(descriptor)
    private var frameCount = 0L

    fun start(): Result<Runtime> {
        runtime = runtime.copy(running = true, error = null)
        return Result.success(runtime)
    }

    fun onFrame(timestampMs: Long): Runtime {
        frameCount++
        runtime = runtime.copy(
            running = true,
            framesReceived = frameCount,
            lastFrameTimestampMs = timestampMs,
            healthy = true,
            error = null,
            source = descriptor.copy(state = StreamState.STREAMING)
        )
        return runtime
    }

    fun heartbeat(nowMs: Long): Runtime {
        val last = runtime.lastFrameTimestampMs
        val healthy = runtime.running && last != null && nowMs - last <= maxFrameAgeMs
        runtime = runtime.copy(
            healthy = healthy,
            source = descriptor.copy(state = if (healthy) StreamState.STREAMING else StreamState.STALE)
        )
        return runtime
    }

    fun fail(message: String): Runtime {
        runtime = runtime.copy(
            running = false,
            healthy = false,
            error = message,
            source = descriptor.copy(state = StreamState.ERROR)
        )
        return runtime
    }

    fun stop(): Runtime {
        runtime = runtime.copy(
            running = false,
            healthy = false,
            source = descriptor.copy(state = StreamState.DISCONNECTED)
        )
        return runtime
    }
}
