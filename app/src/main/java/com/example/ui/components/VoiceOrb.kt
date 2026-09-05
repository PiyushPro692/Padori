package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantStatus
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlueSecondary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryGlow
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.OrbBlueMid
import com.example.ui.theme.OrbCyanStart
import com.example.ui.theme.OrbIndigoEnd
import com.example.ui.theme.SleepPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    status: AssistantStatus,
    audioRms: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "orb_transition")

    // Continuous clockwise rotation
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.THINKING -> 2200
                    AssistantStatus.LISTENING -> 4000
                    AssistantStatus.SPEAKING -> 3500
                    AssistantStatus.SLEEPING -> 12000
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Counter-clockwise rotation for secondary ring
    val counterRotationAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.THINKING -> 1800
                    AssistantStatus.LISTENING -> 5000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Subtle breathing pulse for core
    val breathingScale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == AssistantStatus.SLEEPING) 3000 else 1600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Color coordination based on status
    val primaryColor = when (status) {
        AssistantStatus.LISTENING -> ElectricTeal
        AssistantStatus.THINKING -> EnergyAmber
        AssistantStatus.SPEAKING -> CyanPrimary
        AssistantStatus.SLEEPING -> SleepPurple
        AssistantStatus.ERROR -> AlertRed
        AssistantStatus.IDLE -> CyanPrimary
    }

    val glowColor = when (status) {
        AssistantStatus.LISTENING -> ElectricTeal.copy(alpha = 0.5f)
        AssistantStatus.THINKING -> EnergyAmber.copy(alpha = 0.5f)
        AssistantStatus.SPEAKING -> CyanPrimaryGlow.copy(alpha = 0.6f)
        AssistantStatus.SLEEPING -> SleepPurple.copy(alpha = 0.25f)
        AssistantStatus.ERROR -> AlertRed.copy(alpha = 0.5f)
        AssistantStatus.IDLE -> ArcBlueSecondary.copy(alpha = 0.35f)
    }

    // Audio reactivity boost: amplifies when listening or speaking
    val audioBoost = if (status == AssistantStatus.LISTENING || status == AssistantStatus.SPEAKING) {
        audioRms.coerceIn(0f, 1f) * 0.35f
    } else {
        0f
    }

    val dynamicScale = breathingScale + audioBoost

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("voice_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f

            // 1. Outermost Glowing Field
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = baseRadius * 0.98f
                ),
                radius = baseRadius * 0.98f,
                center = center
            )

            // 2. Outer segmented ring (Counter-clockwise)
            rotate(counterRotationAngle, pivot = center) {
                val outerRadius = baseRadius * 0.90f
                val strokeWidth = 2.5.dp.toPx()
                val arcSize = Size(outerRadius * 2, outerRadius * 2)
                val topLeft = Offset(center.x - outerRadius, center.y - outerRadius)

                // 4 segmented arcs
                for (i in 0 until 4) {
                    val startAngle = i * 90f + 12f
                    drawArc(
                        color = primaryColor.copy(alpha = 0.6f),
                        startAngle = startAngle,
                        sweepAngle = 66f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 8 small tick marks on outer track
                for (i in 0 until 8) {
                    val angleRad = Math.toRadians((i * 45f).toDouble())
                    val p1 = Offset(
                        (center.x + (outerRadius - 6.dp.toPx()) * cos(angleRad)).toFloat(),
                        (center.y + (outerRadius - 6.dp.toPx()) * sin(angleRad)).toFloat()
                    )
                    val p2 = Offset(
                        (center.x + (outerRadius + 2.dp.toPx()) * cos(angleRad)).toFloat(),
                        (center.y + (outerRadius + 2.dp.toPx()) * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // 3. Middle mechanical ring (Clockwise)
            rotate(rotationAngle, pivot = center) {
                val midRadius = baseRadius * 0.72f * dynamicScale
                val midArcSize = Size(midRadius * 2, midRadius * 2)
                val midTopLeft = Offset(center.x - midRadius, center.y - midRadius)

                // 3 sweeping tech arcs
                for (i in 0 until 3) {
                    val startAngle = i * 120f + 15f
                    drawArc(
                        color = primaryColor,
                        startAngle = startAngle,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = midTopLeft,
                        size = midArcSize,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Orbiting satellite dots
                for (i in 0 until 3) {
                    val angleRad = Math.toRadians((i * 120f + 5f).toDouble())
                    val dotCenter = Offset(
                        (center.x + midRadius * cos(angleRad)).toFloat(),
                        (center.y + midRadius * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = dotCenter
                    )
                }
            }

            // 4. Inner Cybernetic Core Sphere (Reacts to Audio RMS)
            val coreRadius = baseRadius * 0.44f * dynamicScale

            // Multi-stop gradient sphere fill: cyan-600 -> blue-500 -> indigo-600
            val sphereBrush = when (status) {
                AssistantStatus.THINKING -> Brush.linearGradient(
                    colors = listOf(EnergyAmber, Color(0xFFEA580C)),
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                )
                AssistantStatus.SLEEPING -> Brush.linearGradient(
                    colors = listOf(SleepPurple, Color(0xFF312E81)),
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                )
                AssistantStatus.ERROR -> Brush.linearGradient(
                    colors = listOf(AlertRed, Color(0xFF991B1B)),
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                )
                else -> Brush.linearGradient(
                    colors = listOf(OrbCyanStart, OrbBlueMid, OrbIndigoEnd),
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                )
            }

            // Core sphere body
            drawCircle(
                brush = sphereBrush,
                radius = coreRadius,
                center = center
            )

            // Specular radial highlight at 30% offset
            val highlightCenter = Offset(
                center.x - coreRadius * 0.35f,
                center.y - coreRadius * 0.35f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = highlightCenter,
                    radius = coreRadius * 0.75f
                ),
                radius = coreRadius * 0.75f,
                center = highlightCenter
            )

            // Outer sphere delicate rim
            drawCircle(
                color = primaryColor.copy(alpha = 0.7f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Center radiant spark
            drawCircle(
                color = Color.White.copy(alpha = if (status == AssistantStatus.SLEEPING) 0.3f else 0.95f),
                radius = (5.dp.toPx() + (audioBoost * 10f)).coerceAtMost(14.dp.toPx()),
                center = center
            )
        }
    }
}
