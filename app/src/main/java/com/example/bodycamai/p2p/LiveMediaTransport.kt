package com.example.bodycamai.p2p

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Framed H.264 media layer for an already-authorized SecurePeerSession.
 * The control/consent channel remains separate; this class never grants camera access.
 */
class SecureLiveMediaTransport(private val session: SecurePeerSession) {
    private val running = AtomicBoolean(false)

    suspend fun sendFrame(ptsUs: Long, flags: Int, h264: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(running.get()) { "Медиаканал остановлен" }
            require(h264.isNotEmpty() || (flags and FLAG_END_OF_STREAM) != 0) { "Пустой видеокадр" }
            require(h264.size <= MAX_FRAME_BYTES) { "Кадр слишком большой" }
            val header = ByteBuffer.allocate(16)
                .putInt(MAGIC)
                .putInt(flags)
                .putLong(ptsUs)
                .array()
            session.send(header + h264)
        }
    }

    suspend fun sendEndOfStream(): Result<Unit> = sendFrame(0L, FLAG_END_OF_STREAM, ByteArray(0))

    fun start() { running.set(true) }
    fun stop() { running.set(false) }
    fun isRunning(): Boolean = running.get()

    companion object {
        private const val MAGIC = 0x42434149 // BCAI
        private const val MAX_FRAME_BYTES = 2 * 1024 * 1024
        const val FLAG_KEY_FRAME = 1
        const val FLAG_END_OF_STREAM = 1 shl 1

        fun decodePacket(packet: ByteArray): LiveMediaFrame {
            require(packet.size >= 16) { "Слишком короткий медиапакет" }
            val header = ByteBuffer.wrap(packet)
            require(header.int == MAGIC) { "Неверный медиапакет" }
            val flags = header.int
            val ptsUs = header.long
            return LiveMediaFrame(ptsUs, flags, packet.copyOfRange(16, packet.size))
        }
    }
}

data class LiveMediaFrame(val ptsUs: Long, val flags: Int, val h264: ByteArray)

/** H.264 decoder helper. Feed LiveMediaFrame.h264 into queueInputBuffer(). */
class H264LiveDecoder(
    width: Int,
    height: Int,
    private val surface: Surface
) : AutoCloseable {
    private val codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        codec.configure(format, surface, null, 0)
        codec.start()
    }

    fun queue(frame: LiveMediaFrame, timeoutUs: Long = 10_000L): Boolean {
        if ((frame.flags and SecureLiveMediaTransport.FLAG_END_OF_STREAM) != 0) {
            val index = codec.dequeueInputBuffer(timeoutUs)
            if (index >= 0) codec.queueInputBuffer(index, 0, 0, frame.ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return index >= 0
        }
        val index = codec.dequeueInputBuffer(timeoutUs)
        if (index < 0) return false
        val input = codec.getInputBuffer(index) ?: return false
        input.clear()
        if (frame.h264.size > input.remaining()) return false
        input.put(frame.h264)
        val flags = if ((frame.flags and SecureLiveMediaTransport.FLAG_KEY_FRAME) != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
        codec.queueInputBuffer(index, 0, frame.h264.size, frame.ptsUs, flags)
        drain(false)
        return true
    }

    private fun drain(end: Boolean) {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val index = codec.dequeueOutputBuffer(info, 0)
            if (index >= 0) {
                codec.releaseOutputBuffer(index, true)
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED || index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }
        }
    }

    override fun close() {
        runCatching { codec.stop() }
        runCatching { codec.release() }
    }
}

/**
 * Encoder configuration for CameraX/ImageAnalysis integration. The encoder is surface-ready,
 * so a future Camera2/CameraX pipeline can feed frames without changing the network protocol.
 */
class H264LiveEncoder(width: Int, height: Int, fps: Int, bitrateKbps: Int) : AutoCloseable {
    private val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val inputSurface: Surface

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfoCompat.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        codec.start()
    }

    fun drain(onFrame: (ptsUs: Long, flags: Int, bytes: ByteArray) -> Unit): Int {
        val info = MediaCodec.BufferInfo()
        var count = 0
        while (true) {
            val index = codec.dequeueOutputBuffer(info, 0)
            when {
                index >= 0 -> {
                    val buffer = codec.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) {
                        val bytes = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        buffer.get(bytes)
                        onFrame(info.presentationTimeUs, info.flags, bytes)
                        count++
                    }
                    codec.releaseOutputBuffer(index, false)
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> return count
            }
        }
    }

    override fun close() {
        runCatching { codec.signalEndOfInputStream() }
        runCatching { codec.stop() }
        runCatching { codec.release() }
        runCatching { inputSurface.release() }
    }
}

private object MediaCodecInfoCompat {
    const val COLOR_FormatSurface = 0x7F000789
}
