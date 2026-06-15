package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.LyricLine
import com.example.data.model.Track
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MusicTheme
import com.example.ui.viewmodel.MusicViewModel

// Custom high-fidelity drawn Pause Icon to completely bypass external package requirements
@Composable
fun CustomPauseIcon(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(0.75f)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(0.75f)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    // ViewModel Flows
    val currentScreen by viewModel.currentScreen.collectAsState()
    val themeState by viewModel.currentTheme.collectAsState()

    val currentTrack by viewModel.audioEngine.currentTrack.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val progress by viewModel.audioEngine.playbackProgress.collectAsState()
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()
    val speedMultiplier by viewModel.audioEngine.playbackSpeed.collectAsState()
    val sleepTimerSec by viewModel.audioEngine.sleepTimeRemainingSec.collectAsState()
    val isMuted by viewModel.audioEngine.isMuted.collectAsState()

    val lyricsLines by viewModel.lyricLines.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()
    val isAudioBookMode by viewModel.isAudioBookMode.collectAsState()

    // Controls slide-up player bottom detail sheet
    var isPlayerSheetExpanded by remember { mutableStateOf(false) }

    // Resolve Visual Themes
    val themeColors = when (themeState) {
        MusicTheme.SOPHISTICATED_DARK -> ThemeConfig(
            background = Color(0xFF0A0A0A),
            surface = Color(0xFF121214),
            primary = Color(0xFFA78BFA), // Violet-400
            accent = Color(0xFFE879F9), // Fuchsia-400
            onBackground = Color(0xFFF1F5F9)
        )
        MusicTheme.AMOLED_BLACK -> ThemeConfig(
            background = Color(0xFF000000),
            surface = Color(0xFF101010),
            primary = Color(0xFF00FF7F), // True spring neon-green
            accent = Color(0xFF1A1A1A),
            onBackground = Color(0xFFFFFFFF)
        )
        MusicTheme.SPOTIFY_GREEN -> ThemeConfig(
            background = Color(0xFF121212),
            surface = Color(0xFF222222),
            primary = Color(0xFF1DB954), // Spotify Green
            accent = Color(0xFF2A2A2A),
            onBackground = Color(0xFFFFFFFF)
        )
        MusicTheme.MATERIAL3_DARK -> ThemeConfig(
            background = Color(0xFF141218),
            surface = Color(0xFF2B2930),
            primary = Color(0xFFD0BCFF), // M3 Purple Accent
            accent = Color(0xFF381E72),
            onBackground = Color(0xFFE6E1E5)
        )
        MusicTheme.GLASSMORPHIC_DARK -> ThemeConfig(
            background = Color(0xFF090A11),
            surface = Color(0x22FFFFFF), // Glass translucent card
            primary = Color(0xFFFF007F), // Warm neon cyber pink
            accent = Color(0xFF00FFFF), // Cyan 3D aura
            onBackground = Color(0xFFF5F8FC)
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background),
        topBar = {
            // Elegant Settings Switcher Top bar
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Logo",
                            tint = themeColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NexMusic Studio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onBackground
                        )
                    }
                },
                actions = {
                    // Theme selector circular options
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeOptionSelectorBubble(
                            color = Color(0xFFA78BFA),
                            isSelected = themeState == MusicTheme.SOPHISTICATED_DARK,
                            onClick = { viewModel.setTheme(MusicTheme.SOPHISTICATED_DARK) },
                            desc = "Sophisticated Dark"
                        )
                        ThemeOptionSelectorBubble(
                            color = Color(0xFF00FF7F),
                            isSelected = themeState == MusicTheme.AMOLED_BLACK,
                            onClick = { viewModel.setTheme(MusicTheme.AMOLED_BLACK) },
                            desc = "AMOLED Black"
                        )
                        ThemeOptionSelectorBubble(
                            color = Color(0xFF1DB954),
                            isSelected = themeState == MusicTheme.SPOTIFY_GREEN,
                            onClick = { viewModel.setTheme(MusicTheme.SPOTIFY_GREEN) },
                            desc = "Spotify Green"
                        )
                        ThemeOptionSelectorBubble(
                            color = Color(0xFFD0BCFF),
                            isSelected = themeState == MusicTheme.MATERIAL3_DARK,
                            onClick = { viewModel.setTheme(MusicTheme.MATERIAL3_DARK) },
                            desc = "Material 3"
                        )
                        ThemeOptionSelectorBubble(
                            color = Color(0xFFFF007F),
                            isSelected = themeState == MusicTheme.GLASSMORPHIC_DARK,
                            onClick = { viewModel.setTheme(MusicTheme.GLASSMORPHIC_DARK) },
                            desc = "Glassmorphism"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = themeColors.background)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(themeColors.background)) {
                // --- MINI PLAYER FLOATING PANEL ---
                if (currentTrack != null) {
                    MiniPlayerPanel(
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() },
                        onExpand = { isPlayerSheetExpanded = true },
                        themeColors = themeColors
                    )
                }

                // --- CORE NAVIGATION BAR ---
                NavigationBar(
                    containerColor = themeColors.background,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.LIBRARY,
                        onClick = { viewModel.selectScreen(AppScreen.LIBRARY) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Library") },
                        label = { Text("Library", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = themeColors.background,
                            selectedTextColor = themeColors.primary,
                            indicatorColor = themeColors.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.EQUALIZER,
                        onClick = { viewModel.selectScreen(AppScreen.EQUALIZER) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Engine") },
                        label = { Text("Equalizer", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = themeColors.background,
                            selectedTextColor = themeColors.primary,
                            indicatorColor = themeColors.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.STATISTICS,
                        onClick = { viewModel.selectScreen(AppScreen.STATISTICS) },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Stats") },
                        label = { Text("Stats", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = themeColors.background,
                            selectedTextColor = themeColors.primary,
                            indicatorColor = themeColors.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.DEVELOPER,
                        onClick = { viewModel.selectScreen(AppScreen.DEVELOPER) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Dev") },
                        label = { Text("About", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = themeColors.background,
                            selectedTextColor = themeColors.primary,
                            indicatorColor = themeColors.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        // Content screen manager with smooth animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.LIBRARY -> LibraryScreen(
                    viewModel = viewModel,
                    onTrackPlay = { track, list -> viewModel.playTrackFromList(track, list) },
                    themeColors = themeColors
                )
                AppScreen.EQUALIZER -> ProAudioScreen(
                    viewModel = viewModel,
                    themeColors = themeColors
                )
                AppScreen.STATISTICS -> StatsScreen(
                    viewModel = viewModel,
                    themeColors = themeColors
                )
                AppScreen.DEVELOPER -> DeveloperScreen(
                    themeColors = themeColors
                )
            }
        }
    }

    // --- FULL SCREEN SHEETS: MUSIC PLAYBACK PANEL ---
    if (isPlayerSheetExpanded && currentTrack != null) {
        Dialog(
            onDismissRequest = { isPlayerSheetExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FullscreenPlayerSheet(
                track = currentTrack!!,
                isPlaying = isPlaying,
                progress = progress,
                currentPositionMs = currentPositionMs,
                speedMultiplier = speedMultiplier,
                sleepTimerSec = sleepTimerSec,
                isMuted = isMuted,
                lyricsLines = lyricsLines,
                currentLyricIndex = currentLyricIndex,
                isAudioBookMode = isAudioBookMode,
                onMuteToggle = { viewModel.audioEngine.setMute(!isMuted) },
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                onSeek = { viewModel.audioEngine.seekTo(it) },
                onSpeedChange = { viewModel.audioEngine.setSpeed(it) },
                onSleepTimerSetting = { mins -> viewModel.audioEngine.startSleepTimer(mins) {} },
                onClearSleepTimer = { viewModel.audioEngine.clearSleepTimer() },
                onToggleAudioBook = { viewModel.toggleAudioBookMode() },
                onDismiss = { isPlayerSheetExpanded = false },
                themeColors = themeColors
            )
        }
    }
}

// Minimal Theme selector circle bubble
@Composable
fun ThemeOptionSelectorBubble(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    desc: String
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = Color.White,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

// --- SUB MODULES: MINI PLAYER ---
@Composable
fun MiniPlayerPanel(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    themeColors: ThemeConfig
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onExpand() }
            .testTag("mini_player_panel"),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round micro art
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(getGenreColor(track.genre)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = themeColors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPause, modifier = Modifier.testTag("mini_play_pause_button")) {
                    if (isPlaying) {
                        CustomPauseIcon(color = themeColors.primary, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = themeColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Play Next",
                        tint = themeColors.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Real-time micro progress line
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = themeColors.primary,
                trackColor = themeColors.background
            )
        }
    }
}

// --- SUB WORKSPACE: FULL SCREEN MUSIC SHEET ---
@Composable
fun FullscreenPlayerSheet(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    currentPositionMs: Long,
    speedMultiplier: Float,
    sleepTimerSec: Long,
    isMuted: Boolean,
    lyricsLines: List<LyricLine>,
    currentLyricIndex: Int,
    isAudioBookMode: Boolean,
    onMuteToggle: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSleepTimerSetting: (Int) -> Unit,
    onClearSleepTimer: () -> Unit,
    onToggleAudioBook: () -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeConfig
) {
    val durationString = DateUtils.formatElapsedTime(track.durationMs / 1000)
    val elapsedString = DateUtils.formatElapsedTime(currentPositionMs / 1000)

    // Animated artwork rotational logic
    val infiniteTransition = rememberInfiniteTransition(label = "rotating_artwork")
    val rotationAngle by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing)
            ),
            label = "angle"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Animated zoom ratio based on play state
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "art_scale"
    )

    var showSleepTimerMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(16.dp)
    ) {
        // --- SHEETS HEADER ACCESS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("fullscreen_player_close")) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize Screen",
                    tint = themeColors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PLAYING FROM LIBRARY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = track.genre,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.primary
                )
            }

            // Sleep Timer Icon with dropdown context
            Box {
                IconButton(onClick = { showSleepTimerMenu = !showSleepTimerMenu }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerSec > 0) themeColors.primary else themeColors.onBackground
                    )
                }

                DropdownMenu(
                    expanded = showSleepTimerMenu,
                    onDismissRequest = { showSleepTimerMenu = false },
                    modifier = Modifier
                        .background(themeColors.surface)
                        .border(1.dp, themeColors.primary, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("10 Minutes", color = themeColors.onBackground) },
                        onClick = { onSleepTimerSetting(10); showSleepTimerMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("30 Minutes", color = themeColors.onBackground) },
                        onClick = { onSleepTimerSetting(30); showSleepTimerMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("1 Hour", color = themeColors.onBackground) },
                        onClick = { onSleepTimerSetting(60); showSleepTimerMenu = false }
                    )
                    if (sleepTimerSec > 0) {
                        DropdownMenuItem(
                            text = { Text("Clear Timer", color = Color.Red) },
                            onClick = { onClearSleepTimer(); showSleepTimerMenu = false }
                        )
                    }
                }
            }
        }

        // Active Sleep Timer Counter indicator
        if (sleepTimerSec > 0) {
            Text(
                text = "Sleep Timer: ${DateUtils.formatElapsedTime(sleepTimerSec)} remaining",
                color = themeColors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ARTWORK AND LYRICS DUAL VIEWER COMPARTMENT ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Glowing Art (Zoom and Rotate)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(0.85f)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ambient halo neon border box
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(themeColors.primary, themeColors.accent, themeColors.primary)
                            ),
                            shape = CircleShape
                        )
                        .graphicsLayer { rotationZ = rotationAngle },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(145.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(getGenreColor(track.genre), Color.Black)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.scale(2f)
                        )
                    }
                }
            }

            // Synced LRC lyrics view (Interactive scroll pane)
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, themeColors.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                if (lyricsLines.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No Synced Lyrics\navailable for track.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                } else {
                    val listState = rememberLazyListState()

                    // Auto scroll effect
                    LaunchedEffect(currentLyricIndex) {
                        if (currentLyricIndex >= 0) {
                            listState.animateScrollToItem(currentLyricIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 40.dp)
                    ) {
                        itemsIndexed(lyricsLines) { idx, line: LyricLine ->
                            val isActive = idx == currentLyricIndex

                            // Spring text glow animations
                            val lyricTextColor by animateColorAsState(
                                targetValue = if (isActive) themeColors.primary else Color.LightGray.copy(alpha = 0.5f),
                                animationSpec = tween(200),
                                label = "color"
                            )
                            val lyricFontSize by animateFloatAsState(
                                targetValue = if (isActive) 13f else 11f,
                                animationSpec = tween(200),
                                label = "fontSize"
                            )

                            Text(
                                text = line.text,
                                color = lyricTextColor,
                                fontSize = lyricFontSize.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TRACK AND ARTIST IDENTIFIER ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = themeColors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${track.artist}  •  ${track.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- VARIABLE SPEED MULTIPLIER SELECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed: ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
            speeds.forEach { s ->
                val active = speedMultiplier == s
                Card(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onSpeedChange(s) },
                    colors = CardDefaults.cardColors(containerColor = if (active) themeColors.primary else themeColors.surface)
                ) {
                    Text(
                        text = "${s}x",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) themeColors.background else themeColors.onBackground,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SCRUB SEEKBAR METERS ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { seeker ->
                    val targetMs = (seeker * track.durationMs).toLong()
                    onSeek(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = themeColors.primary,
                    activeTrackColor = themeColors.primary,
                    inactiveTrackColor = themeColors.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("track_progress_slider")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = elapsedString, fontSize = 10.sp, color = Color.LightGray)
                Text(text = durationString, fontSize = 10.sp, color = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- AUDIO BOOK MODE TOGGLE CONCORDANCE ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleAudioBook() },
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAudioBookMode) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Audiobook",
                        tint = themeColors.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Audiobook Position Save Mode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onBackground
                        )
                        Text(
                            text = "Preserves exact file speaking position",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
                Switch(
                    checked = isAudioBookMode,
                    onCheckedChange = { onToggleAudioBook() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = themeColors.background,
                        checkedTrackColor = themeColors.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SHUFFLE / PREV / PLAY / NEXT / MUTE CONTROL PANEL ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Speaker switch
            IconButton(onClick = onMuteToggle) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Mute",
                    tint = if (isMuted) Color.Red else themeColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous Button
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Prev",
                    tint = themeColors.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Large center Play button wrapped in neon-bordered circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(themeColors.primary)
                    .clickable { onPlayPause() }
                    .testTag("fullscreen_play_pause"),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    CustomPauseIcon(color = themeColors.background, modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = themeColors.background,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Next Button
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    tint = themeColors.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Quick Info popup trigger
            var showEcosystemBrief by remember { mutableStateOf(false) }
            IconButton(onClick = { showEcosystemBrief = !showEcosystemBrief }) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Ecosystem badge",
                    tint = themeColors.primary
                )
            }

            if (showEcosystemBrief) {
                AlertDialog(
                    onDismissRequest = { showEcosystemBrief = false },
                    title = { Text("NexMusic X Ecosystem", fontWeight = FontWeight.Bold, color = themeColors.primary) },
                    text = {
                        Text(
                            text = "Licensed under NexVora Lab's Ofc ecosystem. Pure client-side secure environment compiled for Prince AR Abdur Rahman.",
                            color = themeColors.onBackground,
                            fontSize = 12.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showEcosystemBrief = false },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                        ) {
                            Text("Acknowledge", color = themeColors.background)
                        }
                    },
                    containerColor = themeColors.surface
                )
            }
        }
    }
}
