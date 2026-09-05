package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantStatus
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlueSecondary
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.BorderCyanBright
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryGlow
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.OrbBlueMid
import com.example.ui.theme.OrbCyanStart
import com.example.ui.theme.OrbIndigoEnd
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.TextCyanLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animation progression lifecycle for the JARVIS Glass Pane Overlay.
 * Follows the exact choreographed steps:
 * 1. Orb smoothly appears
 * 2. Glass pane expands/slides out beside the orb
 * 3. Text appears inside pane with typewriter / smooth transition
 * 4. On exit: Text fades -> Pane contracts -> Orb disappears smoothly
 */
enum class OverlayLifecycleState {
    HIDDEN,
    ENTERING_ORB,
    EXPANDING_PANE,
    ACTIVE,
    EXITING_TEXT,
    CONTRACTING_PANE,
    EXITING_ORB
}

/**
 * Reusable JARVIS Glass Pane Overlay Component.
 *
 * Displays a small futuristic JARVIS orb near the bottom navigation area,
 * paired with a square/rounded rectangular frosted glass pane beside it.
 *
 * @param isVisible Controls overlay appearance/disappearance
 * @param currentText The latest spoken words (user speech or JARVIS response)
 * @param isJarvisResponse True if message is from JARVIS, false if from User
 * @param status Assistant status (LISTENING, THINKING, SPEAKING, etc.)
 * @param audioRms Real-time audio RMS level from microphone
 * @param onOrbClick Callback when orb is tapped
 * @param onDismiss Callback when overlay is dismissed (e.g. double-tap or close)
 * @param modifier Custom modifier
 */
@Composable
fun JarvisGlassPaneOverlay(
    isVisible: Boolean,
    currentText: String,
    isJarvisResponse: Boolean = false,
    status: AssistantStatus = AssistantStatus.LISTENING,
    audioRms: Float = 0f,
    onOrbClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var lifecycleState by remember { mutableStateOf(if (isVisible) OverlayLifecycleState.ACTIVE else OverlayLifecycleState.HIDDEN) }

    // Orchestrate multi-phase entry and exit sequences
    LaunchedEffect(isVisible) {
        if (isVisible) {
            if (lifecycleState == OverlayLifecycleState.HIDDEN) {
                lifecycleState = OverlayLifecycleState.ENTERING_ORB
                delay(180)
                lifecycleState = OverlayLifecycleState.EXPANDING_PANE
                delay(220)
                lifecycleState = OverlayLifecycleState.ACTIVE
            }
        } else {
            if (lifecycleState != OverlayLifecycleState.HIDDEN) {
                lifecycleState = OverlayLifecycleState.EXITING_TEXT
                delay(130)
                lifecycleState = OverlayLifecycleState.CONTRACTING_PANE
                delay(200)
                lifecycleState = OverlayLifecycleState.EXITING_ORB
                delay(150)
                lifecycleState = OverlayLifecycleState.HIDDEN
            }
        }
    }

    // Do not render anything if completely hidden
    if (lifecycleState == OverlayLifecycleState.HIDDEN && !isVisible) {
        return
    }

    // Animated values based on lifecycle
    val orbScale by animateFloatAsState(
        targetValue = when (lifecycleState) {
            OverlayLifecycleState.HIDDEN,
            OverlayLifecycleState.EXITING_ORB -> 0f
            OverlayLifecycleState.ENTERING_ORB -> 1.15f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "orb_scale"
    )

    val orbAlpha by animateFloatAsState(
        targetValue = when (lifecycleState) {
            OverlayLifecycleState.HIDDEN,
            OverlayLifecycleState.EXITING_ORB -> 0f
            else -> 1f
        },
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "orb_alpha"
    )

    val paneExpansionFraction by animateFloatAsState(
        targetValue = when (lifecycleState) {
            OverlayLifecycleState.EXPANDING_PANE,
            OverlayLifecycleState.ACTIVE,
            OverlayLifecycleState.EXITING_TEXT -> 1f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pane_expansion"
    )

    val paneAlpha by animateFloatAsState(
        targetValue = when (lifecycleState) {
            OverlayLifecycleState.EXPANDING_PANE,
            OverlayLifecycleState.ACTIVE,
            OverlayLifecycleState.EXITING_TEXT -> 1f
            else -> 0f
        },
        animationSpec = tween(180, easing = LinearOutSlowInEasing),
        label = "pane_alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = when (lifecycleState) {
            OverlayLifecycleState.ACTIVE -> 1f
            else -> 0f
        },
        animationSpec = tween(120, easing = LinearEasing),
        label = "text_alpha"
    )

    // Tap detector to simulate double-tap middle navigation dismiss
    var lastTapTime by remember { mutableStateOf(0L) }
    var localText by remember(currentText) { mutableStateOf(currentText) }
    var localIsJarvis by remember(isJarvisResponse) { mutableStateOf(isJarvisResponse) }

    LaunchedEffect(currentText, isJarvisResponse) {
        localText = currentText
        localIsJarvis = isJarvisResponse
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("jarvis_glass_pane_overlay_root"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Subtle dark backdrop scrim behind the overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(paneAlpha * 0.45f)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
        )

        // Main Bottom Floating Container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Interactive Demonstration Simulation Controls (visible when overlay is active)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .alpha(textAlpha)
            ) {
                // User: "Open YouTube"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC0F172A))
                        .border(0.8.dp, EnergyAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            localText = "Open YouTube"
                            localIsJarvis = false
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("demo_user_speech_button")
                ) {
                    Text(
                        text = "🗣️ User: \"Open YouTube\"",
                        fontSize = 10.sp,
                        color = EnergyAmber,
                        fontWeight = FontWeight.Medium
                    )
                }

                // JARVIS: "Okay, opening YouTube."
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC0F172A))
                        .border(0.8.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            localText = "Okay, opening YouTube."
                            localIsJarvis = true
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("demo_jarvis_response_button")
                ) {
                    Text(
                        text = "🤖 JARVIS: \"Okay, opening YouTube.\"",
                        fontSize = 10.sp,
                        color = CyanPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Row with Glass Pane on left, JARVIS Orb on right
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // 1. Frosted Glass Pane (Expands and contracts horizontally next to orb)
                if (paneExpansionFraction > 0.01f) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 210.dp, max = 290.dp)
                            .heightIn(min = 125.dp, max = 165.dp)
                            .scale(0.85f + (0.15f * paneExpansionFraction))
                            .alpha(paneAlpha)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = CyanPrimary.copy(alpha = 0.35f),
                                ambientColor = CyanPrimary.copy(alpha = 0.2f)
                            )
                            .clip(RoundedCornerShape(22.dp))
                            // Frosted Glass Multi-Layer Background
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xE60D1322), // 90% opacity deep slate
                                        Color(0xCC11192E), // 80% opacity frosted glass
                                        Color(0xD90A0E1A)  // 85% opacity dark base
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(400f, 400f)
                                )
                            )
                            // Delicate Frosted Sheen Accent
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x1F22D3EE),
                                        Color.Transparent
                                    ),
                                    center = Offset(60f, 40f),
                                    radius = 180f
                                )
                            )
                            // Thin Elegant Glowing Border
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x8022D3EE), // cyan light highlight
                                        Color(0x3338BDF8),
                                        Color(0x26475569),
                                        Color(0x6622D3EE)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(300f, 200f)
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("glass_pane_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(textAlpha),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Pane Header: Speaker Pill + Status indicator + Dismiss action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Speaker Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (localIsJarvis) Color(0x1F22D3EE) else Color(0x26FBBF24)
                                        )
                                        .border(
                                            0.8.dp,
                                            if (localIsJarvis) BorderCyan else Color(0x40FBBF24),
                                            RoundedCornerShape(999.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (localIsJarvis) CyanPrimary else EnergyAmber)
                                    )
                                    Text(
                                        text = if (localIsJarvis) "J.A.R.V.I.S." else "OPERATOR",
                                        color = if (localIsJarvis) CyanPrimary else EnergyAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )
                                }

                                // Right status / wave indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    GlassMiniEqualizer(
                                        isActive = status == AssistantStatus.LISTENING || status == AssistantStatus.SPEAKING,
                                        audioRms = audioRms
                                    )

                                    // Compact close button
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x26475569))
                                            .clickable { onDismiss() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close overlay",
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Conversation Text with Smooth Transition & Typewriter Effect
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .heightIn(min = 55.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TypewriterConversationText(
                                    text = localText.ifBlank {
                                        if (status == AssistantStatus.LISTENING) "Listening..." else "System ready."
                                    },
                                    isJarvis = localIsJarvis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Pane Footer Info: double tap hint
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (status) {
                                        AssistantStatus.LISTENING -> "LIVE VOICE INPUT"
                                        AssistantStatus.THINKING -> "ANALYZING INTENT..."
                                        AssistantStatus.SPEAKING -> "TRANSMITTING VOCAL RESPONSE"
                                        AssistantStatus.SLEEPING -> "STANDBY"
                                        else -> "READY"
                                    },
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp,
                                    color = if (status == AssistantStatus.LISTENING) ElectricTeal else TextMuted
                                )

                                Text(
                                    text = "Double-tap to close",
                                    fontSize = 8.5.sp,
                                    color = TextMuted.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                }

                // 2. Futuristic JARVIS Orb (Positioned near bottom navigation area)
                Box(
                    modifier = Modifier
                        .scale(orbScale)
                        .alpha(orbAlpha)
                        .size(62.dp)
                        .testTag("overlay_orb_ball"),
                    contentAlignment = Alignment.Center
                ) {
                    FuturisticOrbBall(
                        status = status,
                        audioRms = audioRms,
                        onClick = onOrbClick
                    )
                }
            }

            // 3. Simulated Bottom Navigation Double-Tap Dismiss Bar
            // "if I click middle naugvigation 2 time no hold then dissappear conversation ends"
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(120.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x401E293B))
                    .border(0.8.dp, Color(0x33475569), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime < 400L) {
                                    // Detected double tap on middle navigation bar!
                                    onDismiss()
                                } else {
                                    onOrbClick()
                                }
                                lastTapTime = now
                            }
                        )
                    }
                    .testTag("navigation_middle_bar"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CyanPrimary.copy(alpha = 0.7f))
                    )
                    Text(
                        text = "NAV",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Animated dynamic text component supporting real-time stream / typewriter cadence.
 */
@Composable
private fun TypewriterConversationText(
    text: String,
    isJarvis: Boolean,
    modifier: Modifier = Modifier
) {
    var displayedLength by remember(text) { mutableIntStateOf(0) }

    LaunchedEffect(text) {
        displayedLength = 0
        if (text.length <= 15) {
            displayedLength = text.length
        } else {
            // Swift progressive reveal (18ms per char)
            for (i in 1..text.length) {
                displayedLength = i
                delay(16)
            }
        }
    }

    AnimatedContent(
        targetState = text to isJarvis,
        transitionSpec = {
            (fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) +
                    slideInVertically(animationSpec = tween(180)) { height -> height / 4 })
                .togetherWith(
                    fadeOut(animationSpec = tween(120)) +
                            slideOutVertically(animationSpec = tween(120)) { -it / 4 }
                )
        },
        label = "typewriter_transition"
    ) { (targetText, jarvis) ->
        val textToShow = if (targetText == text) {
            targetText.take(displayedLength)
        } else {
            targetText
        }

        Text(
            text = textToShow,
            color = if (jarvis) TextCyanLight else TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = if (jarvis) FontWeight.Medium else FontWeight.Normal,
            lineHeight = 18.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.testTag("overlay_conversation_text")
        )
    }
}

/**
 * High-performance, compact futuristic orb canvas rendering:
 * Counter-rotating segmented arcs, glowing gradient sphere with specular highlight,
 * and audio RMS responsive pulse.
 */
@Composable
fun FuturisticOrbBall(
    status: AssistantStatus,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_ball_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arc_rotation"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ball_pulse"
    )

    val dynamicAudioScale by animateFloatAsState(
        targetValue = (1f + (audioRms.coerceIn(0f, 1f) * 0.28f)),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "audio_scale"
    )

    val primaryColor = when (status) {
        AssistantStatus.LISTENING -> ElectricTeal
        AssistantStatus.THINKING -> EnergyAmber
        AssistantStatus.SPEAKING -> CyanPrimary
        AssistantStatus.SLEEPING -> SleepPurple
        AssistantStatus.ERROR -> AlertRed
        AssistantStatus.IDLE -> CyanPrimary
    }

    Box(
        modifier = modifier
            .size(62.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) - 4.dp.toPx()
            val finalRadius = baseRadius * breathingPulse * dynamicAudioScale

            // 1. Ambient soft glow aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = finalRadius * 1.35f
                ),
                radius = finalRadius * 1.35f,
                center = center
            )

            // 2. Outer segmented glowing arc
            drawArc(
                color = primaryColor.copy(alpha = 0.75f),
                startAngle = rotationAngle,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - finalRadius, center.y - finalRadius),
                size = Size(finalRadius * 2, finalRadius * 2),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            drawArc(
                color = primaryColor.copy(alpha = 0.75f),
                startAngle = rotationAngle + 180f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - finalRadius, center.y - finalRadius),
                size = Size(finalRadius * 2, finalRadius * 2),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Counter-rotating inner dashed ring
            val innerRingRadius = finalRadius * 0.78f
            drawArc(
                color = CyanPrimaryGlow.copy(alpha = 0.45f),
                startAngle = -rotationAngle * 1.4f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius),
                size = Size(innerRingRadius * 2, innerRingRadius * 2),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Core Sphere (Cyan-600 -> Blue-500 -> Indigo-600)
            val coreSphereRadius = finalRadius * 0.58f
            val sphereBrush = when (status) {
                AssistantStatus.THINKING -> Brush.linearGradient(
                    colors = listOf(EnergyAmber, Color(0xFFEA580C)),
                    start = Offset(center.x - coreSphereRadius, center.y - coreSphereRadius),
                    end = Offset(center.x + coreSphereRadius, center.y + coreSphereRadius)
                )
                AssistantStatus.SLEEPING -> Brush.linearGradient(
                    colors = listOf(SleepPurple, Color(0xFF312E81)),
                    start = Offset(center.x - coreSphereRadius, center.y - coreSphereRadius),
                    end = Offset(center.x + coreSphereRadius, center.y + coreSphereRadius)
                )
                AssistantStatus.ERROR -> Brush.linearGradient(
                    colors = listOf(AlertRed, Color(0xFF991B1B)),
                    start = Offset(center.x - coreSphereRadius, center.y - coreSphereRadius),
                    end = Offset(center.x + coreSphereRadius, center.y + coreSphereRadius)
                )
                else -> Brush.linearGradient(
                    colors = listOf(OrbCyanStart, OrbBlueMid, OrbIndigoEnd),
                    start = Offset(center.x - coreSphereRadius, center.y - coreSphereRadius),
                    end = Offset(center.x + coreSphereRadius, center.y + coreSphereRadius)
                )
            }

            drawCircle(
                brush = sphereBrush,
                radius = coreSphereRadius,
                center = center
            )

            // 5. Specular highlight for 3D curvature
            val highlightCenter = Offset(
                center.x - coreSphereRadius * 0.35f,
                center.y - coreSphereRadius * 0.35f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = highlightCenter,
                    radius = coreSphereRadius * 0.75f
                ),
                radius = coreSphereRadius * 0.75f,
                center = highlightCenter
            )

            // Center focal spark
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = 3.dp.toPx() + (audioRms.coerceIn(0f, 1f) * 4.dp.toPx()),
                center = center
            )
        }
    }
}

/**
 * Mini 3-bar animated equalizer for the glass pane header.
 */
@Composable
private fun GlassMiniEqualizer(
    isActive: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_eq")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.height(12.dp)
    ) {
        val h1 = if (isActive) (bar1 + audioRms * 0.5f).coerceIn(0.2f, 1f) else 0.25f
        val h2 = if (isActive) (bar2 + audioRms * 0.6f).coerceIn(0.2f, 1f) else 0.25f
        val h3 = if (isActive) (bar3 + audioRms * 0.4f).coerceIn(0.2f, 1f) else 0.25f

        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height((12 * h1).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isActive) ElectricTeal else TextMuted)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height((12 * h2).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isActive) CyanPrimary else TextMuted)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height((12 * h3).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isActive) ArcBlueSecondary else TextMuted)
        )
    }
}
