package com.example.bodycamai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.sensors.DeviceTelemetry

@Composable
fun HudOverlay(
    recording: Boolean,
    aiEnabled: Boolean,
    mapEnabled: Boolean,
    result: AiFrameResult,
    profileName: String,
    telemetry: DeviceTelemetry
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.TopStart).padding(top = 54.dp, start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HudText(if (recording) "● REC" else "STANDBY", if (recording) Color.Red else Color.White)
            HudText(if (aiEnabled) "AI • LOCAL" else "AI • OFF", Color(0xFF00E5FF))
            HudText(if (telemetry.gpsReady) "GPS • ${telemetry.locationText}" else "GPS • SEARCHING")
            HudText(if (telemetry.compassReady) "🧭 ${telemetry.headingText}" else "🧭 —")
            telemetry.speedKmh?.let { HudText("SPEED • ${"%.1f".format(it)} km/h") }
            HudText("PROFILE • $profileName")
        }

        if (mapEnabled) MiniMap(Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 14.dp), telemetry)

        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HudText("OBJ ${result.objects.size}")
            HudText("FACE ${result.faceCount}")
            HudText(if (result.text.isBlank()) "OCR —" else "OCR ✓")
        }

        if (aiEnabled) {
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                result.objects.take(4).forEach { obj ->
                    HudText("${obj.label} ${(obj.confidence * 100).toInt()}%", Color(0xFF00FF88))
                }
                if (result.faceCount > 0) HudText("👤 НЕ ЦЕЛЬ", Color(0xFF00FF88))
            }
        }
    }
}

@Composable
private fun HudText(text: String, color: Color = Color.White) {
    Text(
        text = text,
        color = color,
        modifier = Modifier
            .background(Color.Black.copy(alpha = .68f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun MiniMap(modifier: Modifier, telemetry: DeviceTelemetry) {
    Box(modifier.size(130.dp).background(Color.Black.copy(alpha = .72f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("N", color = Color.White)
            Text("▲", color = Color(0xFF00E5FF), modifier = Modifier.rotate(-(telemetry.headingDegrees ?: 0f)))
            Text(if (telemetry.gpsReady) "GPS" else "GPS —", color = Color(0xFF00E5FF))
            Text(telemetry.headingText, color = Color.White)
        }
    }
}
