package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
            // 1. Header with Equalizer Master ON/OFF Switch
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
                            text = if (equalizerState.isEnabled) {
                                if (equalizerState.audioSessionId != 0) "Status: Aktif • ExoPlayer #${equalizerState.audioSessionId}"
                                else "Status: Aktif • DSP Siap"
                            } else "Status: Nonaktif",
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

            // 2. Real-time Graphic Frequency Response Curve
            FrequencyResponseGraphic(
                bands = equalizerState.bands,
                isEnabled = equalizerState.isEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Preset Dropdown Picker & Quick Chips
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

            // 4. Interactive Studio Faders for Each Frequency Band
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Band Frekuensi (Fader Slider)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Text(
                            text = "+15 dB s/d -15 dB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row of Studio Vertical Faders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        equalizerState.bands.forEach { bandInfo ->
                            StudioVerticalFader(
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

            // 5. Bass Boost & 3D Virtualizer Card
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
                            text = "Kekuatan: $virtPercent%",
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
 * Komponen Grafik Respon Frekuensi (Graphic EQ Curve Canvas)
 * Menggambar kurva Bezier halus di atas frekuensi band secara real-time
 */
@Composable
private fun FrequencyResponseGraphic(
    bands: List<EqualizerBandInfo>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RESPON FREKUENSI (KURVA GRAFIK)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NeonCyan
                        )
                    )
                }

                Text(
                    text = if (isEnabled) "Realtime Response" else "Bypass (Flat)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isEnabled) SunsetPink else TextMuted,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val padX = 24.dp.toPx()
                    val padY = 14.dp.toPx()
                    val usableH = (h - 2 * padY) / 2f

                    // 1. Grid Lines: +15 dB, 0 dB (Center), -15 dB
                    val gridColor = Color.White.copy(alpha = 0.07f)
                    val centerLineColor = if (isEnabled) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)

                    drawLine(
                        color = gridColor,
                        start = Offset(0f, padY),
                        end = Offset(w, padY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = centerLineColor,
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h - padY),
                        end = Offset(w, h - padY),
                        strokeWidth = 1.dp.toPx()
                    )

                    if (bands.isEmpty()) return@Canvas

                    // 2. Compute point coordinates for each frequency band
                    val points = bands.mapIndexed { index, band ->
                        val x = if (bands.size > 1) {
                            padX + (index.toFloat() / (bands.size - 1)) * (w - 2 * padX)
                        } else {
                            w / 2f
                        }
                        val gainRatio = if (isEnabled) {
                            (band.levelMb.toFloat() / 1500f).coerceIn(-1f, 1f)
                        } else {
                            0f
                        }
                        val y = centerY - (gainRatio * usableH)
                        Offset(x, y)
                    }

                    // 3. Construct smooth Cubic Spline Path
                    val curvePath = Path()
                    val fillPath = Path()

                    val startX = 0f
                    val startY = points.first().y
                    val endX = w
                    val endY = points.last().y

                    curvePath.moveTo(startX, startY)
                    fillPath.moveTo(startX, centerY)
                    fillPath.lineTo(startX, startY)

                    // Connect from edge to first point
                    curvePath.lineTo(points.first().x, points.first().y)
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlX = (p0.x + p1.x) / 2f
                        curvePath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    }

                    // Connect to right edge
                    curvePath.lineTo(endX, endY)
                    fillPath.lineTo(endX, endY)
                    fillPath.lineTo(endX, centerY)
                    fillPath.close()

                    // 4. Draw Gradient Fill beneath curve
                    val fillBrush = Brush.verticalGradient(
                        colors = if (isEnabled) listOf(
                            NeonCyan.copy(alpha = 0.35f),
                            ElectricPurple.copy(alpha = 0.15f),
                            Color.Transparent
                        ) else listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        startY = padY,
                        endY = h
                    )
                    drawPath(path = fillPath, brush = fillBrush)

                    // 5. Draw Glowing Neon Stroke Curve
                    val strokeBrush = Brush.horizontalGradient(
                        colors = if (isEnabled) listOf(
                            NeonCyan,
                            ElectricPurple,
                            SunsetPink
                        ) else listOf(
                            TextMuted,
                            TextMuted
                        )
                    )
                    drawPath(
                        path = curvePath,
                        brush = strokeBrush,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 6. Draw Band Nodes (Circles on points)
                    points.forEachIndexed { idx, pt ->
                        val nodeColor = if (isEnabled) NeonCyan else TextMuted
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.25f),
                            radius = 7.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

/**
 * Komponen Studio Vertical Fader untuk Band Frekuensi Equalizer.
 * Mendukung drag vertikal alami, tap pada track, dan tombol presisi +/- 1 dB.
 */
@Composable
private fun StudioVerticalFader(
    bandInfo: EqualizerBandInfo,
    isEnabled: Boolean,
    onLevelChange: (Short) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(58.dp)
    ) {
        // Gain readout badge (e.g. +4.5 dB)
        val gainDb = bandInfo.gainDb
        val formattedGain = if (gainDb >= 0) "+%.1f".format(gainDb) else "%.1f".format(gainDb)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isEnabled) {
                        if (gainDb > 0) NeonCyan.copy(alpha = 0.18f)
                        else if (gainDb < 0) SunsetPink.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.08f)
                    } else Color.White.copy(alpha = 0.04f)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$formattedGain dB",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = if (isEnabled) {
                        if (gainDb > 0) NeonCyan else if (gainDb < 0) SunsetPink else TextPrimary
                    } else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Increment Button (+)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceElevated)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectTapGestures {
                            val next = (bandInfo.levelMb + 100).coerceAtMost(bandInfo.maxMb.toInt()).toShort()
                            onLevelChange(next)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = if (isEnabled) NeonCyan else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Custom Vertical Fader Track with Native Vertical Drag & Tap
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceElevated)
                .border(
                    width = 1.dp,
                    color = if (isEnabled) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(18.dp)
                )
                .pointerInput(isEnabled, bandInfo.minMb, bandInfo.maxMb) {
                    if (isEnabled) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val totalRange = (bandInfo.maxMb - bandInfo.minMb).toFloat()
                                val deltaMb = (-dragAmount / size.height.toFloat()) * totalRange
                                val newLevel = (bandInfo.levelMb + deltaMb)
                                    .coerceIn(bandInfo.minMb.toFloat(), bandInfo.maxMb.toFloat())
                                    .toInt()
                                    .toShort()
                                onLevelChange(newLevel)
                            }
                        )
                    }
                }
                .pointerInput(isEnabled, bandInfo.minMb, bandInfo.maxMb) {
                    if (isEnabled) {
                        detectTapGestures { tapOffset ->
                            val totalRange = (bandInfo.maxMb - bandInfo.minMb).toFloat()
                            val ratio = 1f - (tapOffset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            val newLevel = (bandInfo.minMb + ratio * totalRange)
                                .coerceIn(bandInfo.minMb.toFloat(), bandInfo.maxMb.toFloat())
                                .toInt()
                                .toShort()
                            onLevelChange(newLevel)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Draw center 0dB mark line and track fills
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f

                // Center 0 dB guide line
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(4.dp.toPx(), centerY),
                    end = Offset(w - 4.dp.toPx(), centerY),
                    strokeWidth = 2.dp.toPx()
                )

                // Fill from center (0dB) to current thumb position
                val range = (bandInfo.maxMb - bandInfo.minMb).toFloat()
                val normalizedLevel = (bandInfo.levelMb - bandInfo.minMb) / range // 0.0 to 1.0
                val thumbY = (1f - normalizedLevel) * (h - 24.dp.toPx()) + 12.dp.toPx()

                if (isEnabled) {
                    val fillTop = minOf(centerY, thumbY)
                    val fillBottom = maxOf(centerY, thumbY)
                    val fillColor = if (thumbY <= centerY) NeonCyan.copy(alpha = 0.45f) else SunsetPink.copy(alpha = 0.45f)

                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(w / 2f - 3.dp.toPx(), fillTop),
                        size = androidx.compose.ui.geometry.Size(6.dp.toPx(), fillBottom - fillTop),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
            }

            // Draggable Knob / Fader Thumb
            val range = (bandInfo.maxMb - bandInfo.minMb).toFloat()
            val normalizedLevel = (bandInfo.levelMb - bandInfo.minMb) / range
            val verticalBias = (1f - normalizedLevel) * 2f - 1f // from -1 (top) to +1 (bottom)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationY = verticalBias * ((150.dp.toPx() - 28.dp.toPx()) / 2f)
                    }
                    .size(width = 30.dp, height = 22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isEnabled) Brush.linearGradient(listOf(ElectricPurple, NeonCyan))
                        else Brush.linearGradient(listOf(SurfaceElevated, TextMuted))
                    )
                    .border(
                        1.dp,
                        if (isEnabled) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Notch line in thumb
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(2.dp)
                        .background(Color.White)
                )
            }
        }

        // Decrement Button (-)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceElevated)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectTapGestures {
                            val next = (bandInfo.levelMb - 100).coerceAtLeast(bandInfo.minMb.toInt()).toShort()
                            onLevelChange(next)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                color = if (isEnabled) SunsetPink else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Center Frequency Label (e.g. 60 Hz / 14 kHz)
        Text(
            text = bandInfo.formattedFreq,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = if (isEnabled) TextPrimary else TextMuted,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
