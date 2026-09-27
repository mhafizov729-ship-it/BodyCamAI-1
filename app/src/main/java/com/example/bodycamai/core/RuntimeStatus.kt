package com.example.bodycamai.core

/** Local-only runtime status. Network/server values are unknown until a real check is performed. */
data class RuntimeStatus(
    val aiConnected: Boolean = false,
    val aiLatencyMs: Long? = null,
    val aiError: String? = null,
    val requestCount: Long = 0,
    val contextUsed: Int = 0,
    val contextLimit: Int = 0,
    val protectionEnabled: Boolean = true,
    val networkAvailable: Boolean = false
) {
    val contextRemaining: Int?
        get() = if (contextLimit > 0) (contextLimit - contextUsed).coerceAtLeast(0) else null
}
