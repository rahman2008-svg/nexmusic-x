package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.data.AppDatabase
import com.example.data.MusicRepository
import com.example.data.dao.ArtistStat
import com.example.data.model.PlaybackHistory
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    LIBRARY, EQUALIZER, STATISTICS, DEVELOPER
}

enum class MusicTheme {
    SOPHISTICATED_DARK, AMOLED_BLACK, SPOTIFY_GREEN, MATERIAL3_DARK, GLASSMORPHIC_DARK
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = MusicRepository(application, db.musicDao())
    val audioEngine = AudioEngine(application)

    // --- NAVIGATION & UI THEME STATE ---
    private val _currentScreen = MutableStateFlow(AppScreen.LIBRARY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    private val _currentTheme = MutableStateFlow(MusicTheme.SOPHISTICATED_DARK)
    val currentTheme: StateFlow<MusicTheme> = _currentTheme

    // --- LIBRARY DATA STREAMS ---
    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Track>> = repository.recentlyAdded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Track>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Playlist for listing its tracks
    private val _activePlaylist = MutableStateFlow<Playlist?>(null)
    val activePlaylist: StateFlow<Playlist?> = _activePlaylist

    private val _activePlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val activePlaylistTracks: StateFlow<List<Track>> = _activePlaylistTracks

    // Search & Category Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedMood = MutableStateFlow<String?>(null) // Relax, Focus, Workout, Sleep
    val selectedMood: StateFlow<String?> = _selectedMood

    private val _selectedSmartGroup = MutableStateFlow<String?>(null) // "New", "Old", "Long", "Short", or null
    val selectedSmartGroup: StateFlow<String?> = _selectedSmartGroup

    // Filters integration
    val filteredTracks: StateFlow<List<Track>> = combine(
        allTracks,
        searchQuery,
        selectedMood,
        selectedSmartGroup
    ) { tracks, query, mood, group ->
        tracks.filter { track ->
            val matchesQuery = track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true) ||
                    track.album.contains(query, ignoreCase = true) ||
                    track.genre.contains(query, ignoreCase = true)

            val matchesMood = mood == null || track.mood.equals(mood, ignoreCase = true)

            val matchesGroup = when (group) {
                "New" -> track.isNew()
                "Old" -> track.isOld()
                "Long" -> track.isLong()
                "Short" -> track.isShort()
                else -> true
            }

            matchesQuery && matchesMood && matchesGroup
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- PLAYBACK QUEUE ---
    private val _playbackQueue = MutableStateFlow<List<Track>>(emptyList())
    val playbackQueue: StateFlow<List<Track>> = _playbackQueue

    private val _currentTrackIndex = MutableStateFlow(-1)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex

    // --- ADVANCED AUDIO BOOK MODE PROGRESS STORE ---
    private val _isAudioBookMode = MutableStateFlow(false)
    val isAudioBookMode: StateFlow<Boolean> = _isAudioBookMode

    private val savedTrackPositions = mutableMapOf<Long, Long>() // trackId -> positionMs

    // --- SYNCED LRC LYRICS STATE ---
    private val _lyricLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyricLines: StateFlow<List<LyricLine>> = _lyricLines

    val currentLyricIndex: StateFlow<Int> = combine(
        audioEngine.currentPositionMs,
        _lyricLines
    ) { positionMs, lines ->
        lines.indexOfLast { positionMs >= it.timeMs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(250), -1)

    // --- EQ PRESETS DEFINITIONS ---
    private val _eqIsTenBand = MutableStateFlow(false) // toggle 5 band vs 10 band layout
    val eqIsTenBand: StateFlow<Boolean> = _eqIsTenBand

    private val _currentPreset = MutableStateFlow("Normal")
    val currentPreset: StateFlow<String> = _currentPreset

    // --- STATISTICS DATA ---
    val totalListeningTimeMs: StateFlow<Long> = repository.totalTimeMs
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val favoriteArtists: StateFlow<List<ArtistStat>> = repository.favoriteArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackHistory: StateFlow<List<PlaybackHistory>> = repository.currentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically scan / prepopulate db on first launch
        viewModelScope.launch {
            repository.scanLocalMediaAndSeed()
        }

        // Monitor current playing track changes to load synced LRC lyrics
        viewModelScope.launch {
            audioEngine.currentTrack.collect { track ->
                if (track != null) {
                    _lyricLines.value = parseLrc(track.lyrics)
                    
                    // Audio Book Mode check: restore previous playing position of this specific track
                    if (_isAudioBookMode.value) {
                        val savedPos = savedTrackPositions[track.id] ?: 0L
                        if (savedPos > 0) {
                            audioEngine.seekTo(savedPos)
                        }
                    }

                    // Increment playlist stats
                    repository.incrementPlayCount(track.id)
                } else {
                    _lyricLines.value = emptyList()
                }
            }
        }

        // Monitor progress periodically to save playback progress under Audio Book Mode
        viewModelScope.launch {
            audioEngine.currentPositionMs.collect { pos ->
                val track = audioEngine.currentTrack.value
                if (track != null && _isAudioBookMode.value && pos > 0) {
                    savedTrackPositions[track.id] = pos
                }
            }
        }
    }

    // --- USER ACTIONS ---
    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setTheme(theme: MusicTheme) {
        _currentTheme.value = theme
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMoodFilter(mood: String?) {
        _selectedMood.value = mood
    }

    fun setSmartGroupFilter(group: String?) {
        _selectedSmartGroup.value = group
    }

    fun toggleAudioBookMode() {
        _isAudioBookMode.value = !_isAudioBookMode.value
    }

    // --- PLAYBACK TRIGGERS ---
    fun playTrackFromList(track: Track, contextList: List<Track>) {
        _playbackQueue.value = contextList
        val index = contextList.indexOfFirst { it.id == track.id }
        _currentTrackIndex.value = if (index != -1) index else 0
        audioEngine.playTrack(track)
    }

    fun togglePlayPause() {
        if (audioEngine.isPlaying.value) {
            // Log active play session to stats before stopping/pausing
            logPlayingHistorySegment()
            audioEngine.pause()
        } else {
            if (audioEngine.currentTrack.value != null) {
                audioEngine.resume()
            } else if (allTracks.value.isNotEmpty()) {
                // Play first song
                playTrackFromList(allTracks.value.first(), allTracks.value)
            }
        }
    }

    fun playNext() {
        val q = _playbackQueue.value
        if (q.isNotEmpty()) {
            logPlayingHistorySegment()
            var nextIndex = _currentTrackIndex.value + 1
            if (nextIndex >= q.size) {
                nextIndex = 0 // Loop queue
            }
            _currentTrackIndex.value = nextIndex
            audioEngine.playTrack(q[nextIndex])
        }
    }

    fun playPrevious() {
        val q = _playbackQueue.value
        if (q.isNotEmpty()) {
            logPlayingHistorySegment()
            var prevIndex = _currentTrackIndex.value - 1
            if (prevIndex < 0) {
                prevIndex = q.size - 1 // Loop backwards
            }
            _currentTrackIndex.value = prevIndex
            audioEngine.playTrack(q[prevIndex])
        }
    }

    private fun logPlayingHistorySegment() {
        val track = audioEngine.currentTrack.value ?: return
        val pos = audioEngine.currentPositionMs.value
        if (pos > 1000) {
            viewModelScope.launch {
                repository.logPlaybackHistory(track.id, pos)
            }
        }
    }

    // --- RE-SORTING / DRAGGING QUEUE CONVENIENCE ---
    fun swapQueueItems(fromIndex: Int, toIndex: Int) {
        val list = _playbackQueue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _playbackQueue.value = list
            if (_currentTrackIndex.value == fromIndex) {
                _currentTrackIndex.value = toIndex
            } else if (_currentTrackIndex.value in fromIndex..toIndex || _currentTrackIndex.value in toIndex..fromIndex) {
                // Adjust index tracker
                val currentTrack = audioEngine.currentTrack.value
                _currentTrackIndex.value = list.indexOfFirst { it.id == currentTrack?.id }
            }
        }
    }

    // --- EQUALIZER PRESETS CONTROLLER ---
    fun toggleEqBandLayout() {
        _eqIsTenBand.value = !_eqIsTenBand.value
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        val gains = when (presetName) {
            "Rock" -> floatArrayOf(4f, 2f, -1f, 3f, 5f)
            "Pop" -> floatArrayOf(-1f, 2f, 4f, 1f, -2f)
            "Jazz" -> floatArrayOf(3f, 1f, -2f, 2f, 4f)
            "Bass Booster" -> floatArrayOf(6f, 4f, 0f, 0f, -3f)
            "Classic" -> floatArrayOf(2f, 1f, 0f, 2f, 3f)
            else -> floatArrayOf(0f, 0f, 0f, 0f, 0f) // Normal
        }
        for (i in gains.indices) {
            audioEngine.setEqBandGain(i, gains[i])
        }
    }

    // --- DIRECT SEEDING FOR RE-SCAN ---
    fun triggerLibraryAutoScan() {
        viewModelScope.launch {
            repository.scanLocalMediaAndSeed()
        }
    }

    // --- PLAYLIST MODIFIERS ---
    fun selectActivePlaylist(playlist: Playlist?) {
        _activePlaylist.value = playlist
        if (playlist != null) {
            viewModelScope.launch {
                repository.getTracksForPlaylist(playlist.id).collect {
                    _activePlaylistTracks.value = it
                }
            }
        } else {
            _activePlaylistTracks.value = emptyList()
        }
    }

    fun createCustomPlaylist(name: String, description: String) {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deleteCustomPlaylist(playlistId: Long) {
        viewModelScope.launch {
            if (_activePlaylist.value?.id == playlistId) {
                _activePlaylist.value = null
            }
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            // Refresh currently selected view
            val active = _activePlaylist.value
            if (active != null && active.id == playlistId) {
                selectActivePlaylist(active)
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            // Refresh currently selected view
            val active = _activePlaylist.value
            if (active != null && active.id == playlistId) {
                selectActivePlaylist(active)
            }
        }
    }

    suspend fun toggleFavoriteTrack(trackId: Long) {
        repository.toggleFavorite(trackId)
    }

    // --- UTILS ---
    private fun parseLrc(lrcText: String?): List<LyricLine> {
        if (lrcText.isNullOrBlank()) return emptyList()
        val lines = lrcText.split("\n")
        val result = mutableListOf<LyricLine>()
        // Match formatting: [00:00.00] text
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)")
        
        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                try {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val ms = match.groupValues[3].toLong() * 10 // Convert 2-digit ms centiseconds to milliseconds
                    val timeMs = (min * 60 + sec) * 1000 + ms
                    val text = match.groupValues[4].trim()
                    result.add(LyricLine(timeMs, text))
                } catch (e: Exception) {
                    // skip malformed lines safely
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
