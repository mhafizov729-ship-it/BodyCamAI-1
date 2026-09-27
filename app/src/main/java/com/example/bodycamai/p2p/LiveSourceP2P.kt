package com.example.bodycamai.p2p

import org.json.JSONObject
import java.util.UUID

enum class LiveSourceType { PHONE_CAMERA, DRONE_CAMERA, EXTERNAL_CAMERA }
enum class LiveSourceState { AVAILABLE, REQUESTED, ACCEPTED, REJECTED, CONNECTING, LIVE, STOPPED, UNAVAILABLE }

data class LiveSource(
    val id: String = UUID.randomUUID().toString(),
    val ownerDeviceId: String,
    val name: String,
    val type: LiveSourceType,
    val state: LiveSourceState = LiveSourceState.AVAILABLE,
    val audioSupported: Boolean = false,
    val controlSupported: Boolean = false
)

data class LiveSourceRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val requesterId: String,
    val sourceId: String,
    val wantAudio: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveSourceResponse(
    val requestId: String,
    val accepted: Boolean,
    val sourceId: String,
    val state: LiveSourceState,
    val reason: String = ""
)

object LiveSourceCodec {
    fun encodeRequest(v: LiveSourceRequest) = JSONObject()
        .put("type", "source_request").put("requestId", v.requestId)
        .put("requesterId", v.requesterId).put("sourceId", v.sourceId)
        .put("wantAudio", v.wantAudio).put("timestamp", v.timestamp)
        .toString().toByteArray(Charsets.UTF_8)

    fun encodeResponse(v: LiveSourceResponse) = JSONObject()
        .put("type", "source_response").put("requestId", v.requestId)
        .put("accepted", v.accepted).put("sourceId", v.sourceId)
        .put("state", v.state.name).put("reason", v.reason)
        .toString().toByteArray(Charsets.UTF_8)
}

/** Available sources are switched explicitly; stopping always returns to no stream/phone camera as chosen by UI. */
class LiveSourceController {
    private var active: LiveSource? = null
    fun switchTo(source: LiveSource): LiveSource {
        active = source.copy(state = LiveSourceState.LIVE)
        return active!!
    }
    fun stop(): LiveSource? {
        val old = active
        active = null
        return old?.copy(state = LiveSourceState.STOPPED)
    }
    fun activeSource(): LiveSource? = active
}
