package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.FloatingTriggerBall
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ScrollCaptureViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScrollCaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ScrollCaptureViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                    AppScreen.SCROLL_STUDIO -> ScrollCaptureStudioScreen(viewModel = viewModel)
                    AppScreen.IMAGE_STITCHER -> ImageStitcherScreen(viewModel = viewModel)
                    AppScreen.SCREENSHOT_EDITOR -> ScreenshotEditorScreen(viewModel = viewModel)
                    AppScreen.GALLERY_DETAIL -> GalleryDetailScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }

            // Floating screenshot trigger ball visible on Home screen
            if (currentScreen == AppScreen.HOME) {
                FloatingTriggerBall(
                    onClick = {
                        viewModel.resetScrollStudio()
                        viewModel.navigateTo(AppScreen.SCROLL_STUDIO)
                        viewModel.startAutoScrollCapture()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 90.dp)
                )
            }
        }
    }
}
