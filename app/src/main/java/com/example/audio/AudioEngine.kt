package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.example.data.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class AudioEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // State flows for real-time monitoring
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val playbackProgress: StateFlow<Float> = _playbackProgress

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    // Music Stats & Configuration
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    // EQ bands tracking State
    private val _eqEnabled = MutableStateFlow(true)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled

    // 10 key virtual frequencies for EQ UI (We'll map our standard 5-band or adapt with smooth curves)
    private val _eqBands = MutableStateFlow(floatArrayOf(0f, 0f, 0f, 0f, 0f)) // -15dB to +15dB
    val eqBands: StateFlow<FloatArray> = _eqBands

    private val _bassBoostLevel = MutableStateFlow(0f) // 0 to 1000
    val bassBoostLevel: StateFlow<Float> = _bassBoostLevel

    private val _virtualizerLevel = MutableStateFlow(0f) // 0 to 1000
    val virtualizerLevel: StateFlow<Float> = _virtualizerLevel

    private val _loudnessBoost = MutableStateFlow(0f) // 0 to 2000 mB
    val loudnessBoost: StateFlow<Float> = _loudnessBoost

    private val _vocalBoostLevel = MutableStateFlow(0f) // Simulated voc. boost
    val vocalBoostLevel: StateFlow<Float> = _vocalBoostLevel

    // Sleep Timer tracking
    private val _sleepTimeRemainingSec = MutableStateFlow(0L)
    val sleepTimeRemainingSec: StateFlow<Long> = _sleepTimeRemainingSec

    private var sleepTimerJob: Job? = null
    private var progressTrackingJob: Job? = null

    private val sampleTracksPaths = HashMap<String, String>()

    init {
        // Pre-generate procedural WAV loops on thread pool
        scope.launch(Dispatchers.IO) {
            try {
                generateProceduralSongs()
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error generating placeholder tracks", e)
            }
        }
    }

    // --- PROCEDURAL AUDIO SINE SYNTHESIZATION ENGINE ---
    // Generates a proper, lightweight PCM offline wave file in app caching directory.
    private fun generateProceduralSongs() {
        // 1. Chill Lofi Loop: Melodic low synth chords, low BPM
        createWavFile("lofi_loop.wav", 85, audioType = "lofi")
        // 2. Deep Focus Waves: Alpha Binaural Waves paired with cosmic pad
        createWavFile("focus_waves.wav", 60, audioType = "focus")
        // 3. Workout Pulse: Trance-like heavy bass rhythm, high BPM
        createWavFile("workout_pulse.wav", 128, audioType = "workout")
        // 4. Sleep Cradle: Soothing 528Hz cosmic frequency sequence
        createWavFile("sleep_cradle.wav", 50, audioType = "sleep")
    }

    private fun createWavFile(filename: String, bpm: Int, audioType: String) {
        val file = File(context.cacheDir, filename)
        if (file.exists() && file.length() > 1000) {
            sampleTracksPaths[audioType] = file.absolutePath
            return
        }

        val sampleRate = 22050 // Lower sample rate for lightning-fast scanning and compact disk usage
        val durationSeconds = 12 // Small seamless loop file
        val numSamples = sampleRate * durationSeconds
        val numBytes = numSamples * 2 // 16-bit sound (2 bytes/sample)

        val headerSize = 44
        val totalSize = headerSize + numBytes

        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF Header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize - 8)
        buffer.put("WAVE".toByteArray())

        // Sub-chunk 1: Format definition
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Sub-chunk 1 size (16 for PCM)
        buffer.putShort(1) // Audio format: 1 (PCM uncompressed)
        buffer.putShort(1) // Mono channel
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2) // Byte rate (SampleRate * 1 channel * 2 bytes/sample)
        buffer.putShort(2) // Block align (1 channel * 2 bytes/sample)
        buffer.putShort(16) // Bits per sample

        // Sub-chunk 2: Audio data
        buffer.put("data".toByteArray())
        buffer.putInt(numBytes)

        // Audio generation based on categories
        val beatIntervalSamples = sampleRate * 60 / bpm
        for (i in 0 until numSamples) {
            var value = 0.0
            when (audioType) {
                "lofi" -> {
                    // Generative synth pad: combination of three harmonic frequencies (Major 7th chord)
                    val t = i.toDouble() / sampleRate
                    // Chord notes: C4 (261.6 Hz), E4 (329.6 Hz), G4 (392.0 Hz), B4 (493.9 Hz)
                    val chord = sin(2 * Math.PI * 261.6 * t) * 0.3 +
                                sin(2 * Math.PI * 329.6 * t) * 0.3 +
                                sin(2 * Math.PI * 392.0 * t) * 0.2 +
                                sin(2 * Math.PI * 493.9 * t) * 0.2
                    // Lofi background noise (simulated using light randomized waves)
                    val staticCrack = (Math.random() - 0.5) * 0.05
                    // Modulate to simulate a tremolo vinyl effect
                    val tremolo = 1.0 + 0.3 * sin(2 * Math.PI * 3.0 * t)
                    value = (chord * tremolo + staticCrack) * 0.65
                }
                "focus" -> {
                    // Deep focus binauaral: Left chord (110Hz) and Right chord (120Hz) - creates 10Hz alpha waves
                    val t = i.toDouble() / sampleRate
                    val drone = sin(2 * Math.PI * 110.0 * t) * 0.6 +
                                sin(2 * Math.PI * 165.0 * t) * 0.3 +
                                sin(2 * Math.PI * 55.0 * t) * 0.1
                    // Ambient slow sweep
                    val sweep = 0.5 + 0.5 * sin(2 * Math.PI * 0.1 * t)
                    value = drone * sweep * 0.75
                }
                "workout" -> {
                    // Pulse Trance: Heavy 4-on-the-floor beat kick
                    val t = i.toDouble() / sampleRate
                    val sampleInBeat = i % beatIntervalSamples
                    val decay = Math.exp(-6.0 * sampleInBeat.toDouble() / beatIntervalSamples)
                    val kick = sin(2 * Math.PI * 55.0 * (1.0 - decay * 0.5) * (sampleInBeat.toDouble() / sampleRate)) * decay * 0.8
                    
                    // Fast pulsing pluck melody
                    val beatIndex = (i / beatIntervalSamples) % 8
                    val melodyFreq = when (beatIndex) {
                        0 -> 220.0; 2 -> 261.6; 4 -> 329.6; 6 -> 293.7; else -> 0.0
                    }
                    val pluck = if (melodyFreq > 0.0) {
                        val pluckSample = sampleInBeat % (beatIntervalSamples / 2)
                        val pluckDecay = Math.exp(-20.0 * pluckSample.toDouble() / beatIntervalSamples)
                        sin(2 * Math.PI * melodyFreq * (pluckSample.toDouble() / sampleRate)) * pluckDecay * 0.3
                    } else 0.0

                    value = (kick + pluck) * 0.7
                }
                "sleep" -> {
                    // Sleep cradle: slow, soothing sliding sine wave (Solfeggio 528 Hz)
                    val t = i.toDouble() / sampleRate
                    val core = sin(2 * Math.PI * 528.0 * t) * 0.6 + sin(2 * Math.PI * 264.0 * t) * 0.4
                    // Ultra-slow breathing swell (10-second circle)
                    val swell = 0.4 + 0.6 * sin(2 * Math.PI * 0.1 * t)
                    value = core * swell * 0.5
                }
                else -> {
                    val t = i.toDouble() / sampleRate
                    value = sin(2 * Math.PI * 440.0 * t) * 0.5
                }
            }

            // Clip the signal to safe PCM 16-bit range
            val sampleVal = (value * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
            buffer.putShort(sampleVal)
        }

        try {
            FileOutputStream(file).use { out ->
                out.write(buffer.array())
            }
            sampleTracksPaths[audioType] = file.absolutePath
            Log.d("AudioEngine", "Successfully synthesized procedural loop: $filename ($audioType) at ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to write WAV file $filename", e)
        }
    }

    fun getFilePathForType(type: String): String {
        return sampleTracksPaths[type] ?: ""
    }

    // --- PLAYBACK CONTROLLER ---
    fun playTrack(track: Track, startPositionMs: Long = 0L) {
        scope.launch(Dispatchers.Main) {
            stopCurrentTrack()

            try {
                _currentTrack.value = track
                val actualPath = if (track.path.startsWith("simulated://")) {
                    val type = track.path.substringAfter("simulated://")
                    getFilePathForType(type)
                } else {
                    track.path
                }

                if (actualPath.isEmpty() || !File(actualPath).exists()) {
                    Log.e("AudioEngine", "Path does not exist, can't play: $actualPath")
                    return@launch
                }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(actualPath)
                    isLooping = true // Music Player is continuous loop in default player screen
                    prepare()
                    if (startPositionMs > 0) {
                        seekTo(startPositionMs.toInt())
                    }
                    start()
                }

                _isPlaying.value = true
                applySpeed(_playbackSpeed.value)
                initializeAudioEffects()

                // Trigger statistics callbacks or progress updates
                startProgressTracker()

            } catch (e: Exception) {
                Log.e("AudioEngine", "Error playing media", e)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                progressTrackingJob?.cancel()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
            val duration = it.duration
            if (duration > 0) {
                _playbackProgress.value = positionMs.toFloat() / duration
            }
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let {
                try {
                    val params = it.playbackParams
                    params.speed = speed
                    it.playbackParams = params
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Failed to apply speed", e)
                }
            }
        }
    }

    fun setMute(muted: Boolean) {
        _isMuted.value = muted
        val vol = if (muted) 0f else 1f
        mediaPlayer?.setVolume(vol, vol)
    }

    private fun stopCurrentTrack() {
        progressTrackingJob?.cancel()
        releaseAudioEffects()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val current = mp.currentPosition.toLong()
                        val duration = mp.duration.toLong()
                        _currentPositionMs.value = current
                        if (duration > 0) {
                            _playbackProgress.value = current.toFloat() / duration
                        }
                    }
                }
                delay(250)
            }
        }
    }

    // --- SLEEP TIMER ---
    fun startSleepTimer(minutes: Int, onTimerFinished: () -> Unit) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimeRemainingSec.value = 0L
            return
        }

        _sleepTimeRemainingSec.value = minutes.toLong() * 60
        sleepTimerJob = scope.launch(Dispatchers.Main) {
            while (_sleepTimeRemainingSec.value > 0) {
                delay(1000)
                _sleepTimeRemainingSec.value -= 1
            }
            // Finished!
            pause()
            onTimerFinished()
        }
    }

    fun clearSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimeRemainingSec.value = 0L
    }

    // --- HIGH-FIDELITY PRO AUDIO EFFECTS API ---
    private fun initializeAudioEffects() {
        val mp = mediaPlayer ?: return
        val sessionId = mp.audioSessionId
        Log.d("AudioEngine", "Initializing native audio effects for AudioSession ID: $sessionId")

        try {
            // Equalizer Setup (5 bands typically)
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _eqEnabled.value
                // Load current database band gains
                applyEqBands()
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Equalizer effect hardware not found on this device", e)
        }

        try {
            // BassBoost Setup
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
                setStrength((_bassBoostLevel.value * 10).toInt().toShort()) // level * 10 (0 to 1000 range)
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "BassBoost effect hardware not found on this device", e)
        }

        try {
            // Virtualizer 3D Setup
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = true
                setStrength((_virtualizerLevel.value * 10).toInt().toShort()) // 0 to 1000 range
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Virtualizer effect hardware not found", e)
        }

        try {
            // Loudness Enhancer Setup
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    enabled = true
                    setTargetGain((_loudnessBoost.value).toInt()) // gain in mB
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "LoudnessEnhancer hardware not found", e)
        }
    }

    private fun releaseAudioEffects() {
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }

    fun toggleEq(enabled: Boolean) {
        _eqEnabled.value = enabled
        equalizer?.enabled = enabled
    }

    fun setEqBandGain(bandIndex: Int, gainValue: Float) {
        // gain is -15 to +15 dB
        if (bandIndex in _eqBands.value.indices) {
            val bands = _eqBands.value.clone()
            bands[bandIndex] = gainValue
            _eqBands.value = bands

            equalizer?.let { eq ->
                try {
                    if (bandIndex < eq.numberOfBands) {
                        val minLevel = eq.bandLevelRange[0]
                        val maxLevel = eq.bandLevelRange[1]
                        // Convert gain (-15 to 15) to level (usually millibels: -1500 to 1500)
                        val level = (gainValue * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                        eq.setBandLevel(bandIndex.toShort(), level.toShort())
                    }
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Error setting hardware EQ band gain", e)
                }
            }
        }
    }

    private fun applyEqBands() {
        equalizer?.let { eq ->
            try {
                val bands = _eqBands.value
                for (i in bands.indices) {
                    if (i < eq.numberOfBands) {
                        val minLevel = eq.bandLevelRange[0]
                        val maxLevel = eq.bandLevelRange[1]
                        val level = (bands[i] * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                        eq.setBandLevel(i.toShort(), level.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error applying native EQ presets", e)
            }
        }
    }

    fun setBassBoost(level: Float) {
        // incoming: 0f to 100f
        _bassBoostLevel.value = level
        try {
            bassBoost?.let {
                it.enabled = level > 0
                it.setStrength((level * 10).toInt().toShort()) // scale to 0-1000 millibels strength
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error setting hardware Bass Boost", e)
        }
    }

    fun setVirtualizer(level: Float) {
        // incoming: 0f to 100f
        _virtualizerLevel.value = level
        try {
            virtualizer?.let {
                it.enabled = level > 0
                it.setStrength((level * 10).toInt().toShort()) // scale to 0-1000
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error setting Virtualizer strength", e)
        }
    }

    fun setLoudnessEnhancer(level: Float) {
        // incoming: 0f to 100f -> mapped to 0 to 1000 mB gain
        _loudnessBoost.value = level * 10
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer?.let {
                    it.enabled = level > 0
                    it.setTargetGain((level * 10).toInt())
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error setting Loudness boost", e)
        }
    }

    fun setVocalBoost(level: Float) {
        // Equalizer mapping to boost vocal ranges (usually middle bands around 1kHz - 3kHz)
        _vocalBoostLevel.value = level
        // Boost midrange EQ band (band 2 and 3 in standard 5-band configuration)
        if (equalizer != null) {
            setEqBandGain(2, level * 0.15f) // Scale vocal boost into midrange EQ
            setEqBandGain(3, level * 0.15f)
        }
    }

    fun getDuration(): Long {
        return mediaPlayer?.duration?.toLong() ?: 0L
    }

    fun release() {
        scope.cancel()
        stopCurrentTrack()
    }
}
