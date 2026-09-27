package com.example.bodycamai.sync

import kotlin.math.abs

/** Timestamp-based pairing for independently arriving camera/sensor frames. */
class FrameSynchronizer(private val maxSkewNs: Long = 50_000_000L) {
    data class Frame<T>(val timestampNs: Long, val payload: T)
    data class Pair<A, B>(val first: Frame<A>, val second: Frame<B>, val skewNs: Long)

    fun <A, B> pair(first: Frame<A>, second: Frame<B>): Pair<A, B>? {
        val skew = abs(first.timestampNs - second.timestampNs)
        return if (skew <= maxSkewNs) Pair(first, second, skew) else null
    }
}
