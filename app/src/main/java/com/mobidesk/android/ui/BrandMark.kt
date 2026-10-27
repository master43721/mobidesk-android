package com.mobidesk.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
    ) {
        Canvas(Modifier.size(size)) {
            val scale = this.size.minDimension / 48f
            val stroke = 2.4f * scale
            val left = 9f * scale
            val top = 11f * scale
            val right = 39f * scale
            val bottom = 36f * scale
            drawRoundRect(
                color = surface,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                size = this.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(11f * scale)
            )
            drawRoundRect(
                color = outline,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale),
                style = Stroke(width = stroke)
            )
            drawLine(
                color = accent,
                start = androidx.compose.ui.geometry.Offset(left, 17f * scale),
                end = androidx.compose.ui.geometry.Offset(right, 17f * scale),
                strokeWidth = stroke
            )
            val cursor = Path().apply {
                moveTo(22f * scale, 22f * scale)
                lineTo(34f * scale, 29f * scale)
                lineTo(26f * scale, 30f * scale)
                lineTo(29f * scale, 37f * scale)
                lineTo(25.5f * scale, 38f * scale)
                lineTo(22.5f * scale, 31f * scale)
                lineTo(18f * scale, 36f * scale)
                close()
            }
            drawPath(cursor, color = Color(0xFF20BFA9))
        }
    }
}
