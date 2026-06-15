package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun ProAudioScreen(
    viewModel: MusicViewModel,
    themeColors: ThemeConfig,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Collect flow states
    val eqEnabled by viewModel.audioEngine.eqEnabled.collectAsState()
    val eqIsTenBand by viewModel.eqIsTenBand.collectAsState()
    val eqBands by viewModel.audioEngine.eqBands.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()

    val bassBoostLevel by viewModel.audioEngine.bassBoostLevel.collectAsState()
    val virtualizerLevel by viewModel.audioEngine.virtualizerLevel.collectAsState()
    val loudnessEnhancerLevel by viewModel.audioEngine.loudnessBoost.collectAsState()
    val vocalBoostLevel by viewModel.audioEngine.vocalBoostLevel.collectAsState()

    // Locally track Treble Boost (simulated or mapped to high band EQ)
    var trebleBoostLocally by remember { mutableStateOf(50f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Equalizer",
                tint = themeColors.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Pro Audio Engine",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
                Text(
                    text = "Poweramp-quality high fidelity control",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- EQUALIZER Master Toggle Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Audio Equalizer",
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onBackground,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (eqEnabled) "Bands active" else "Equalizer bypassed",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.toggleEqBandLayout() }
                        ) {
                            Text(
                                text = if (eqIsTenBand) "10 Bands" else "5 Bands",
                                color = themeColors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { viewModel.audioEngine.toggleEq(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = themeColors.background,
                                checkedTrackColor = themeColors.primary,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = themeColors.surface
                            ),
                            modifier = Modifier.testTag("eq_master_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- EQ SLIDERS BAR ---
                if (eqEnabled) {
                    val actualBandsCount = if (eqIsTenBand) 10 else 5
                    val bandsListLabels = if (eqIsTenBand) {
                        listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
                    } else {
                        listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 0 until actualBandsCount) {
                            // Map the 10 bands to the 5 standard hardware EQ parameters safely
                            val eqIndex = if (eqIsTenBand) i / 2 else i
                            val bandProgress = eqBands.getOrElse(eqIndex) { 0f }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "${bandProgress.toInt()}dB",
                                    fontSize = 9.sp,
                                    color = themeColors.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Slider(
                                    value = bandProgress,
                                    onValueChange = { newValue ->
                                        viewModel.audioEngine.setEqBandGain(eqIndex, newValue)
                                    },
                                    valueRange = -15f..15f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("eq_slider_$i"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = themeColors.primary,
                                        activeTrackColor = themeColors.primary,
                                        inactiveTrackColor = themeColors.background
                                    )
                                )

                                Text(
                                    text = bandsListLabels[i],
                                    fontSize = 8.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // --- PRESETS ROW ---
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Acoustic Presets",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf("Normal", "Rock", "Pop", "Jazz", "Bass Booster", "Classic")
                        items(presets) { p ->
                            FilterChip(
                                selected = currentPreset == p,
                                onClick = { viewModel.applyPreset(p) },
                                label = { Text(p, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = themeColors.primary,
                                    selectedLabelColor = themeColors.background,
                                    containerColor = themeColors.background,
                                    labelColor = themeColors.onBackground
                                )
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(themeColors.background, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Bypassed",
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Toggle switch to unlock Equalizer",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PRO AUDIO EFFECTS PANELS ---
        Text(
            text = "Audio Environment & Booster FX",
            fontWeight = FontWeight.Bold,
            color = themeColors.primary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Bass Boost Card
        AudioEffectControlItem(
            title = "Subwoofer Bass Boost",
            value = bassBoostLevel / 10f, // 0 to 100 range
            onValueChange = { viewModel.audioEngine.setBassBoost(it) },
            category = "Native 3D Low Filter Boost",
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3D Surround / Virtualizer Card
        AudioEffectControlItem(
            title = "Spatial 3D Surround",
            value = virtualizerLevel / 10f,
            onValueChange = { viewModel.audioEngine.setVirtualizer(it) },
            category = "Acoustic Virtualizer Expansion",
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vocal Boost Card
        AudioEffectControlItem(
            title = "Vocal Accent Booster",
            value = vocalBoostLevel, // 0 to 100 range
            onValueChange = { viewModel.audioEngine.setVocalBoost(it) },
            category = "Mid-range Frequency Compression",
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Loudness Booster (Pre-amp amplifier)
        AudioEffectControlItem(
            title = "Hard Loudness Enhancer",
            value = loudnessEnhancerLevel / 20f, // 0 to 100 range
            onValueChange = { viewModel.audioEngine.setLoudnessEnhancer(it) },
            category = "Pre-amplifier absolute gain (mB)",
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Treble Boost Card
        AudioEffectControlItem(
            title = "Treble Clarity Booster",
            value = trebleBoostLocally,
            onValueChange = {
                trebleBoostLocally = it
                // Map treble booster directly to high band equalizer elements (band 4)
                if (eqEnabled) {
                    viewModel.audioEngine.setEqBandGain(4, (it - 50f) * 0.3f)
                }
            },
            category = "High Frequency Sharpener",
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Safe Tips Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tips",
                    tint = themeColors.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Audiophile Notice: Extremely high settings for treble booster or preamp loudness might distort local simulated waveforms depending on your system's hardware capabilities. Tune securely.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun AudioEffectControlItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    category: String,
    themeColors: ThemeConfig
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onBackground,
                        fontSize = 13.sp
                    )
                    Text(
                        text = category,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "${value.toInt()}%",
                    color = themeColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = themeColors.primary,
                    activeTrackColor = themeColors.primary,
                    inactiveTrackColor = themeColors.background
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
