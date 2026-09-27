package com.example.bodycamai

import android.graphics.Rect
import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.core.DetectionBox
import kotlin.math.roundToInt

/** Lightweight temporal smoothing for ML Kit detections. Keeps overlays stable between frames. */
class AiTrackingSmoother(private val alpha: Float = 0.55f) {
    private val previous = mutableMapOf<String, Rect>()

    fun smooth(result: AiFrameResult): AiFrameResult {
        val next = mutableMapOf<String, Rect>()
        val boxes = result.objects.mapIndexed { index, box ->
            val key = "${box.label.lowercase()}#${box.trackingId ?: index}"
            val old = previous[key]
            val b = if (old == null) box.bounds else lerp(old, box.bounds, alpha)
            next[key] = b
            box.copy(bounds = b)
        }
        previous.clear()
        previous.putAll(next)
        return result.copy(objects = boxes)
    }

    fun reset() = previous.clear()

    private fun lerp(a: Rect, b: Rect, t: Float): Rect = Rect(
        mix(a.left, b.left, t), mix(a.top, b.top, t),
        mix(a.right, b.right, t), mix(a.bottom, b.bottom, t)
    )

    private fun mix(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()
}
