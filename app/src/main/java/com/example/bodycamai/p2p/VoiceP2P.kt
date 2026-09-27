package com.example.bodycamai.p2p

/** Control-plane model for a consent-based peer voice channel. */
enum class VoiceState { IDLE, REQUESTED, ACCEPTED, CONNECTING, ACTIVE, REJECTED, ENDED, UNAVAILABLE }

data class VoiceRequest(
    val id: String,
    val requesterId: String,
    val targetDeviceId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class VoiceResponse(
    val requestId: String,
    val accepted: Boolean,
    val state: VoiceState,
    val reason: String = ""
)

interface VoiceTransport {
    suspend fun open(request: VoiceRequest): Result<Unit>
    suspend fun close(requestId: String)
}
