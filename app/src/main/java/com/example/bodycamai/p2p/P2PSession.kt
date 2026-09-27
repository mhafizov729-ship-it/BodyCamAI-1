package com.example.bodycamai.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.example.bodycamai.core.ConnectionChannel
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local-network discovery. It does not contact a central server. */
class LocalPeerDiscovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_bodycamai._tcp."
    private var registration: NsdManager.RegistrationListener? = null

    fun advertise(group: P2PGroup, port: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = "BodyCam-${group.roomCode}"
            serviceType = serviceType
            this.port = port
            setAttribute("group", group.groupId)
            setAttribute("host", group.hostDeviceId)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registration = listener
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stop() {
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
    }
}

class P2PHost {
    private var server: ServerSocket? = null
    val deviceId: String = UUID.randomUUID().toString()

    suspend fun start(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = ServerSocket(0)
            server = socket
            socket.localPort
        }
    }

    fun stop() { runCatching { server?.close() }; server = null }
}

class DirectPeerTransport : PeerTransport {
    private var socket: Socket? = null

    override suspend fun connect(endpoint: PeerEndpoint): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            socket = Socket(endpoint.host, endpoint.port)
        }
    }

    override suspend fun send(payload: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val output = socket?.getOutputStream() ?: error("Нет P2P-соединения")
            output.write(payload)
            output.flush()
            Unit
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) { runCatching { socket?.close() } }
        socket = null
    }
}


/** Encrypted transport using the room-code-bound ECDH session. */
class SecureDirectPeerTransport(private val roomCode: String) : PeerTransport {
    private var session: SecurePeerSession? = null

    override suspend fun connect(endpoint: PeerEndpoint): Result<Unit> =
        SecureP2PClient.connect(endpoint.host, endpoint.port, roomCode).map { session = it }

    override suspend fun send(payload: ByteArray): Result<Unit> = runCatching {
        session?.send(payload) ?: error("Нет защищённого P2P-соединения")
    }

    override suspend fun close() {
        runCatching { session?.close() }
        session = null
    }
}
