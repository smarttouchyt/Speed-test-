package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary

@Composable
fun HistoryChart(
    dataPoints: List<Double>,
    lineColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        if (dataPoints.size < 2) {
            Text(
                text = "Developing real-time graph...",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            return
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val maxVal = (dataPoints.maxOrNull() ?: 10.0).coerceAtLeast(10.0)
            val minVal = 0.0

            val range = maxVal - minVal
            val xStep = width / (dataPoints.size - 1)

            val path = Path()
            val fillPath = Path()

            // Map each index and data speed item to a pixel location (X, Y)
            dataPoints.forEachIndexed { index, value ->
                val x = index * xStep
                // Invert Y because canvas draws from top-left (0,0) down
                val y = height - ((value - minVal) / range * height).toFloat()

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == dataPoints.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            // Draw area gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line curve
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // Draw baseline
            drawLine(
                color = TextSecondary.copy(alpha = 0.1f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )
        }
    }
}
