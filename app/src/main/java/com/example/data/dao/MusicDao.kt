package com.example.data.dao

import androidx.room.*
import com.example.data.model.PlaybackHistory
import com.example.data.model.Playlist
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // --- Track Queries ---
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY addedTime DESC")
    fun getRecentlyAdded(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 20")
    fun getMostPlayed(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE mood = :mood")
    fun getTracksByMood(mood: String): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Delete
    suspend fun deleteTrack(track: Track)

    // --- Playlist Queries ---
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_cross_ref r ON t.id = r.trackId
        WHERE r.playlistId = :playlistId
        ORDER BY t.title ASC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>>

    // --- History & Stats Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistory)

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC")
    fun getPlaybackHistory(): Flow<List<PlaybackHistory>>

    @Query("SELECT SUM(durationPlayedMs) FROM playback_history")
    fun getTotalListeningTimeMs(): Flow<Long?>

    @Query("SELECT artist, COUNT(*) as play_count FROM tracks GROUP BY artist ORDER BY play_count DESC LIMIT 5")
    fun getFavoriteArtists(): Flow<List<ArtistStat>>
}

data class ArtistStat(
    val artist: String,
    val play_count: Int
)
