package com.example.ui.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisGlassPaneOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Transparent Floating Overlay Activity for JARVIS.
 * Triggered from outside the app (e.g. Assist gesture, home/navigation shortcut).
 * Appears seamlessly above whatever the user is currently doing on the phone.
 */
class JarvisOverlayActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                var isVisible by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    viewModel.onMicPressed()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    JarvisGlassPaneOverlay(
                        isVisible = isVisible,
                        currentText = if (uiState.overlayText.isNotBlank()) uiState.overlayText else uiState.spokenText,
                        isJarvisResponse = uiState.isOverlayJarvisResponse,
                        status = uiState.status,
                        audioRms = uiState.audioRms,
                        onOrbClick = {
                            viewModel.onMicPressed()
                        },
                        onDismiss = {
                            isVisible = false
                            lifecycleScope.launch {
                                // Wait for smooth contract and fade animation to finish
                                delay(450)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}
