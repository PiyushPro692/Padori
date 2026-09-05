package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantStatus
import com.example.ui.JarvisUiState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.BorderCyanBright
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CyanBgSubtle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleBg

@Composable
fun StatusHeader(
    uiState: JarvisUiState,
    modifier: Modifier = Modifier,
    onLearningClick: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "status_dot_pulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val statusColor = when (uiState.status) {
        AssistantStatus.LISTENING -> ElectricTeal
        AssistantStatus.THINKING -> EnergyAmber
        AssistantStatus.SPEAKING -> CyanPrimary
        AssistantStatus.SLEEPING -> SleepPurple
        AssistantStatus.ERROR -> AlertRed
        AssistantStatus.IDLE -> CyanPrimary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top row: Sleek Header inspired by Professional Polish HTML
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SYSTEM ACTIVE",
                    color = CyanPrimary.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "JARVIS",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.5).sp
                )
            }

            // Status Pill badge: px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyanBgSubtle)
                    .border(1.dp, BorderCyan, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = dotAlpha))
                )
                Text(
                    text = uiState.statusMessage.uppercase(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }

        // Second row: HUD Telemetry chips (refined slate styling)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            HudTelemetryChip(
                icon = Icons.Default.Psychology,
                label = uiState.currentProvider,
                modifier = Modifier.weight(1f)
            )

            HudTelemetryChip(
                icon = Icons.Default.BatteryChargingFull,
                label = "${uiState.batteryPercentage}% PWR",
                modifier = Modifier.weight(1f)
            )

            HudTelemetryChip(
                icon = Icons.Default.Memory,
                label = "${uiState.memoryCount} MEM",
                modifier = Modifier.weight(1f)
            )

            HudTelemetryChip(
                icon = Icons.Default.ModelTraining,
                label = "${uiState.learningInsightsCount} ML",
                highlight = true,
                modifier = Modifier.weight(1f),
                onClick = onLearningClick
            )
        }
    }
}

@Composable
fun HudTelemetryChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(UserBubbleBg)
            .border(
                0.8.dp,
                if (highlight) BorderCyanBright else BorderSlate,
                RoundedCornerShape(8.dp)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlight) CyanPrimary else TextSecondary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (highlight) CyanPrimary else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

