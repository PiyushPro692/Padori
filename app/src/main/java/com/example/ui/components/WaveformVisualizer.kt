package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantStatus
import com.example.ui.theme.ArcBlueSecondary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryGlow
import com.example.ui.theme.ElectricTeal

@Composable
fun WaveformVisualizer(
    status: AssistantStatus,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 19
    val transition = rememberInfiniteTransition(label = "waveform_anim")

    // Dynamic wave animation offsets
    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    Row(
        modifier = modifier.height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isActive = status == AssistantStatus.LISTENING || status == AssistantStatus.SPEAKING

        for (i in 0 until barCount) {
            // Distance from center (symmetric waveform)
            val distFromCenter = Math.abs(i - (barCount / 2)) / (barCount / 2f)
            val bellCurve = (1f - distFromCenter * 0.7f).coerceIn(0.2f, 1f)

            val baseHeight = if (isActive) {
                val jitter = if (i % 2 == 0) waveOffset * 0.35f else (1f - waveOffset) * 0.35f
                val rmsFactor = (audioRms * 1.6f).coerceIn(0.15f, 1f)
                (bellCurve * rmsFactor + jitter).coerceIn(0.12f, 1f)
            } else {
                0.12f
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(baseHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CyanPrimaryGlow,
                                CyanPrimary,
                                if (isActive) ElectricTeal else ArcBlueSecondary.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }
    }
}
