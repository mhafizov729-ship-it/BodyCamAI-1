package com.example.bodycamai.drone

/** Vendor-neutral safety gate for automatic follow modes. */
class FollowSafetyGate {
    data class Input(val gpsFresh: Boolean, val targetVisible: Boolean, val linkQualityPercent: Int?, val distanceMeters: Float?, val heightMeters: Float?, val limits: DroneSafetyLimits)
    fun allow(i: Input): Boolean {
        if (!i.gpsFresh || !i.targetVisible) return false
        if (i.limits.stopOnLinkLoss && (i.linkQualityPercent ?: 0) <= 0) return false
        if (i.distanceMeters != null && i.distanceMeters > i.limits.maxDistanceMeters) return false
        if (i.heightMeters != null && i.heightMeters > i.limits.maxHeightMeters) return false
        return true
    }
}
