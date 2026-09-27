package com.example.bodycamai.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Lightweight stereo audio analyzer.
 * Direction is deliberately approximate: it reports left/right/center tendency,
 * not an exact bearing or source localization.
 */
class SpatialAudioAnalyzer(
    private val sampleRate: Int = 16_000,
    private val onResult: (SpatialAudioResult) -> Unit
) {
    private var record: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    fun start(): Boolean {
        if (running) return true
        val min = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (min <= 0) return false
        return runCatching {
            val r = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(min * 2, 4096)
            )
            if (r.state != AudioRecord.STATE_INITIALIZED) {
                r.release()
                return false
            }
            record = r
            running = true
            r.startRecording()
            worker = Thread({ loop(max(min, 2048)) }, "bodycam-spatial-audio")
                .also { it.start() }
            true
        }.getOrDefault(false)
    }

    private fun loop(bufferSize: Int) {
        val buffer = ShortArray(bufferSize + bufferSize % 2)
        while (running) {
            val n = record?.read(buffer, 0, buffer.size) ?: -1
            if (n <= 1) continue
            val pairs = n / 2
            var leftEnergy = 0.0
            var rightEnergy = 0.0
            var cross = 0.0
            for (i in 0 until pairs) {
                val l = buffer[i * 2].toDouble()
                val r = buffer[i * 2 + 1].toDouble()
                leftEnergy += l * l
                rightEnergy += r * r
                cross += l * r
            }
            val left = sqrt(leftEnergy / pairs).toFloat()
            val right = sqrt(rightEnergy / pairs).toFloat()
            val total = max(left, right)
            val balance = if (total > 1f) ((right - left) / total).coerceIn(-1f, 1f) else 0f
            val correlation = if (left > 1f && right > 1f) {
                (cross / sqrt(leftEnergy * rightEnergy)).toFloat().coerceIn(-1f, 1f)
            } else 0f
            val direction = when {
                total < 900f -> AudioDirection.NONE
                balance < -0.18f -> AudioDirection.LEFT
                balance > 0.18f -> AudioDirection.RIGHT
                else -> AudioDirection.CENTER
            }
            onResult(SpatialAudioResult(left, right, balance, correlation, direction))
        }
    }

    fun stop() {
        running = false
        runCatching { record?.stop() }
        worker?.join(250)
        worker = null
        record?.release()
        record = null
    }
}

enum class AudioDirection { NONE, LEFT, CENTER, RIGHT }

data class SpatialAudioResult(
    val leftRms: Float,
    val rightRms: Float,
    val stereoBalance: Float,
    val correlation: Float,
    val direction: AudioDirection
) {
    val confidence: Float
        get() = (abs(stereoBalance) * 0.7f + (1f - abs(correlation)) * 0.3f)
            .coerceIn(0f, 1f)
}
