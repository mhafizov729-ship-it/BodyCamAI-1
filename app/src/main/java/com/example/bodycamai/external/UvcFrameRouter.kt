package com.example.bodycamai.external

import android.graphics.BitmapFactory
import com.example.bodycamai.OfflineAiEngine
import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.sensors.adapters.UvcFallbackAdapter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Bridges decoded UVC JPEG frames from a real USB/UVC adapter into the offline AI pipeline.
 * The adapter remains responsible for USB/UVC transport and must provide actual frame bytes.
 */
class UvcFrameRouter(
    private val source: ExternalVideoSource,
    private val ai: OfflineAiEngine,
    private val minAiIntervalMs: Long = 100L
) {
    private val running = AtomicBoolean(false)
    private val frameCount = AtomicLong(0L)
    @Volatile private var lastAiMs = 0L
    @Volatile private var lastFrameNs: Long? = null

    var onSourceUpdated: ((ExternalVideoSource) -> Unit)? = null
    var onAiResult: ((AiFrameResult) -> Unit)? = null
    var onFrame: ((width: Int, height: Int, timestampNs: Long) -> Unit)? = null

    fun attach(adapter: UvcFallbackAdapter): Result<Unit> {
        if (!adapter.connected) return Result.failure(IllegalStateException("UVC device is not connected"))
        if (!running.compareAndSet(false, true)) return Result.success(Unit)

        adapter.frameConsumer { timestampNs, data ->
            if (!running.get()) return@frameConsumer
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return@frameConsumer
            val nowMs = System.currentTimeMillis()
            lastFrameNs = timestampNs
            frameCount.incrementAndGet()
            onFrame?.invoke(bitmap.width, bitmap.height, timestampNs)
            onSourceUpdated?.invoke(
                source.copy(
                    state = StreamState.STREAMING,
                    width = bitmap.width,
                    height = bitmap.height,
                    fps = null,
                    lastFrameTimestampNs = timestampNs,
                    error = null
                )
            )

            if (nowMs - lastAiMs >= minAiIntervalMs) {
                lastAiMs = nowMs
                ai.analyze(bitmap, 0) { result -> onAiResult?.invoke(result) }
            }
            bitmap.recycle()
        }
        return Result.success(Unit)
    }

    fun heartbeat(nowNs: Long): Boolean {
        val last = lastFrameNs ?: return false
        return running.get() && (nowNs - last) <= 1_500_000_000L
    }

    fun stop() {
        running.set(false)
    }

    fun framesReceived(): Long = frameCount.get()
}
