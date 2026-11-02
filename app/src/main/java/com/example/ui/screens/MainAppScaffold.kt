package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.PlayerViewModel
import com.example.ui.components.MiniPlayer
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class AppNavScreen(val title: String) {
    LIBRARY("Library"),
    NOW_PLAYING("Now Playing"),
    EQUALIZER("Equalizer")
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(AppNavScreen.LIBRARY) }

    // Android System BackHandler: returns to Library screen if not already on Library
    BackHandler(enabled = currentScreen != AppNavScreen.LIBRARY) {
        currentScreen = AppNavScreen.LIBRARY
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianDark,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Persistent Mini Player above the navigation bar (only when on Library or Equalizer)
                if (currentScreen != AppNavScreen.NOW_PLAYING && uiState.currentTrack != null) {
                    MiniPlayer(
                        playbackState = viewModel.musicController.playbackState.collectAsState().value,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onSkipNextClick = { viewModel.skipToNext() },
                        onOpenFullPlayer = { currentScreen = AppNavScreen.NOW_PLAYING },
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Modern Material 3 Bottom Navigation Bar
                NavigationBar(
                    containerColor = SurfaceCard,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("app_navigation_bar")
                ) {
                    // 1. Library Tab
                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.LIBRARY,
                        onClick = { currentScreen = AppNavScreen.LIBRARY },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppNavScreen.LIBRARY)
                                    Icons.Filled.LibraryMusic
                                else
                                    Icons.Outlined.LibraryMusic,
                                contentDescription = "Library"
                            )
                        },
                        label = {
                            Text(
                                text = "Library",
                                fontWeight = if (currentScreen == AppNavScreen.LIBRARY) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = ElectricPurple
                        ),
                        modifier = Modifier.testTag("nav_item_library")
                    )

                    // 2. Now Playing Tab
                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.NOW_PLAYING,
                        onClick = { currentScreen = AppNavScreen.NOW_PLAYING },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppNavScreen.NOW_PLAYING)
                                    Icons.Filled.PlayCircle
                                else
                                    Icons.Outlined.PlayCircle,
                                contentDescription = "Now Playing"
                            )
                        },
                        label = {
                            Text(
                                text = "Now Playing",
                                fontWeight = if (currentScreen == AppNavScreen.NOW_PLAYING) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = ElectricPurple
                        ),
                        modifier = Modifier.testTag("nav_item_now_playing")
                    )

                    // 3. Equalizer Tab
                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.EQUALIZER,
                        onClick = { currentScreen = AppNavScreen.EQUALIZER },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppNavScreen.EQUALIZER)
                                    Icons.Filled.Tune
                                else
                                    Icons.Outlined.Tune,
                                contentDescription = "Equalizer"
                            )
                        },
                        label = {
                            Text(
                                text = "Equalizer",
                                fontWeight = if (currentScreen == AppNavScreen.EQUALIZER) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = ElectricPurple
                        ),
                        modifier = Modifier.testTag("nav_item_equalizer")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_navigation_transition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppNavScreen.LIBRARY -> {
                        LibraryScreen(
                            viewModel = viewModel,
                            onNavigateToEqualizer = { currentScreen = AppNavScreen.EQUALIZER }
                        )
                    }
                    AppNavScreen.NOW_PLAYING -> {
                        NowPlayingScreen(
                            viewModel = viewModel,
                            onNavigateToEqualizer = { currentScreen = AppNavScreen.EQUALIZER }
                        )
                    }
                    AppNavScreen.EQUALIZER -> {
                        EqualizerScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
