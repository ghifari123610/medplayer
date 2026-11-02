package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LocalAudioRepository
import com.example.ui.AudioSourceTab
import com.example.ui.MainViewModel
import com.example.ui.components.TrackItem
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredTracks = uiState.filteredTracks
    val localTracks = uiState.localTracks

    var isSearchVisible by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onAudioPermissionResult(isGranted)
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
    ) {
        // Library Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(ElectricPurple, NeonCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Music Library",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${filteredTracks.size} lagu tersedia",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonCyan
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isSearchVisible = !isSearchVisible },
                    modifier = Modifier.testTag("library_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search library",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier.testTag("library_equalizer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Open Equalizer",
                        tint = NeonCyan
                    )
                }
            }
        }

        // Search Bar (if opened)
        if (isSearchVisible) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("library_search_field"),
                placeholder = { Text("Cari judul, artis, atau album...", color = TextMuted) },
                singleLine = true,
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextMuted
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Source Tab Row (Semua / Lokal / Streaming)
        TabRow(
            selectedTabIndex = uiState.selectedSourceTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedSourceTab.ordinal]),
                    color = NeonCyan,
                    height = 2.5.dp
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Tab(
                selected = uiState.selectedSourceTab == AudioSourceTab.ALL,
                onClick = { viewModel.selectSourceTab(AudioSourceTab.ALL) },
                text = {
                    Text(
                        "Semua",
                        fontWeight = if (uiState.selectedSourceTab == AudioSourceTab.ALL) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.selectedSourceTab == AudioSourceTab.ALL) NeonCyan else TextSecondary
                    )
                }
            )
            Tab(
                selected = uiState.selectedSourceTab == AudioSourceTab.LOCAL_DEVICE,
                onClick = {
                    viewModel.selectSourceTab(AudioSourceTab.LOCAL_DEVICE)
                    if (!uiState.hasAudioPermission) {
                        audioPermissionLauncher.launch(LocalAudioRepository.getRequiredAudioPermission())
                    }
                },
                text = {
                    Text(
                        "Lagu Lokal (${localTracks.size})",
                        fontWeight = if (uiState.selectedSourceTab == AudioSourceTab.LOCAL_DEVICE) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.selectedSourceTab == AudioSourceTab.LOCAL_DEVICE) NeonCyan else TextSecondary
                    )
                }
            )
            Tab(
                selected = uiState.selectedSourceTab == AudioSourceTab.CURATED_STREAM,
                onClick = { viewModel.selectSourceTab(AudioSourceTab.CURATED_STREAM) },
                text = {
                    Text(
                        "Streaming",
                        fontWeight = if (uiState.selectedSourceTab == AudioSourceTab.CURATED_STREAM) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.selectedSourceTab == AudioSourceTab.CURATED_STREAM) NeonCyan else TextSecondary
                    )
                }
            )
        }

        // Main Track List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Local Audio Permission Card if needed
            if (uiState.selectedSourceTab == AudioSourceTab.LOCAL_DEVICE && !uiState.hasAudioPermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan))
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Izin Penyimpanan Diperlukan",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val permName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                "READ_MEDIA_AUDIO"
                            } else {
                                "READ_EXTERNAL_STORAGE"
                            }

                            Text(
                                text = "Aplikasi membutuhkan izin $permName untuk mengekstrak lagu lokal dari penyimpanan perangkat.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    audioPermissionLauncher.launch(LocalAudioRepository.getRequiredAudioPermission())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Izinkan Akses Lagu Lokal", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Local Audio Empty State
            if (uiState.selectedSourceTab == AudioSourceTab.LOCAL_DEVICE && uiState.hasAudioPermission && localTracks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum Ada File Audio Lokal",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tidak ada file musik yang ditemukan di penyimpanan perangkat via MediaStore.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.scanLocalTracks() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (uiState.isScanningLocal) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Pindai Ulang", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.selectSourceTab(AudioSourceTab.CURATED_STREAM) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Putar Streaming", fontSize = 12.sp, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Category Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.categories) { cat ->
                        val isSelected = uiState.selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(cat) },
                            label = {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceCard,
                                selectedContainerColor = ElectricPurple
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.1f),
                                selectedBorderColor = NeonCyan
                            )
                        )
                    }
                }
            }

            // Track List
            items(filteredTracks, key = { it.id }) { track ->
                val isCurrentTrack = uiState.currentTrack?.id == track.id
                TrackItem(
                    track = track,
                    isSelected = isCurrentTrack,
                    isPlaying = isCurrentTrack && uiState.isPlaying,
                    onClick = {
                        viewModel.playTrack(track)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
