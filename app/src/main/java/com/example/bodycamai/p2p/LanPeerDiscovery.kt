package com.example.bodycamai.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * LAN discovery only. It does not claim internet/NAT traversal support.
 * Services are resolved to concrete host/port pairs before a connection attempt.
 */
class LanPeerDiscovery(private val context: Context) {
    companion object { const val SERVICE_TYPE = "_bodycamai._tcp." }

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = mutableSetOf<Job>()

    fun advertise(name: String, port: Int, onError: (Throwable) -> Unit = {}) {
        stopAdvertise()
        val info = NsdServiceInfo().apply {
            serviceName = name.take(60)
            serviceType = SERVICE_TYPE
            this.port = port
        }
        registration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = onError(IllegalStateException("NSD registration failed: $errorCode"))
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration)
    }

    fun discover(onPeer: (PeerEndpoint) -> Unit, onError: (Throwable) -> Unit = {}) {
        stopDiscovery()
        discovery = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = onError(IllegalStateException("NSD discovery failed: $errorCode"))
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                jobs += scope.launch {
                    resolve(serviceInfo)?.let(onPeer)
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
        }
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery)
    }

    private suspend fun resolve(info: NsdServiceInfo): PeerEndpoint? = suspendCancellableCoroutine { cont ->
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                if (cont.isActive) cont.resume(null)
            }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                if (cont.isActive) {
                    cont.resume(PeerEndpoint(
                        host = serviceInfo.host?.hostAddress ?: return,
                        port = serviceInfo.port,
                        channel = com.example.bodycamai.core.ConnectionChannel.WIFI_LOCAL,
                        advertisedName = serviceInfo.serviceName
                    ))
                }
            }
        }
        nsd.resolveService(info, listener)
        cont.invokeOnCancellation { /* NSD resolver has no reliable cancellation API on all API levels. */ }
    }

    fun stopAdvertise() { registration?.let { runCatching { nsd.unregisterService(it) } }; registration = null }
    fun stopDiscovery() { discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }; discovery = null; jobs.forEach { it.cancel() }; jobs.clear() }
    fun close() { stopAdvertise(); stopDiscovery() }
}
