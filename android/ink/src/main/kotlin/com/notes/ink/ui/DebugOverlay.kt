package com.notes.ink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DebugOverlay(
    strokeCount: Int,
    frameTimeMs: Float,
    pageRevision: Long,
    scale: Float,
    panX: Float,
    panY: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xCC1E1E1E), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = "PERF / DEBUG OVERLAY",
                color = Color(0xFFFFB74D),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Strokes: $strokeCount | Rev: $pageRevision",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Frame Time: ${"%.1f".format(frameTimeMs)} ms",
                color = if (frameTimeMs > 16.6f) Color(0xFFEF5350) else Color(0xFF81C784),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Zoom: ${"%.2f".format(scale)}x | Pan: (${panX.toInt()}, ${panY.toInt()})",
                color = Color(0xFFB0BEC5),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
