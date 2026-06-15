package com.example.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.dao.ArtistStat
import com.example.data.dao.MusicDao
import com.example.data.model.PlaybackHistory
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {
    val allTracks: Flow<List<Track>> = musicDao.getAllTracks()
    val favoriteTracks: Flow<List<Track>> = musicDao.getFavoriteTracks()
    val recentlyAdded: Flow<List<Track>> = musicDao.getRecentlyAdded()
    val mostPlayed: Flow<List<Track>> = musicDao.getMostPlayed()
    val playlists: Flow<List<Playlist>> = musicDao.getAllPlaylists()
    val totalTimeMs: Flow<Long?> = musicDao.getTotalListeningTimeMs()
    val favoriteArtists: Flow<List<ArtistStat>> = musicDao.getFavoriteArtists()
    val currentHistory: Flow<List<PlaybackHistory>> = musicDao.getPlaybackHistory()

    suspend fun getTrackById(id: Long): Track? {
        return musicDao.getTrackById(id)
    }

    suspend fun toggleFavorite(trackId: Long) {
        val track = musicDao.getTrackById(trackId)
        if (track != null) {
            val updated = track.copy(isFavorite = !track.isFavorite)
            musicDao.updateTrack(updated)
        }
    }

    suspend fun incrementPlayCount(trackId: Long) {
        val track = musicDao.getTrackById(trackId)
        if (track != null) {
            val updated = track.copy(playCount = track.playCount + 1)
            musicDao.updateTrack(updated)
        }
    }

    suspend fun logPlaybackHistory(trackId: Long, durationMs: Long) {
        val history = PlaybackHistory(trackId = trackId, durationPlayedMs = durationMs)
        musicDao.insertHistory(history)
    }

    // --- PLAYLIST MANAGERS ---
    suspend fun createPlaylist(name: String, description: String = "", isSystem: Boolean = false): Long {
        val playlist = Playlist(name = name, description = description, isSystemCreated = isSystem)
        return musicDao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return musicDao.getTracksForPlaylist(playlistId)
    }

    fun getTracksByMood(mood: String): Flow<List<Track>> {
        return musicDao.getTracksByMood(mood)
    }

    // --- AUTO SCAN OFF-LINE TRACKS & PRE-POPULATE SYSTEM ---
    suspend fun scanLocalMediaAndSeed() = withContext(Dispatchers.IO) {
        // 1. Seed or re-seed procedural music first
        seedProceduralTracks()

        // 2. Scan external storage for actual files (if permission granted and files exist)
        try {
            val resolver = context.contentResolver
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )

            // Only filter music
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            resolver.query(uri, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                val scannedTracks = mutableListOf<Track>()

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val path = cursor.getString(dataCol) ?: ""

                    // Map randomly into general music genres/categories
                    val genre = when {
                        title.contains("chill", true) -> "Lofi"
                        title.contains("rock", true) -> "Rock"
                        title.contains("pop", true) -> "Pop"
                        else -> "Classical"
                    }

                    val mood = when {
                        duration < 120_000 -> "Sleep"
                        duration > 300_000 -> "Focus"
                        else -> "Relax"
                    }

                    scannedTracks.add(
                        Track(
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            path = path,
                            genre = genre,
                            mood = mood
                        )
                    )
                }

                if (scannedTracks.isNotEmpty()) {
                    musicDao.insertTracks(scannedTracks)
                    Log.d("MusicRepository", "Scanned and inserted ${scannedTracks.size} user songs!")
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Skip storage scan or error reading contentResolver", e)
        }
    }

    suspend fun seedProceduralTracks() {
        val tracksList = listOf(
            Track(
                id = 1,
                title = "Chill Lofi Synth Loop",
                artist = "Prince AR Abdur Rahman",
                album = "NexMusic Chill Tape",
                durationMs = 120_000,
                path = "simulated://lofi",
                genre = "Lofi",
                mood = "Relax",
                lyrics = """
                    [00:00.00] ♫ NexMusic X - Chill Lofi Synth Loop
                    [00:02.00] ♫ (Relax and breathe in...)
                    [00:04.00] ♫ High audiophile sound warm low-pass filters active...
                    [00:08.00] ♫ Fully offline... No login... No cloud required.
                    [00:12.00] ♫ This represents the finest craftsmanship of Prince AR Abdur Rahman!
                    [00:16.00] ♫ Enjoy the vinyl warmth on your device.
                    [00:20.00] ♫ Adjust the EQ 10-band sliders to modify the vibe.
                    [00:24.00] ♫ NexVora Lab's Ofc, powering NexPlay X ecosystem!
                    [00:28.00] ♫ (Breathe out slowly... let the static sweep roll...)
                    [00:32.00] ♫ ♫ ♫ ♫ (Ambient melodies repeating...)
                """.trimIndent()
            ),
            Track(
                id = 2,
                title = "Deep Focus Waves",
                artist = "NexVora Lab's Ofc",
                album = "Study AI Ambient",
                durationMs = 360_000, // 6 minutes (Long track)
                path = "simulated://focus",
                genre = "Ambient",
                mood = "Focus",
                lyrics = """
                    [00:00.00] ♫ Deep Focus Waves: SOLFEGGIO & ALPHA BINAURAL
                    [00:03.00] ♫ Designed for intense programming, studying, or planning.
                    [00:06.00] ♫ (Alpha binaural frequencies stimulation...)
                    [00:10.00] ♫ Keep your focus 100%. No account, 100% private.
                    [00:14.00] ♫ Music statistics are computed securely purely offline.
                    [00:18.00] ♫ Adjust Bass Boost and Treble sliders for deep hum notes.
                    [00:22.00] ♫ Study AI - Smart Day Planner X integration concept...
                    [00:26.00] ♫ Just pure, raw, relaxing waves.
                """.trimIndent()
            ),
            Track(
                id = 3,
                title = "Workout Pulse Trance",
                artist = "Prince AR Abdur Rahman",
                album = "Ecosystem Beats",
                durationMs = 180_000,
                path = "simulated://workout",
                genre = "Trance",
                mood = "Workout",
                lyrics = """
                    [00:00.00] ♫ Workout Pulse: Energy Trance Beat (128 BPM)
                    [00:01.50] * Heavy Bass Pulse activated! *
                    [00:03.00] ♫ Drive your workout! No limitations, no buffering!
                    [00:05.00] * High-tempo synth plucking activated *
                    [00:06.50] ♫ Feel your streak! Track your statistics!
                    [00:09.00] ♫ Dynamic audio buffers, minimal latency, extreme efficiency.
                    [00:12.00] ♫ Boost Vocal and Loudness parameters for maximum drive!
                    [00:16.00] ♫ LifeSphere OS, NexPlay X, and NexLens Studio X!
                    [00:20.00] ♫ Push 10 more reps! Accelerate speed to 1.5x speed!
                """.trimIndent()
            ),
            Track(
                id = 4,
                title = "Cosmic Sleep Cradle",
                artist = "NexVora Lab's Ofc",
                album = "LifeSphere Resonance",
                durationMs = 55_000, // 55 seconds (Short track)
                path = "simulated://sleep",
                genre = "Cosmic",
                mood = "Sleep",
                lyrics = """
                    [00:00.00] ♫ Cosmic Sleep Cradle (528 Hz Solfeggio Scale)
                    [00:03.00] ♫ (Slow down your chest... deep, warm breath...)
                    [00:06.00] ♫ Let go of the Day... Enter the LifeSphere OS sleep cycle.
                    [00:10.00] ♫ Toggle the 30-minute Sleep Timer in the top drawer.
                    [00:14.00] ♫ Pure offline harmony, zero cloud emissions.
                    [00:18.00] ♫ Rest well, Prince. NexVora Lab's is keeping the watch.
                    [00:22.00] ♫ (Goodnight...)
                """.trimIndent()
            )
        )

        musicDao.insertTracks(tracksList)

        // Seed some system playlists if empty
        val defaultPlaylists = listOf(
            Playlist(id = 1, name = "Study Playlist", description = "Sharpen your focus", isSystemCreated = true),
            Playlist(id = 2, name = "Workout Playlist", description = "High-energy training loops", isSystemCreated = true),
            Playlist(id = 3, name = "Travel Playlist", description = "On-the-road soundtracks", isSystemCreated = true),
            Playlist(id = 4, name = "Sleep Playlist", description = "Soft cosmic soundscapes", isSystemCreated = true)
        )

        for (pl in defaultPlaylists) {
            musicDao.insertPlaylist(pl)
        }

        // Add corresponding tracks to these playlists
        musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(1, 2)) // Focus wave to Study Playlist
        musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(2, 3)) // Workout Pulse to Workout Playlist
        musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(4, 4)) // Sleep Cradle to Sleep Playlist
    }
}
