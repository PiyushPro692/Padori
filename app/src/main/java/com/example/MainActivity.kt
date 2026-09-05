package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AssistantStatus
import com.example.ui.JarvisUiState
import com.example.ui.JarvisViewModel
import com.example.ui.components.ActionControlBar
import com.example.ui.components.ChatHistoryView
import com.example.ui.components.JarvisGlassPaneOverlay
import com.example.ui.components.QuickActionChips
import com.example.ui.components.StatusHeader
import com.example.ui.components.VoiceOrb
import com.example.ui.components.WaveformVisualizer
import com.example.ui.dialogs.LearningDialog
import com.example.ui.dialogs.MemoryDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.TextInputDialog
import com.example.ui.dialogs.ToolsDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EnergyAmber
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JarvisScreen()
            }
        }
    }
}

@Composable
fun JarvisScreen(viewModel: JarvisViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.conversationHistory.collectAsState()
    val memories by viewModel.memoryList.collectAsState()
    val learnedInsights by viewModel.learnedInsightsList.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime Permissions (Audio recording & notifications)
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("jarvis_screen"),
        containerColor = CyberVoid,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ActionControlBar(
                status = uiState.status,
                isSleepMode = uiState.isSleepMode,
                onMicToggle = {
                    if (hasAudioPermission) {
                        viewModel.onMicPressed()
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                },
                onOpenTextInput = { viewModel.setTextInputOpen(true) },
                onOpenMemory = { viewModel.setMemoryOpen(true) },
                onOpenLearning = { viewModel.setLearningDialogOpen(true) },
                onOpenTools = { viewModel.setToolsOpen(true) },
                onToggleSleep = { viewModel.toggleSleepMode() },
                onOpenSettings = { viewModel.setSettingsOpen(true) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CyberVoid, CyberBackground, CyberSurface)
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HUD Telemetry Header
            StatusHeader(
                uiState = uiState,
                onLearningClick = { viewModel.setLearningDialogOpen(true) }
            )

            // Permission Warning Banner if mic is not granted
            if (!hasAudioPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AlertRed.copy(alpha = 0.15f))
                        .border(1.dp, AlertRed, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Microphone access required for voice operations.",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Central Holographic Core (Voice Orb & Reactive Waveform)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                VoiceOrb(
                    status = uiState.status,
                    audioRms = uiState.audioRms,
                    size = 190.dp,
                    onClick = {
                        if (hasAudioPermission) {
                            viewModel.toggleGlassOverlay()
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    }
                )
            }

            // 3. Audio Waveform Visualizer
            WaveformVisualizer(
                status = uiState.status,
                audioRms = uiState.audioRms,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // 4. Current Status or Spoken Response Text Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayText = when {
                    uiState.partialSpeech.isNotBlank() -> "\"${uiState.partialSpeech}\""
                    uiState.status == AssistantStatus.LISTENING -> "Listening for operator speech..."
                    uiState.status == AssistantStatus.THINKING -> "Analyzing intent via AI Brain..."
                    uiState.status == AssistantStatus.SLEEPING -> "JARVIS is in sleep mode. Tap mic or say 'Hey JARVIS'."
                    uiState.spokenText.isNotBlank() -> uiState.spokenText
                    else -> "Say \"JARVIS\" or tap the pulse to begin command."
                }

                Text(
                    text = displayText,
                    color = when (uiState.status) {
                        AssistantStatus.LISTENING -> ElectricTeal
                        AssistantStatus.THINKING -> EnergyAmber
                        AssistantStatus.SLEEPING -> SleepPurple
                        else -> if (uiState.spokenText.isNotBlank()) TextPrimary else TextMuted
                    },
                    fontSize = 12.5.sp,
                    fontWeight = if (uiState.status == AssistantStatus.LISTENING || uiState.status == AssistantStatus.THINKING) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    maxLines = 2
                )
            }

            // 5. Quick Action Command Chips
            QuickActionChips(
                onChipSelected = { command ->
                    when (command) {
                        "⚡ Trigger Glass Overlay" -> {
                            viewModel.setGlassOverlayVisible(true)
                            viewModel.updateOverlayText("Open YouTube", false)
                        }
                        "🧠 Train Machine Learning Model" -> {
                            viewModel.setLearningDialogOpen(true)
                            viewModel.triggerModelTrainingCycle()
                        }
                        else -> {
                            viewModel.processUserInput(command)
                        }
                    }
                }
            )

            // 6. Communication Log Area
            ChatHistoryView(
                messages = messages,
                onClearHistory = { viewModel.clearHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Dialogs
        if (uiState.isSettingsOpen) {
            SettingsDialog(
                preferences = preferences,
                onDismiss = { viewModel.setSettingsOpen(false) },
                onUpdateProvider = { viewModel.setAiProvider(it) },
                onUpdateApiKey = { viewModel.setCustomApiKey(it) },
                onUpdateRate = { viewModel.updateSpeechRate(it) },
                onUpdatePitch = { viewModel.updateSpeechPitch(it) },
                onToggleAutoSpeak = { viewModel.toggleAutoSpeak(it) },
                onToggleWakeWord = { viewModel.toggleWakeWord(it) },
                onToggleQuietHours = { viewModel.toggleQuietHours(it) }
            )
        }

        if (uiState.isMemoryOpen) {
            MemoryDialog(
                memories = memories,
                onDismiss = { viewModel.setMemoryOpen(false) },
                onAddMemory = { k, v -> viewModel.addManualMemory(k, v) },
                onDeleteMemory = { mem -> viewModel.deleteMemory(mem) },
                onClearAll = { viewModel.clearMemory() }
            )
        }

        if (uiState.isLearningDialogOpen) {
            LearningDialog(
                insights = learnedInsights,
                isTrainingActive = uiState.isTrainingActive,
                trainingStatus = uiState.trainingStatusMessage,
                fitnessScore = uiState.modelFitnessScore,
                onDismiss = { viewModel.setLearningDialogOpen(false) },
                onTriggerTraining = { viewModel.triggerModelTrainingCycle() },
                onDeleteInsight = { insight -> viewModel.deleteInsight(insight) },
                onClearAll = { viewModel.clearAllLearnedInsights() }
            )
        }

        if (uiState.isToolsOpen) {
            ToolsDialog(
                tools = (context.applicationContext as JarvisApplication).toolRegistry.getAllTools(),
                onDismiss = { viewModel.setToolsOpen(false) },
                onTestTool = { tool ->
                    viewModel.setToolsOpen(false)
                    viewModel.processUserInput("Test ${tool.definition.name}")
                }
            )
        }

        if (uiState.isTextInputOpen) {
            TextInputDialog(
                query = uiState.textInputQuery,
                onQueryChange = { viewModel.updateTextInputQuery(it) },
                onSend = { query -> viewModel.processUserInput(query) },
                onDismiss = { viewModel.setTextInputOpen(false) }
            )
        }

        // Floating JARVIS Glass Pane Overlay Interface
        JarvisGlassPaneOverlay(
            isVisible = uiState.isGlassOverlayVisible,
            currentText = if (uiState.overlayText.isNotBlank()) uiState.overlayText else uiState.spokenText,
            isJarvisResponse = uiState.isOverlayJarvisResponse,
            status = uiState.status,
            audioRms = uiState.audioRms,
            onOrbClick = {
                if (hasAudioPermission) {
                    viewModel.onMicPressed()
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            },
            onDismiss = {
                viewModel.setGlassOverlayVisible(false)
            }
        )
    }
}
