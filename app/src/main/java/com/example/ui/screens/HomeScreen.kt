package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel

/**
 * HomeScreen delegating to MainAppScaffold containing LibraryScreen, NowPlayingScreen, and EqualizerScreen.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    MainAppScaffold(
        viewModel = viewModel,
        modifier = modifier
    )
}
