package com.example.bodycamai.p2p

import com.example.bodycamai.core.ConnectionChannel

/** Serverless group: one device is the authoritative host for the current session. */
enum class PeerRole { HOST, CLIENT }

enum class PeerLinkState { DISCOVERING, CONNECTING, CONNECTED, BLOCKED_BY_NETWORK, OFFLINE }

data class PeerEndpoint(
    val host: String,
    val port: Int,
    val channel: ConnectionChannel,
    val advertisedName: String
)

data class P2PGroup(
    val groupId: String,
    val roomCode: String,
    val hostDeviceId: String,
    val hostName: String,
    val role: PeerRole,
    val linkState: PeerLinkState = PeerLinkState.DISCOVERING,
    val endpoint: PeerEndpoint? = null
)

/** No central backend: the host owns the session state and peers exchange it directly. */
interface PeerTransport {
    suspend fun connect(endpoint: PeerEndpoint): Result<Unit>
    suspend fun send(payload: ByteArray): Result<Unit>
    suspend fun close()
}
