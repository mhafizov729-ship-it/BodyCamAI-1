package com.example.bodycamai.p2p

import org.json.JSONObject
import java.util.UUID

/** Negotiation layer for consent-based peer camera viewing. Actual media transport is capability-based. */
enum class LiveViewState { IDLE, REQUESTED, ACCEPTED, REJECTED, CONNECTING, LIVE, ENDED, UNAVAILABLE }

data class LiveViewRequest(
    val id: String = UUID.randomUUID().toString(),
    val requesterId: String,
    val sourceDeviceId: String,
    val audioRequested: Boolean = false,
    val maxWidth: Int = 1280,
    val maxFps: Int = 30,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveViewResponse(
    val requestId: String,
    val accepted: Boolean,
    val state: LiveViewState,
    val sourceDeviceId: String,
    val reason: String = ""
)

data class LiveViewCapabilities(
    val camera: Boolean,
    val microphone: Boolean,
    val maxWidth: Int,
    val maxFps: Int,
    val audioSupported: Boolean
)

object LiveViewCodec {
    fun encodeRequest(value: LiveViewRequest): ByteArray = JSONObject()
        .put("type", "live_request").put("id", value.id).put("requesterId", value.requesterId)
        .put("sourceDeviceId", value.sourceDeviceId).put("audio", value.audioRequested)
        .put("maxWidth", value.maxWidth).put("maxFps", value.maxFps).put("timestamp", value.timestamp)
        .toString().toByteArray(Charsets.UTF_8)

    fun encodeResponse(value: LiveViewResponse): ByteArray = JSONObject()
        .put("type", "live_response").put("requestId", value.requestId).put("accepted", value.accepted)
        .put("state", value.state.name).put("sourceDeviceId", value.sourceDeviceId).put("reason", value.reason)
        .toString().toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): Any {
        val o = JSONObject(String(bytes, Charsets.UTF_8))
        return when (o.getString("type")) {
            "live_request" -> LiveViewRequest(
                id = o.getString("id"), requesterId = o.getString("requesterId"),
                sourceDeviceId = o.getString("sourceDeviceId"), audioRequested = o.optBoolean("audio"),
                maxWidth = o.optInt("maxWidth", 1280), maxFps = o.optInt("maxFps", 30), timestamp = o.optLong("timestamp")
            )
            "live_response" -> LiveViewResponse(
                requestId = o.getString("requestId"), accepted = o.optBoolean("accepted"),
                state = LiveViewState.valueOf(o.optString("state", "UNAVAILABLE")),
                sourceDeviceId = o.getString("sourceDeviceId"), reason = o.optString("reason")
            )
            else -> error("Неизвестный Live View пакет")
        }
    }
}

/** Keeps media negotiation separate from the P2P control channel so a future media engine can be swapped in. */
interface LiveViewTransport {
    suspend fun open(request: LiveViewRequest): Result<Unit>
    suspend fun close(requestId: String)
}


/** Local state machine for consent and transport lifecycle. It never activates a remote camera by itself. */
class LiveViewController {
    var state: LiveViewState = LiveViewState.IDLE
        private set
    var activeRequest: LiveViewRequest? = null
        private set

    fun request(request: LiveViewRequest): LiveViewState {
        activeRequest = request
        state = LiveViewState.REQUESTED
        return state
    }

    fun applyResponse(response: LiveViewResponse): LiveViewState {
        if (activeRequest?.id != response.requestId) return state
        state = response.state
        if (state == LiveViewState.REJECTED || state == LiveViewState.ENDED || state == LiveViewState.UNAVAILABLE) {
            activeRequest = null
        }
        return state
    }

    fun beginTransport(): LiveViewState {
        if (state == LiveViewState.ACCEPTED) state = LiveViewState.CONNECTING
        return state
    }

    fun markLive(): LiveViewState {
        if (state == LiveViewState.CONNECTING) state = LiveViewState.LIVE
        return state
    }

    fun stop(): LiveViewState {
        state = LiveViewState.ENDED
        activeRequest = null
        return state
    }

    fun reset(): LiveViewState {
        state = LiveViewState.IDLE
        activeRequest = null
        return state
    }
}

/** Conservative quality profile used by a future real media transport. */
data class LiveQuality(val width: Int, val fps: Int, val bitrateKbps: Int)

object LiveQualityPolicy {
    fun choose(networkPercent: Int, thermalHot: Boolean = false, batteryPercent: Int = 100): LiveQuality = when {
        thermalHot || batteryPercent < 15 -> LiveQuality(640, 15, 700)
        networkPercent < 25 -> LiveQuality(640, 15, 600)
        networkPercent < 50 -> LiveQuality(854, 24, 1200)
        networkPercent < 75 -> LiveQuality(1280, 30, 2200)
        else -> LiveQuality(1920, 30, 4000)
    }
}
