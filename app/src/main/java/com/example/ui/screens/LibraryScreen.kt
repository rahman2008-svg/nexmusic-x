package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dao.ArtistStat
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onTrackPlay: (Track, List<Track>) -> Unit,
    themeColors: ThemeConfig,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All Songs", "Playlists", "Recent", "Most Played")

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val favoritedTracks by viewModel.favoriteTracks.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    
    val selectedMood by viewModel.selectedMood.collectAsState()
    val selectedSmartGroup by viewModel.selectedSmartGroup.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf<Track?>(null) }

    // Custom Playlist State Drawer
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val activePlaylistTracks by viewModel.activePlaylistTracks.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. TITLE & AUTO SCAN CARD ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NexMusic X",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
                Text(
                    text = "Ultimate Offline Player",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        isScanning = true
                        scanProgress = 0f
                        while (scanProgress < 1.0f) {
                            delay(100)
                            scanProgress += 0.1f
                        }
                        viewModel.triggerLibraryAutoScan()
                        isScanning = false
                    }
                },
                modifier = Modifier
                    .background(themeColors.surface, RoundedCornerShape(12.dp))
                    .testTag("auto_scan_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier.size(24.dp),
                        color = themeColors.primary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Audio Files",
                        tint = themeColors.primary
                    )
                }
            }
        }

        // Animated scan helper card
        if (isScanning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Scanning Folders",
                        tint = themeColors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Scanning Device Storage...",
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onBackground,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Importing songs with Poweramp Engine...",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // --- 2. SEARCH BAR ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("music_search_input"),
            placeholder = { Text("Search songs, folders, artists...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = themeColors.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = themeColors.primary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.primary,
                unfocusedBorderColor = themeColors.surface,
                focusedContainerColor = themeColors.surface,
                unfocusedContainerColor = themeColors.surface,
                focusedTextColor = themeColors.onBackground,
                unfocusedTextColor = themeColors.onBackground
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // --- 3. MOOD & SMART COLLECTIONS FILTERS ---
        // Mood collection horizontal row
        Text(
            text = "Mood Playlists",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.primary.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            val moods = listOf("Relax", "Focus", "Workout", "Sleep")
            item {
                FilterChip(
                    selected = selectedMood == null,
                    onClick = { viewModel.setMoodFilter(null) },
                    label = { Text("All Moods") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.primary,
                        selectedLabelColor = themeColors.background,
                        containerColor = themeColors.surface,
                        labelColor = themeColors.onBackground
                    )
                )
            }
            items(moods) { mood ->
                FilterChip(
                    selected = selectedMood == mood,
                    onClick = { viewModel.setMoodFilter(mood) },
                    label = { Text(mood) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.primary,
                        selectedLabelColor = themeColors.background,
                        containerColor = themeColors.surface,
                        labelColor = themeColors.onBackground
                    )
                )
            }
        }

        // Smart Collection Types row (Automatically Group: New, Old, Long, Short)
        Text(
            text = "Smart Collections",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.primary.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            val groups = listOf("New", "Old", "Long", "Short")
            item {
                FilterChip(
                    selected = selectedSmartGroup == null,
                    onClick = { viewModel.setSmartGroupFilter(null) },
                    label = { Text("Standard Size") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.primary,
                        selectedLabelColor = themeColors.background,
                        containerColor = themeColors.surface,
                        labelColor = themeColors.onBackground
                    )
                )
            }
            items(groups) { group ->
                val displayLabel = when (group) {
                    "New" -> "New Songs"
                    "Old" -> "Old Songs"
                    "Long" -> "Long (>5m)"
                    "Short" -> "Short (<1m)"
                    else -> group
                }
                FilterChip(
                    selected = selectedSmartGroup == group,
                    onClick = { viewModel.setSmartGroupFilter(group) },
                    label = { Text(displayLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.primary,
                        selectedLabelColor = themeColors.background,
                        containerColor = themeColors.surface,
                        labelColor = themeColors.onBackground
                    )
                )
            }
        }

        // If a playlist is active, display the playlist views instead of tabs
        if (activePlaylist != null) {
            PlaylistDetailView(
                playlist = activePlaylist!!,
                tracks = activePlaylistTracks,
                onBack = { viewModel.selectActivePlaylist(null) },
                onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(activePlaylist!!.id, trackId) },
                onPlayTrack = { track -> onTrackPlay(track, activePlaylistTracks) },
                themeColors = themeColors
            )
        } else {
            // --- 4. TAB BAR ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = themeColors.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = themeColors.primary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, text ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = text,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        unselectedContentColor = Color.Gray,
                        selectedContentColor = themeColors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 5. MAIN LISTINGS TAB RENDER ---
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> { // All Songs (Using filtered list)
                        if (filteredTracks.isEmpty()) {
                            LibraryEmptyState(themeColors, "No songs found.", "Try clearing filters, searching, or click Sync trigger above.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredTracks) { track ->
                                    TrackItemRow(
                                        track = track,
                                        isPlaying = viewModel.audioEngine.currentTrack.collectAsState().value?.id == track.id,
                                        isAudioPlaying = viewModel.audioEngine.isPlaying.collectAsState().value,
                                        onPlay = { onTrackPlay(track, filteredTracks) },
                                        onFavoriteToggle = {
                                            coroutineScope.launch {
                                                viewModel.toggleFavoriteTrack(track.id)
                                            }
                                        },
                                        onPlaylistAddTrigger = { showAddToPlaylistDialog = track },
                                        themeColors = themeColors
                                    )
                                }
                            }
                        }
                    }

                    1 -> { // Playlists list view
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Playlists",
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.onBackground
                                )
                                Button(
                                    onClick = { showCreatePlaylistDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface, contentColor = themeColors.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = "Create", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Playlist", fontSize = 12.sp)
                                }
                            }

                            if (playlists.isEmpty()) {
                                LibraryEmptyState(themeColors, "No playlists created.", "Click 'New' above to set custom offline vibes.")
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(playlists) { playlist ->
                                        PlaylistRowItem(
                                            playlist = playlist,
                                            onClick = { viewModel.selectActivePlaylist(playlist) },
                                            onDelete = { viewModel.deleteCustomPlaylist(playlist.id) },
                                            themeColors = themeColors
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // Recent added
                        if (recentlyAdded.isEmpty()) {
                            LibraryEmptyState(themeColors, "Empty index files.", "Sync songs to compile offline tracking databases.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(recentlyAdded) { track ->
                                    TrackItemRow(
                                        track = track,
                                        isPlaying = viewModel.audioEngine.currentTrack.collectAsState().value?.id == track.id,
                                        isAudioPlaying = viewModel.audioEngine.isPlaying.collectAsState().value,
                                        onPlay = { onTrackPlay(track, recentlyAdded) },
                                        onFavoriteToggle = {
                                            coroutineScope.launch {
                                                viewModel.toggleFavoriteTrack(track.id)
                                            }
                                        },
                                        onPlaylistAddTrigger = { showAddToPlaylistDialog = track },
                                        themeColors = themeColors
                                    )
                                }
                            }
                        }
                    }

                    3 -> { // Most played Top-20
                        if (mostPlayed.isEmpty()) {
                            LibraryEmptyState(themeColors, "No playback history.", "Start playing songs to compute most valuable tracks.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(mostPlayed) { idx, track ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${idx + 1}.",
                                            color = themeColors.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(32.dp),
                                            fontSize = 14.sp
                                        )
                                        Box(modifier = Modifier.weight(1f)) {
                                            TrackItemRow(
                                                track = track,
                                                isPlaying = viewModel.audioEngine.currentTrack.collectAsState().value?.id == track.id,
                                                isAudioPlaying = viewModel.audioEngine.isPlaying.collectAsState().value,
                                                onPlay = { onTrackPlay(track, mostPlayed) },
                                                onFavoriteToggle = {
                                                    coroutineScope.launch {
                                                        viewModel.toggleFavoriteTrack(track.id)
                                                    }
                                                },
                                                onPlaylistAddTrigger = { showAddToPlaylistDialog = track },
                                                themeColors = themeColors
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS CONTROLLERS ---
    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist", color = themeColors.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = themeColors.onBackground,
                            unfocusedTextColor = themeColors.onBackground
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = themeColors.onBackground,
                            unfocusedTextColor = themeColors.onBackground
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createCustomPlaylist(playlistName, playlistDesc)
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary, contentColor = themeColors.background)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = themeColors.primary)
                }
            },
            containerColor = themeColors.surface
        )
    }

    if (showAddToPlaylistDialog != null) {
        val targetTrack = showAddToPlaylistDialog!!
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            title = { Text("Add Track to Playlist", color = themeColors.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (playlists.isEmpty()) {
                        Text("Create a custom playlist first from the Playlists tab.", color = Color.LightGray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 240.dp)) {
                            items(playlists) { playlist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addTrackToPlaylist(playlist.id, targetTrack.id)
                                            showAddToPlaylistDialog = null
                                        },
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                                ) {
                                    Text(
                                        text = playlist.name,
                                        modifier = Modifier.padding(16.dp),
                                        color = themeColors.onBackground,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Close", color = themeColors.primary)
                }
            },
            containerColor = themeColors.surface
        )
    }
}

@Composable
fun TrackItemRow(
    track: Track,
    isPlaying: Boolean,
    isAudioPlaying: Boolean,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onPlaylistAddTrigger: () -> Unit,
    themeColors: ThemeConfig
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("track_item_${track.id}"),
        colors = CardDefaults.cardColors(containerColor = if (isPlaying) themeColors.surface else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded Genre Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                getGenreColor(track.genre).copy(alpha = 0.8f),
                                themeColors.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying && isAudioPlaying) {
                    SoundWaveVisualizer(isPlaying = true, modifier = Modifier.size(24.dp), color = themeColors.primary)
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                        contentDescription = "Song Thumbnail",
                        tint = if (isPlaying) themeColors.primary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) themeColors.primary else themeColors.onBackground
                )
                Text(
                    text = "${track.artist} • ${track.genre}",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play Count Indicator
            if (track.playCount > 0) {
                Badge(
                    containerColor = themeColors.surface,
                    contentColor = themeColors.primary,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("${track.playCount} plays", fontSize = 9.sp)
                }
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (track.isFavorite) themeColors.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onPlaylistAddTrigger) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to playlist",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistRowItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    themeColors: ThemeConfig
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playlist.isSystemCreated) Icons.Default.Star else Icons.Default.List,
                    contentDescription = "Playlist",
                    tint = themeColors.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = themeColors.onBackground
                )
                Text(
                    text = playlist.description.ifEmpty { "Offline Custom Playlist" },
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }

            if (!playlist.isSystemCreated) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Playlist",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Custom drag-and-drop simulated list detail
@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    tracks: List<Track>,
    onBack: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onPlayTrack: (Track) -> Unit,
    themeColors: ThemeConfig
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = themeColors.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = themeColors.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tracks.size} offline tracks scanned",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        if (tracks.isEmpty()) {
            LibraryEmptyState(themeColors, "Playlist is empty", "Click the '+' icon of any track in the 'All Songs' tab to inject tracks.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayTrack(track) },
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Reorder", tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.onBackground,
                                    fontSize = 13.sp
                                )
                                Text(text = track.artist, color = Color.LightGray, fontSize = 10.sp)
                            }
                            IconButton(onClick = { onRemoveTrack(track.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryEmptyState(themeColors: ThemeConfig, headline: String, subline: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = "Empty",
            tint = themeColors.primary.copy(alpha = 0.35f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = headline, color = themeColors.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            text = subline,
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SoundWaveVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffsets = List(4) { index ->
        if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 280 + index * 95, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
        } else {
            remember { mutableStateOf(0.15f) }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveOffsets.forEach { progressState ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(progressState.value)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
    }
}

fun getGenreColor(genre: String): Color {
    return when (genre.lowercase()) {
        "lofi" -> Color(0xFF9370DB) // Medium Purple
        "ambient" -> Color(0xFF00CED1) // Dark Turquoise
        "trance" -> Color(0xFFFF1493) // Deep Pink
        "cosmic" -> Color(0xFF4169E1) // Royal Blue
        else -> Color(0xFF00FF7F) // Spring Green
    }
}

data class ThemeConfig(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val accent: Color,
    val onBackground: Color
)
