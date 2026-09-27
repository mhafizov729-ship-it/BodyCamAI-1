package com.example.bodycamai.p2p

import android.content.Context

/** Explicit local trust list. A discovered peer is never trusted automatically. */
class PeerTrustStore(context: Context) {
    private val prefs = context.getSharedPreferences("p2p_trusted_peers", Context.MODE_PRIVATE)

    fun isTrusted(deviceId: String): Boolean = prefs.getBoolean("trusted:$deviceId", false)

    fun setTrusted(deviceId: String, trusted: Boolean) {
        prefs.edit().putBoolean("trusted:$deviceId", trusted).apply()
    }

    fun clear(deviceId: String) { prefs.edit().remove("trusted:$deviceId").apply() }
}
