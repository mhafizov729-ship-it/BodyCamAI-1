package com.example.bodycamai.performance

import android.app.ActivityManager
import android.content.Context

 data class PerformanceSnapshot(
    val memoryUsedMb: Long,
    val memoryLimitMb: Long,
    val lowMemory: Boolean
)

class PerformanceMonitor(private val context: Context) {
    fun snapshot(): PerformanceSnapshot {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val limit = runtime.maxMemory() / (1024 * 1024)
        return PerformanceSnapshot(used, limit, info.lowMemory)
    }
}
