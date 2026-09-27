package com.example.bodycamai.p2p

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicReference

/** Connection state machine with bounded LAN reconnect backoff. */
class P2PConnectionManager(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) : AutoCloseable {
    private val state = AtomicReference(PeerLinkState.OFFLINE)
    private var retryJob: Job? = null
    private var session: SecurePeerSession? = null

    fun state(): PeerLinkState = state.get()

    fun connect(endpoint: PeerEndpoint, roomCode: String, maxRetries: Int = 4, onConnected: () -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        retryJob?.cancel()
        retryJob = scope.launch {
            var delayMs = 500L
            repeat(maxRetries.coerceIn(1, 6)) { attempt ->
                if (!isActive) return@launch
                state.set(PeerLinkState.CONNECTING)
                val result = SecureP2PClient.connect(endpoint.host, endpoint.port, roomCode)
                if (result.isSuccess) {
                    session = result.getOrNull()
                    state.set(PeerLinkState.CONNECTED)
                    onConnected()
                    return@launch
                }
                state.set(PeerLinkState.BLOCKED_BY_NETWORK)
                onError(result.exceptionOrNull() ?: IllegalStateException("P2P connection failed"))
                if (attempt + 1 < maxRetries) { delay(delayMs); delayMs = (delayMs * 2).coerceAtMost(4000L) }
            }
            state.set(PeerLinkState.OFFLINE)
        }
    }

    fun currentSession(): SecurePeerSession? = session

    fun closeSession() {
        runCatching { session?.close() }
        session = null
        state.set(PeerLinkState.OFFLINE)
        retryJob?.cancel()
        retryJob = null
    }

    override fun close() { closeSession(); scope.coroutineContext.cancel() }
}
