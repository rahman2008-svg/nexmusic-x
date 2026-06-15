package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MusicViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset

@Composable
fun StatsScreen(
    viewModel: MusicViewModel,
    themeColors: ThemeConfig,
    modifier: Modifier = Modifier
) {
    val totalTimeMs by viewModel.totalListeningTimeMs.collectAsState()
    val favoriteArtists by viewModel.favoriteArtists.collectAsState()
    val historyLog by viewModel.playbackHistory.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()

    // Format Listening time
    val minutesPlayed = totalTimeMs / 60000
    val formatTime = when {
        minutesPlayed < 1 -> "1 Minute"
        minutesPlayed < 60 -> "$minutesPlayed Mins"
        else -> {
            val hours = minutesPlayed / 60
            val remainingMins = minutesPlayed % 60
            "$hours hrs $remainingMins min"
        }
    }

    val mostPopularGenre = allTracks.groupBy { it.genre }
        .maxByOrNull { it.value.sumOf { track -> track.playCount } }?.key ?: "Lofi"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
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
                imageVector = Icons.Default.Star,
                contentDescription = "Statistics",
                tint = themeColors.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Listening Stats",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
                Text(
                    text = "Track your complete offline habits",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- STATS LEDGER ROWS ---
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Badges Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBadgeCard(
                        title = "Listening Time",
                        stat = formatTime,
                        subtitle = "Cumulative hours",
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Favorite Genre",
                        stat = mostPopularGenre,
                        subtitle = "Highest play counts",
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- ANIMATED WEEKLY CANVAS CHART ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Weekly Activity Distribution",
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onBackground,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Focus vs Relaxation session counts",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 7-Days simulated heights based on actual/mock stats
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val hoursByDay = listOf(0.4f, 0.75f, 0.2f, 0.85f, 0.5f, 0.95f, 0.6f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            days.forEachIndexed { idx, day ->
                                val fraction = hoursByDay[idx]
                                // Animate height
                                val animatedHeight by animateFloatAsState(
                                    targetValue = fraction,
                                    animationSpec = tween(1000),
                                    label = "height_$idx"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Canvas(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .fillMaxHeight(0.85f)
                                    ) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height
                                        val barHeight = canvasHeight * animatedHeight

                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(themeColors.primary, themeColors.accent)
                                            ),
                                            topLeft = Offset(0f, canvasHeight - barHeight),
                                            size = Size(canvasWidth, barHeight),
                                            cornerRadius = CornerRadius(10f, 10f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = day, fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }

            // Favorite Artists Row
            item {
                Column {
                    Text(
                        text = "Most Played Artists",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (favoriteArtists.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                        ) {
                            Text(
                                text = "Listen to curated loops under Library to rank custom artists.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            favoriteArtists.take(3).forEach { stat ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    themeColors.primary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stat.artist.take(1),
                                                fontWeight = FontWeight.Bold,
                                                color = themeColors.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stat.artist,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = themeColors.onBackground,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${stat.play_count} plays",
                                            fontSize = 9.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Playback Journal Feed
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.List, contentDescription = "History", tint = themeColors.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Acoustic Activity Journal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.primary
                    )
                }
            }

            if (historyLog.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Empty Log", tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No play entries registered yet.", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            } else {
                items(historyLog.take(25)) { logItem ->
                    val matchedTrack = allTracks.find { it.id == logItem.trackId }
                    HistoryRowItem(
                        trackName = matchedTrack?.title ?: "Offline Synthesizer Wave",
                        artist = matchedTrack?.artist ?: "Unknown Artist",
                        durationSec = logItem.durationPlayedMs / 1000,
                        timestamp = logItem.timestamp,
                        themeColors = themeColors
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBadgeCard(
    title: String,
    stat: String,
    subtitle: String,
    themeColors: ThemeConfig,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stat,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = themeColors.primary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
fun HistoryRowItem(
    trackName: String,
    artist: String,
    durationSec: Long,
    timestamp: Long,
    themeColors: ThemeConfig
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeFormatted = formatter.format(Date(timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Played Music note",
                tint = themeColors.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trackName,
                    fontWeight = FontWeight.SemiBold,
                    color = themeColors.onBackground,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = "$artist • listened for ${durationSec}s",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Text(
                text = timeFormatted,
                fontSize = 10.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
