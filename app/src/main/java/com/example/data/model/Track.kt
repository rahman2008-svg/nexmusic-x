package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String, // "simulated://..." or file path
    val genre: String,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val addedTime: Long = System.currentTimeMillis(),
    val lyrics: String? = null, // Sync lrc or plain lyrics
    val mood: String = "Relax" // Relax, Focus, Workout, Sleep
) {
    // Helper to check if it fits the Smart Collections rules
    fun isShort(): Boolean = durationMs < 60_000 // < 1 minute
    fun isLong(): Boolean = durationMs > 300_000 // > 5 minutes
    fun isNew(): Boolean = (System.currentTimeMillis() - addedTime) < 7 * 24 * 60 * 60 * 1000 // added in last week
    fun isOld(): Boolean = !isNew()
}
