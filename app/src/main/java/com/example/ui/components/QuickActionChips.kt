package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleBg

@Composable
fun QuickActionChips(
    onChipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        "🧠 Train Machine Learning Model",
        "⚡ Trigger Glass Overlay",
        "Open YouTube",
        "Open Google",
        "Search Google for Minecraft shaders",
        "What is my battery percentage?",
        "What time is it?",
        "What date is it?",
        "Open Camera",
        "Open WiFi Settings",
        "Device info",
        "Remember that I prefer dark mode",
        "JARVIS, sleep"
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickActions) { action ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(UserBubbleBg)
                    .border(0.8.dp, BorderSlate, RoundedCornerShape(14.dp))
                    .clickable { onChipSelected(action) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("quick_action_${action.take(10)}")
            ) {
                Text(
                    text = action,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

