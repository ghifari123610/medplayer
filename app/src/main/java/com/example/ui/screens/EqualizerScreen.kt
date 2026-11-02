package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.EqualizerBandInfo
import com.example.playback.EqualizerController
import com.example.ui.MainViewModel
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SunsetPink
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val equalizerController = viewModel.equalizerController
    val equalizerState by viewModel.equalizerState.collectAsState()

    var isPresetDropdownExpanded by remember { mutableStateOf(false) }

    val currentPresetName = if (equalizerState.currentPreset in 0 until equalizerState.presets.size) {
        equalizerState.presets[equalizerState.currentPreset.toInt()]
    } else {
        "Kustom (Manual)"
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen_view"),
        color = ObsidianDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header with Equalizer Master ON/OFF Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Audio Equalizer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (equalizerState.isEnabled) "Status: Aktif • ExoPlayer #${equalizerState.audioSessionId}" else "Status: Nonaktif",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (equalizerState.isEnabled) NeonCyan else TextMuted
                            )
                        )
                    }
                }

                // Master ON/OFF Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (equalizerState.isEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (equalizerState.isEnabled) NeonCyan else TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = equalizerState.isEnabled,
                        onCheckedChange = { equalizerController.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricPurple,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        ),
                        modifier = Modifier.testTag("equalizer_switch_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset Dropdown Picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PRESET EQUALIZER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ExposedDropdownMenu for Presets
                    ExposedDropdownMenuBox(
                        expanded = isPresetDropdownExpanded,
                        onExpandedChange = {
                            if (equalizerState.isEnabled) {
                                isPresetDropdownExpanded = !isPresetDropdownExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = currentPresetName,
                            onValueChange = {},
                            readOnly = true,
                            enabled = equalizerState.isEnabled,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPresetDropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("preset_dropdown_field")
                        )

                        ExposedDropdownMenu(
                            expanded = isPresetDropdownExpanded,
                            onDismissRequest = { isPresetDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceElevated)
                        ) {
                            equalizerState.presets.forEachIndexed { index, preset ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = preset,
                                            color = if (equalizerState.currentPreset == index.toShort()) NeonCyan else TextPrimary,
                                            fontWeight = if (equalizerState.currentPreset == index.toShort()) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        equalizerController.usePreset(index.toShort())
                                        isPresetDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        itemsIndexed(equalizerState.presets) { index, preset ->
                            val isSelected = equalizerState.currentPreset == index.toShort()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (equalizerState.isEnabled) {
                                        equalizerController.usePreset(index.toShort())
                                    }
                                },
                                enabled = equalizerState.isEnabled,
                                label = {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceElevated,
                                    selectedContainerColor = ElectricPurple
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = equalizerState.isEnabled,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.1f),
                                    selectedBorderColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graphic Equalizer: Vertical Interactive Sliders for Each Frequency Band
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(listOf(ElectricPurple, NeonCyan))
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Band Frekuensi (Graphic EQ)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = "+15 dB s/d -15 dB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row of Vertical Sliders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        equalizerState.bands.forEach { bandInfo ->
                            VerticalBandSlider(
                                bandInfo = bandInfo,
                                isEnabled = equalizerState.isEnabled,
                                onLevelChange = { newLevelMb ->
                                    equalizerController.setBandLevel(bandInfo.band, newLevelMb)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bass Boost & 3D Virtualizer Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Section
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speaker,
                                    contentDescription = null,
                                    tint = SunsetPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bass Boost",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            Switch(
                                checked = equalizerState.isBassBoostEnabled,
                                onCheckedChange = { equalizerController.setBassBoostEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = SunsetPink,
                                    checkedThumbColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val bbPercent = (equalizerState.bassBoostStrength / 10f).roundToInt()
                        Text(
                            text = "Kekuatan: $bbPercent%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SunsetPink,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Slider(
                            value = equalizerState.bassBoostStrength.toFloat(),
                            onValueChange = {
                                equalizerController.setBassBoostStrength(it.toInt().toShort())
                            },
                            valueRange = 0f..1000f,
                            enabled = equalizerState.isBassBoostEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = SunsetPink,
                                activeTrackColor = SunsetPink,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("bass_boost_controller_slider")
                        )
                    }
                }

                // 3D Virtualizer Section
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SurroundSound,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "3D Surround",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            Switch(
                                checked = equalizerState.isVirtualizerEnabled,
                                onCheckedChange = { equalizerController.setVirtualizerEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = NeonCyan,
                                    checkedThumbColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val virtPercent = (equalizerState.virtualizerStrength / 10f).roundToInt()
                        Text(
                            text = "Spatial: $virtPercent%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Slider(
                            value = equalizerState.virtualizerStrength.toFloat(),
                            onValueChange = {
                                equalizerController.setVirtualizerStrength(it.toInt().toShort())
                            },
                            valueRange = 0f..1000f,
                            enabled = equalizerState.isVirtualizerEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.testTag("virtualizer_controller_slider")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Komponen Slider Vertikal Interaktif untuk Band Frekuensi Equalizer.
 * Menggunakan rotasi 270 derajat pada Slider Jetpack Compose sehingga geser ke atas menaikkan gain (dB)
 * dan geser ke bawah menurunkan gain.
 */
@Composable
private fun VerticalBandSlider(
    bandInfo: EqualizerBandInfo,
    isEnabled: Boolean,
    onLevelChange: (Short) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(58.dp)
            .height(220.dp)
    ) {
        // Gain readout (e.g. +4.5 dB)
        val gainDb = bandInfo.gainDb
        val formattedGain = if (gainDb >= 0) "+%.1f".format(gainDb) else "%.1f".format(gainDb)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isEnabled) ElectricPurple.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$formattedGain dB",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = if (isEnabled) NeonCyan else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // True Vertical Slider Container
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = bandInfo.levelMb.toFloat(),
                onValueChange = {
                    onLevelChange(it.toInt().toShort())
                },
                valueRange = bandInfo.minMb.toFloat()..bandInfo.maxMb.toFloat(),
                enabled = isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = ElectricPurple,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .requiredWidth(140.dp)
                    .graphicsLayer {
                        rotationZ = 270f
                    }
                    .testTag("vertical_slider_band_${bandInfo.band}")
            )
        }

        // Center Frequency Label (e.g. 60 Hz / 14 kHz)
        Text(
            text = bandInfo.formattedFreq,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = if (isEnabled) TextPrimary else TextMuted,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
