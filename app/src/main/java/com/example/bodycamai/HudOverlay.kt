package com.example.bodycamai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.core.AiOverlayMode
import kotlin.math.max
import com.example.bodycamai.core.FusionState
import com.example.bodycamai.core.FusedFrameState
import com.example.bodycamai.core.VisionMode
import com.example.bodycamai.core.WorldMarker
import com.example.bodycamai.sensors.DeviceTelemetry

private val HudCyan = Color(0xFF00E5FF)
private val HudGreen = Color(0xFF63FF8B)
private val HudAmber = Color(0xFFFFC857)

@Composable
fun HudOverlay(
    recording: Boolean,
    aiEnabled: Boolean,
    result: AiFrameResult,
    profileName: String,
    telemetry: DeviceTelemetry,
    fusion: FusionState,
    fusedFrameState: FusedFrameState,
    aiOverlayMode: AiOverlayMode,
    alert: AiAlert? = null
) {
    Box(Modifier.fillMaxSize()) {
        alert?.let { a ->
            HudPill("${a.title}  •  ${a.detail}", if (a.level == AlertLevel.WARNING) Color.Red else HudAmber, true, Modifier.align(Alignment.TopCenter).padding(top = 96.dp))
        }
        Column(
            Modifier.align(Alignment.TopStart).padding(top = 54.dp, start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HudPill(if (recording) "● REC" else "● READY", if (recording) Color.Red else HudGreen, true)
            HudPill("$profileName • ${fusion.visionMode.title.uppercase()}", HudCyan, true)
            HudPill(if (aiEnabled) "AI ON • TRACK ${if (fusion.trackingEnabled) "ON" else "OFF"}" else "AI OFF", if (aiEnabled) HudCyan else Color.LightGray)
            HudPill("SRC ${fusion.connectedCount} • FUSION ${if (fusedFrameState.synchronized) "SYNC" else "STANDBY"}", if (fusedFrameState.synchronized) HudGreen else Color.White)
            HudPill("CONF ${"%.0f".format(fusion.fusionConfidence * 100)}%", if (fusion.fusionConfidence >= .7f) HudGreen else HudAmber)
        }

        Column(
            Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HudPill(if (telemetry.gpsReady) "GPS" else "GPS —", if (telemetry.gpsReady) HudGreen else HudAmber)
            HudPill(if (telemetry.compassReady) "HDG ${telemetry.headingText}" else "HDG —", Color.White)
            telemetry.speedKmh?.let { HudPill("${"%.0f".format(it)} KM/H", Color.White) }
            fusedFrameState.deltaMs?.let { HudPill("Δ ${it}ms", if (fusedFrameState.synchronized) HudGreen else HudAmber) }
        }

        if (fusion.minimapEnabled) {
            MiniMap(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), telemetry, fusion)
        }

        if (aiEnabled && fusion.worldMarkersEnabled) {
            WorldMarkerLayer(result, aiOverlayMode)
            result.objects.filter { obj ->
                when (aiOverlayMode) {
                    AiOverlayMode.ALL -> true
                    AiOverlayMode.PEOPLE -> obj.label.lowercase() in setOf("person", "человек", "люди")
                    AiOverlayMode.FACES -> false
                    AiOverlayMode.VEHICLES -> obj.label.lowercase() in setOf("car", "vehicle", "автомобиль", "машина", "truck", "bus", "motorcycle")
                    AiOverlayMode.TEXT -> false
                    AiOverlayMode.HIGH_CONFIDENCE -> obj.confidence >= 0.70f
                }
            }.take(6).forEachIndexed { index, obj ->
                val top = 150 + index * 52
                Box(Modifier.fillMaxWidth().padding(start = 18.dp, end = 150.dp, top = top.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HudPill("${obj.label.uppercase()} ${"%.0f".format(obj.confidence * 100)}%", HudGreen)
                        obj.trackingId?.let { HudPill("#${it}", HudCyan) }
                    }
                }
            }
        }

        if (fusion.visionMode == VisionMode.FUSED && !fusion.thermalAvailable) {
            HudPill(
                "ТЕПЛО: внешний сенсор не подключён",
                HudAmber,
                modifier = Modifier.align(Alignment.Center).padding(12.dp)
            )
        }

        SurfaceInfo(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 96.dp),
            objects = result.objects.size,
            faces = result.faceCount,
            ocr = result.text.isNotBlank(),
            fusion = fusion
        )
    }
}

@Composable
private fun WorldMarkerLayer(result: AiFrameResult, mode: AiOverlayMode) {
    if (result.frameWidth <= 0 || result.frameHeight <= 0) return
    val markers = result.objects.filter { box ->
        when (mode) {
            AiOverlayMode.ALL -> true
            AiOverlayMode.PEOPLE -> box.label.lowercase() in setOf("person", "человек", "люди")
            AiOverlayMode.FACES, AiOverlayMode.TEXT -> false
            AiOverlayMode.VEHICLES -> box.label.lowercase() in setOf("car", "vehicle", "автомобиль", "машина", "truck", "bus", "motorcycle")
            AiOverlayMode.HIGH_CONFIDENCE -> box.confidence >= 0.70f
        }
    }.take(10)
    Canvas(Modifier.fillMaxSize()) {
        val sx = size.width / result.frameWidth.toFloat()
        val sy = size.height / result.frameHeight.toFloat()
        markers.forEach { box ->
            val r = box.bounds
            val left = r.left * sx
            val top = r.top * sy
            val right = r.right * sx
            val bottom = r.bottom * sy
            val stroke = 2f
            drawRect(Color(0xFF63FF8B), androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Size(right-left, bottom-top), style=androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawLine(Color(0xFF00E5FF), androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left+18, top), strokeWidth=stroke)
            drawLine(Color(0xFF00E5FF), androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top+18), strokeWidth=stroke)
        }
    }
}

@Composable
private fun SurfaceInfo(modifier: Modifier, objects: Int, faces: Int, ocr: Boolean, fusion: FusionState) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        HudPill("OBJ $objects")
        HudPill("FACE $faces")
        HudPill(if (ocr) "OCR" else "OCR —")
        HudPill("SENS ${fusion.connectedCount}")
    }
}

@Composable
private fun MiniMap(modifier: Modifier, telemetry: DeviceTelemetry, fusion: FusionState) {
    Box(
        modifier.size(154.dp)
            .background(Color.Black.copy(alpha = .78f), RoundedCornerShape(14.dp))
            .border(1.dp, HudCyan.copy(alpha = .45f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("MINI MAP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("N", color = Color.White, fontSize = 10.sp)
            Text("▲", color = HudCyan, modifier = Modifier.rotate(-(telemetry.headingDegrees ?: 0f)), fontSize = 30.sp)
            Text(if (telemetry.gpsReady) telemetry.locationText else "GPS SEARCH", color = HudGreen, fontSize = 9.sp)
            Text(if (fusion.externalVideoAvailable) "REMOTE SOURCE" else "LOCAL", color = Color.LightGray, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HudPill(text: String, color: Color = Color.White, bold: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        modifier = modifier
            .background(Color.Black.copy(alpha = .72f), RoundedCornerShape(7.dp))
            .border(1.dp, color.copy(alpha = .25f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    )
}
