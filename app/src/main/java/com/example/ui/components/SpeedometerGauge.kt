package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TestState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    currentSpeed: Double,
    maxSpeed: Double = 100.0,
    state: TestState,
    modifier: Modifier = Modifier
) {
    // Smooth, bouncy spring response for physical analog feel
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SpeedDial"
    )

    // Determine color based on active test state
    val activeColor = when (state) {
        TestState.PINGING -> NeonAmber
        TestState.DOWNLOADING -> NeonCyan
        TestState.UPLOADING -> NeonViolet
        TestState.COMPLETED -> NeonCyan
        else -> NeonCyan.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2 - 20f
            val innerRadius = outerRadius - 15f
            
            // Draw background gauge arc (240 degree coverage, from 150 to 390 degrees)
            val startAngle = 150f
            val sweepAngle = 240f
            
            drawArc(
                color = TextSecondary.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )

            // Draw active speed arc accent with a beautiful futuristic gradient
            val currentSweep = (animatedSpeed / maxSpeed.toFloat()).coerceIn(0f, 1f) * sweepAngle
            if (currentSweep > 0.1f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.3f),
                            activeColor
                        ),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )
            }

            // Draw clean tick divisions for speedometer realism
            val totalTicks = 20
            for (i in 0..totalTicks) {
                val tickAngleDeg = startAngle + (sweepAngle / totalTicks) * i
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())
                
                // Outer point
                val startX = center.x + outerRadius * cos(tickAngleRad).toFloat()
                val startY = center.y + outerRadius * sin(tickAngleRad).toFloat()
                
                // End point (longer ticks for major marks)
                val length = if (i % 5 == 0) 30f else 15f
                val endX = center.x + (outerRadius - length) * cos(tickAngleRad).toFloat()
                val endY = center.y + (outerRadius - length) * sin(tickAngleRad).toFloat()
                
                // Tick coloring
                val tickColor = if (tickAngleDeg <= startAngle + currentSweep) {
                    activeColor
                } else {
                    TextSecondary.copy(alpha = 0.3f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 5 == 0) 4f else 2f
                )
            }

            // Draw Glowing digital needle tip
            val needleAngleDeg = startAngle + currentSweep
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = outerRadius - 35f
            val needleX = center.x + needleLength * cos(needleAngleRad).toFloat()
            val needleY = center.y + needleLength * sin(needleAngleRad).toFloat()

            // Outer pointer circle
            drawCircle(
                color = activeColor,
                radius = 6f,
                center = Offset(needleX, needleY)
            )

            // Center subtle dial anchor
            drawCircle(
                color = activeColor,
                radius = 12f,
                center = center
            )
            drawCircle(
                color = CyberBlack,
                radius = 6f,
                center = center
            )
        }

        // Inside display layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (state) {
                    TestState.IDLE -> "READY"
                    TestState.PINGING -> "PINGING..."
                    TestState.DOWNLOADING -> "DOWNLOADING"
                    TestState.UPLOADING -> "UPLOADING"
                    TestState.COMPLETED -> "FINISHED"
                    TestState.ERROR -> "ERROR"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = String.format(java.util.Locale.US, "%.1f", currentSpeed),
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Mbps",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        }
    }
}
