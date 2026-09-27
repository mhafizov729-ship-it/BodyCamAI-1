package com.example.bodycamai.p2p

/** Simple policy layer: preserve connectivity by lowering media quality on weak links. */
enum class MediaQuality { LOW, MEDIUM, HIGH }

data class LinkMetrics(val kbps: Int, val packetLossPercent: Int = 0, val latencyMs: Int = 0)

data class MediaProfile(val quality: MediaQuality, val maxWidth: Int, val maxFps: Int, val audioBitrateKbps: Int)

object AdaptiveMediaPolicy {
    fun choose(metrics: LinkMetrics): MediaProfile = when {
        metrics.kbps < 350 || metrics.packetLossPercent >= 12 -> MediaProfile(MediaQuality.LOW, 480, 12, 16)
        metrics.kbps < 1200 || metrics.packetLossPercent >= 6 || metrics.latencyMs >= 300 -> MediaProfile(MediaQuality.MEDIUM, 720, 20, 24)
        else -> MediaProfile(MediaQuality.HIGH, 1280, 30, 32)
    }
}
