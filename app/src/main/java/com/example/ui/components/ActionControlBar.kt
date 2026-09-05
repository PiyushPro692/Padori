package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantStatus
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.DockBg
import com.example.ui.theme.DockBorder
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ActionControlBar(
    status: AssistantStatus,
    isSleepMode: Boolean,
    onMicToggle: () -> Unit,
    onOpenTextInput: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenLearning: () -> Unit = {},
    onOpenTools: () -> Unit,
    onToggleSleep: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = status == AssistantStatus.LISTENING
    val transition = rememberInfiniteTransition(label = "mic_pulse")

    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Professional Polish Dock Container: rounded-3xl bg-slate-900/60 border-slate-800/50
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(DockBg)
                .border(1.dp, DockBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left actions: Input, Memory, ML Learning
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                DockActionItem(
                    icon = Icons.Default.Keyboard,
                    label = "INPUT",
                    contentDescription = "Text Input",
                    testTag = "keyboard_button",
                    onClick = onOpenTextInput
                )
                DockActionItem(
                    icon = Icons.Default.Memory,
                    label = "MEMORY",
                    contentDescription = "Memory Bank",
                    testTag = "memory_button",
                    onClick = onOpenMemory
                )
                DockActionItem(
                    icon = Icons.Default.ModelTraining,
                    label = "LEARN",
                    contentDescription = "Machine Learning",
                    testTag = "learning_button",
                    tint = CyanPrimary,
                    onClick = onOpenLearning
                )
            }

            // Spacing clearance for elevated mic
            Spacer(modifier = Modifier.size(54.dp))

            // Right actions: Tools, Sleep, Config
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                DockActionItem(
                    icon = Icons.Default.Build,
                    label = "TOOLS",
                    contentDescription = "Tools Registry",
                    testTag = "tools_button",
                    onClick = onOpenTools
                )
                DockActionItem(
                    icon = Icons.Default.Bedtime,
                    label = "SLEEP",
                    contentDescription = "Sleep Mode",
                    testTag = "sleep_button",
                    tint = if (isSleepMode) SleepPurple else TextSecondary,
                    onClick = onToggleSleep
                )
                DockActionItem(
                    icon = Icons.Default.Settings,
                    label = "CONFIG",
                    contentDescription = "Settings",
                    testTag = "settings_button",
                    onClick = onOpenSettings
                )
            }
        }

        // Central Elevated Mic Button: -mt-8 w-16 h-16 rounded-full bg-white text-[#0A0B0E]
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(72.dp)
        ) {
            // Ripple glow when listening
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(ElectricTeal.copy(alpha = 0.3f))
                        .border(1.5.dp, ElectricTeal.copy(alpha = 0.7f), CircleShape)
                )
            }

            // Main Push-To-Talk Button
            val micButtonBg = when {
                isListening -> ElectricTeal
                isSleepMode -> SleepPurple
                else -> Color.White
            }
            val micIconTint = when {
                isListening || isSleepMode -> Color.White
                else -> CyberVoid
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(micButtonBg)
                    .border(
                        width = 1.5.dp,
                        color = if (isListening) Color.White else DockBorder,
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMicToggle
                    )
                    .testTag("mic_toggle_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else if (isSleepMode) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Push To Talk",
                    tint = micIconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun DockActionItem(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    testTag: String,
    tint: Color = TextSecondary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (tint != TextSecondary) tint else TextMuted
        )
    }
}
