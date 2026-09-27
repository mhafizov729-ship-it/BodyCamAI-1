package com.example.bodycamai.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.bodycamai.core.ConnectionChannel
import com.example.bodycamai.core.ConnectionState
import com.example.bodycamai.core.chooseBestConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkChannelMonitor(context: Context) {
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refresh()
        override fun onLost(network: Network) = refresh()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refresh()
    }

    init {
        connectivity.registerDefaultNetworkCallback(callback)
        refresh()
    }

    fun refresh() {
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val channels = buildSet {
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add(ConnectionChannel.MOBILE)
            if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) add(ConnectionChannel.INTERNET)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add(ConnectionChannel.WIFI_LOCAL)
        }.ifEmpty { setOf(ConnectionChannel.OFFLINE) }
        _state.value = ConnectionState(active = chooseBestConnection(channels), available = channels)
    }

    fun close() = connectivity.unregisterNetworkCallback(callback)
}
