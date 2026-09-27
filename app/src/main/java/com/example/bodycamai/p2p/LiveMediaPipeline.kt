package com.example.bodycamai.p2p

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX -> MediaCodec byte-buffer pipeline. This intentionally uses ImageAnalysis
 * rather than remote camera control: the local camera must already be authorized/open.
 */
class LiveCameraEncoder(
    private val width: Int,
    private val height: Int,
    private val fps: Int = 15,
    private val bitrateKbps: Int = 1200,
    private val onEncodedFrame: (Long, Int, ByteArray) -> Unit,
    private val onError: (Throwable) -> Unit = {}
) : AutoCloseable {
    private val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var frameIndex = 0L

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        running.set(true)
    }

    fun offer(image: ImageProxy) {
        if (!running.get()) { image.close(); return }
        try {
            val index = codec.dequeueInputBuffer(0)
            if (index < 0) return
            val input = codec.getInputBuffer(index) ?: return
            val yuv = ImageProxyYuv420.toI420(image)
            if (yuv.size > input.capacity()) throw IllegalStateException("Кадр YUV слишком большой")
            input.clear()
            input.put(yuv)
            val ptsUs = image.imageInfo.timestamp / 1000L
            codec.queueInputBuffer(index, 0, yuv.size, ptsUs, 0)
            drain()
            frameIndex++
        } catch (t: Throwable) {
            onError(t)
        } finally {
            image.close()
        }
    }

    private fun drain() {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val index = codec.dequeueOutputBuffer(info, 0)
            when {
                index >= 0 -> {
                    codec.getOutputBuffer(index)?.let { buffer ->
                        if (info.size > 0) {
                            val bytes = ByteArray(info.size)
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            buffer.get(bytes)
                            var flags = 0
                            if ((info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) flags = flags or SecureLiveMediaTransport.FLAG_KEY_FRAME
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) flags = flags or SecureLiveMediaTransport.FLAG_END_OF_STREAM
                            onEncodedFrame(info.presentationTimeUs, flags, bytes)
                        }
                    }
                    codec.releaseOutputBuffer(index, false)
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> return
            }
        }
    }

    override fun close() {
        if (!running.getAndSet(false)) return
        runCatching { codec.signalEndOfInputStream() }
        runCatching { drain() }
        runCatching { codec.stop() }
        runCatching { codec.release() }
        scope.cancel()
    }
}

/** I420 conversion handles CameraX row/pixel strides without assuming tightly packed planes. */
object ImageProxyYuv420 {
    fun toI420(image: ImageProxy): ByteArray {
        val w = image.width
        val h = image.height
        val out = ByteArray(w * h + (w / 2) * (h / 2) * 2)
        copyPlane(image.planes[0].buffer, image.planes[0].rowStride, image.planes[0].pixelStride, w, h, out, 0)
        val chromaSize = (w / 2) * (h / 2)
        copyPlane(image.planes[1].buffer, image.planes[1].rowStride, image.planes[1].pixelStride, w / 2, h / 2, out, w * h)
        copyPlane(image.planes[2].buffer, image.planes[2].rowStride, image.planes[2].pixelStride, w / 2, h / 2, out, w * h + chromaSize)
        return out
    }

    private fun copyPlane(buffer: ByteBuffer, rowStride: Int, pixelStride: Int, width: Int, height: Int, out: ByteArray, offset: Int) {
        val start = buffer.position()
        var dst = offset
        for (y in 0 until height) {
            val row = start + y * rowStride
            for (x in 0 until width) {
                val pos = row + x * pixelStride
                if (pos < buffer.limit()) out[dst++] = buffer.get(pos)
            }
        }
    }
}

/** Sends encoded frames asynchronously so camera analysis is never blocked by network IO. */
class LiveMediaSender(private val transport: SecureLiveMediaTransport) : AutoCloseable {
    private data class Packet(val ptsUs: Long, val flags: Int, val bytes: ByteArray)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val closed = AtomicBoolean(false)
    private val queue = Channel<Packet>(
        capacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var worker: Job? = null

    fun start() {
        if (closed.get()) return
        transport.start()
        if (worker?.isActive != true) {
            worker = scope.launch {
                for (packet in queue) {
                    transport.sendFrame(packet.ptsUs, packet.flags, packet.bytes)
                        .onFailure { /* connection state is reported by the session layer */ }
                }
            }
        }
    }

    fun send(ptsUs: Long, flags: Int, bytes: ByteArray) {
        if (closed.get() || !transport.isRunning()) return
        queue.trySend(Packet(ptsUs, flags, bytes.copyOf()))
    }

    fun stop() {
        if (!closed.compareAndSet(false, true)) return
        queue.close()
        runCatching { transport.stop() }
        scope.cancel()
        worker = null
    }

    override fun close() = stop()
}

/** Receiver loop for a dedicated media session. UI decides where the decoder Surface lives. */
class LiveMediaReceiver(
    private val session: SecurePeerSession,
    private val onFrame: (LiveMediaFrame) -> Unit,
    private val onClosed: () -> Unit = {}
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val closed = AtomicBoolean(false)

    fun start() {
        scope.launch {
            try {
                while (!closed.get()) onFrame(SecureLiveMediaTransport.decodePacket(session.receive()))
            } catch (_: Throwable) {
                if (closed.compareAndSet(false, true)) onClosed()
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { session.close() }
        scope.cancel()
        onClosed()
    }
}

/** Process-wide bridge used by the camera composable; the bridge is inert until a consented session is attached. */
object LiveStreamBridge : AutoCloseable {
    @Volatile private var encoder: LiveCameraEncoder? = null
    @Volatile private var sender: LiveMediaSender? = null

    @Synchronized
    fun attach(session: SecurePeerSession, width: Int, height: Int, fps: Int = 15, bitrateKbps: Int = 1200, onError: (Throwable) -> Unit = {}) {
        close()
        val transport = SecureLiveMediaTransport(session)
        val mediaSender = LiveMediaSender(transport)
        val mediaEncoder = LiveCameraEncoder(width, height, fps, bitrateKbps,
            onEncodedFrame = { pts, flags, bytes -> mediaSender.send(pts, flags, bytes) },
            onError = onError
        )
        sender = mediaSender
        encoder = mediaEncoder
        mediaSender.start()
    }

    fun isAttached(): Boolean = encoder != null

    fun offer(image: ImageProxy) {
        encoder?.offer(image) ?: Unit
    }

    @Synchronized
    override fun close() {
        runCatching { sender?.stop() }
        runCatching { encoder?.close() }
        sender = null
        encoder = null
    }
}
