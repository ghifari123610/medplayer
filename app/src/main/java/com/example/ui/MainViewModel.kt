package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.data.model.AudioTrack
import com.example.data.repository.LocalAudioRepository
import com.example.playback.EqualizerBandInfo
import com.example.playback.EqualizerController
import com.example.playback.EqualizerUiState
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    // Playback State (Media3 Service & ExoPlayer)
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val currentTrack: AudioTrack? = null,
    val currentTrackIndex: Int = 0,
    val playbackState: Int = Player.STATE_IDLE,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1.0f,
    val audioFocusStatus: String = "Focus Ready",
    val isServiceConnected: Boolean = false,
    val audioSessionId: Int = 0,

    // Storage Scanner Repository & Playlist State
    val selectedSourceTab: AudioSourceTab = AudioSourceTab.ALL,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val playlist: List<AudioTrack> = emptyList(),
    val localTracks: List<AudioTrack> = emptyList(),
    val filteredTracks: List<AudioTrack> = emptyList(),
    val hasAudioPermission: Boolean = false,
    val isScanningLocal: Boolean = false,

    // Equalizer & AudioFX State (android.media.audiofx.Equalizer)
    val isEqualizerEnabled: Boolean = false,
    val equalizerBands: List<EqualizerBandInfo> = emptyList(),
    val equalizerPresets: List<String> = emptyList(),
    val currentPreset: Short = -1,
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500,
    val isBassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0,
    val isVirtualizerEnabled: Boolean = false,
    val virtualizerStrength: Short = 0,

    // UI Extra Controls
    val sleepTimerMinutesRemaining: Int? = null,
    val isSimulatingPhoneCall: Boolean = false,
    val simulatedFocusMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isBuffering: Boolean
        get() = playbackState == Player.STATE_BUFFERING
}

/**
 * MainViewModel yang menghubungkan:
 * 1. Media3 PlaybackService (ExoPlayer & MediaSession) via MusicController
 * 2. Storage Scanner Repository (LocalAudioRepository via MediaStore & ContentResolver)
 * 3. EqualizerController (Native Android Equalizer, BassBoost, Virtualizer)
 * ke antarmuka Jetpack Compose secara reaktif.
 */
open class MainViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Media3 Playback Controller
    val musicController: MusicController = MusicController(application.applicationContext)

    // 2. Local Audio Storage Scanner Repository
    val localAudioRepository: LocalAudioRepository = LocalAudioRepository(application.applicationContext)

    // 3. Native AudioFX Equalizer Controller (Shared Singleton Instance)
    val equalizerController: EqualizerController = EqualizerController.instance

    // Expose EqualizerUiState StateFlow for UI screens
    val equalizerState: StateFlow<EqualizerUiState> = equalizerController.uiState

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var simulatedFocusRequest: AudioFocusRequest? = null
    private var simulatedCallJob: Job? = null
    private var sleepTimerJob: Job? = null

    val categories = listOf("All", "Local Audio", "Jazz", "Electronic", "Synthwave", "Cinematic", "Acoustic")
    val sampleTracks: List<AudioTrack> = AudioTrack.sampleTracks

    // Internal UI filters & extra states
    private val _selectedTab = MutableStateFlow(AudioSourceTab.ALL)
    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _hasAudioPermission = MutableStateFlow(localAudioRepository.hasAudioPermission())
    private val _isScanningLocal = MutableStateFlow(false)
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    private val _isSimulatingPhoneCall = MutableStateFlow(false)
    private val _simulatedFocusMsg = MutableStateFlow<String?>(null)

    private data class FilterState(
        val tab: AudioSourceTab,
        val category: String,
        val query: String,
        val hasPermission: Boolean,
        val isScanning: Boolean
    )

    private val filterStateFlow: Flow<FilterState> = combine(
        _selectedTab,
        _selectedCategory,
        _searchQuery,
        _hasAudioPermission,
        _isScanningLocal
    ) { tab: AudioSourceTab, cat: String, query: String, perm: Boolean, scanning: Boolean ->
        FilterState(tab, cat, query, perm, scanning)
    }

    private val extraStateFlow: Flow<UiExtraState> = combine(
        _sleepTimerMinutes,
        _isSimulatingPhoneCall,
        _simulatedFocusMsg
    ) { timer: Int?, call: Boolean, msg: String? ->
        UiExtraState(
            isSimulatingPhoneCall = call,
            sleepTimerMinutesRemaining = timer,
            simulatedFocusMessage = msg
        )
    }

    val extraUiState: StateFlow<UiExtraState> = extraStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiExtraState())

    // Flow lagu lokal yang dipindai dari MediaStore
    val localTracks: StateFlow<List<AudioTrack>> = localAudioRepository.getLocalAudioTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State reaktif gabungan untuk seluruh UI Jetpack Compose
    val uiState: StateFlow<MainUiState> = combine(
        musicController.playbackState,
        localTracks,
        equalizerState,
        filterStateFlow,
        extraStateFlow
    ) { playback: PlaybackState, locals: List<AudioTrack>, eqState: EqualizerUiState, filter: FilterState, extra: UiExtraState ->
        val query = filter.query.trim().lowercase()
        val cat = filter.category
        val tab = filter.tab

        val baseList = when (tab) {
            AudioSourceTab.ALL -> sampleTracks + locals
            AudioSourceTab.LOCAL_DEVICE -> locals
            AudioSourceTab.CURATED_STREAM -> sampleTracks
        }

        val filtered = baseList.filter { track ->
            val matchesCategory = cat == "All" || track.genre.equals(cat, ignoreCase = true)
            val matchesQuery = query.isEmpty() ||
                track.title.lowercase().contains(query) ||
                track.artist.lowercase().contains(query) ||
                track.album.lowercase().contains(query)
            matchesCategory && matchesQuery
        }

        MainUiState(
            // Playback
            isPlaying = playback.isPlaying,
            currentPositionMs = playback.currentPositionMs,
            durationMs = playback.durationMs,
            bufferedPositionMs = playback.bufferedPositionMs,
            currentTrack = playback.currentTrack,
            currentTrackIndex = playback.currentMediaItemIndex,
            playbackState = playback.playbackState,
            isShuffleEnabled = playback.isShuffleEnabled,
            repeatMode = playback.repeatMode,
            playbackSpeed = playback.playbackSpeed,
            audioFocusStatus = playback.audioFocusStatus,
            isServiceConnected = playback.isServiceConnected,
            audioSessionId = playback.audioSessionId,

            // Playlist & Scanner
            selectedSourceTab = filter.tab,
            selectedCategory = filter.category,
            searchQuery = filter.query,
            playlist = playback.playlist.ifEmpty { filtered },
            localTracks = locals,
            filteredTracks = filtered,
            hasAudioPermission = filter.hasPermission,
            isScanningLocal = filter.isScanning,

            // Equalizer
            isEqualizerEnabled = eqState.isEnabled,
            equalizerBands = eqState.bands,
            equalizerPresets = eqState.presets,
            currentPreset = eqState.currentPreset,
            minLevelMb = eqState.minLevelMb,
            maxLevelMb = eqState.maxLevelMb,
            isBassBoostEnabled = eqState.isBassBoostEnabled,
            bassBoostStrength = eqState.bassBoostStrength,
            isVirtualizerEnabled = eqState.isVirtualizerEnabled,
            virtualizerStrength = eqState.virtualizerStrength,

            // Extra
            sleepTimerMinutesRemaining = extra.sleepTimerMinutesRemaining,
            isSimulatingPhoneCall = extra.isSimulatingPhoneCall,
            simulatedFocusMessage = extra.simulatedFocusMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    // Granular StateFlows untuk kemudahan observasi
    val currentPlayingTrack: StateFlow<AudioTrack?> = uiState.map { it.currentTrack }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentPosition: StateFlow<Long> = uiState.map { it.currentPositionMs }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val isPlaying: StateFlow<Boolean> = uiState.map { it.isPlaying }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val playlist: StateFlow<List<AudioTrack>> = uiState.map { it.filteredTracks }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sampleTracks)

    val equalizerBands: StateFlow<List<EqualizerBandInfo>> = uiState.map { it.equalizerBands }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val equalizerPresets: StateFlow<List<String>> = uiState.map { it.equalizerPresets }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Inisialisasi daftar putar awal dengan lagu sampel jika kosong
        viewModelScope.launch {
            delay(500)
            if (musicController.playbackState.value.playlist.isEmpty()) {
                musicController.setPlaylist(sampleTracks, startIndex = 0, playWhenReady = false)
            }
        }

        // Hubungkan EqualizerController secara otomatis saat audioSessionId dari ExoPlayer tersedia
        viewModelScope.launch {
            musicController.playbackState.collect { playback ->
                if (playback.audioSessionId != 0) {
                    equalizerController.bindToAudioSession(playback.audioSessionId)
                }
            }
        }
    }

    // --- Action Methods: Media3 Playback ---

    fun play() = musicController.play()

    fun pause() = musicController.pause()

    fun togglePlayPause() = musicController.togglePlayPause()

    fun seekTo(positionMs: Long) = musicController.seekTo(positionMs)

    fun seekRelative(offsetMs: Long) = musicController.seekRelative(offsetMs)

    fun skipToNext() = musicController.skipToNext()

    fun skipToPrevious() = musicController.skipToPrevious()

    fun toggleShuffle() = musicController.toggleShuffle()

    fun cycleRepeatMode() = musicController.cycleRepeatMode()

    fun setPlaybackSpeed(speed: Float) = musicController.setPlaybackSpeed(speed)

    fun playTrack(track: AudioTrack) {
        val currentTracks = uiState.value.filteredTracks
        val index = currentTracks.indexOfFirst { it.id == track.id }
        if (index != -1) {
            musicController.setPlaylist(currentTracks, startIndex = index, playWhenReady = true)
        }
    }

    fun playTrackAtIndex(index: Int) {
        musicController.playTrackAtIndex(index)
    }

    // --- Action Methods: Storage Scanner Repository ---

    fun selectSourceTab(tab: AudioSourceTab) {
        _selectedTab.value = tab
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onAudioPermissionResult(isGranted: Boolean) {
        _hasAudioPermission.value = isGranted
        if (isGranted) {
            scanLocalTracks()
        }
    }

    fun scanLocalTracks() {
        viewModelScope.launch {
            _isScanningLocal.value = true
            val scanned = localAudioRepository.scanAudioTracks()
            _isScanningLocal.value = false
            if (scanned.isNotEmpty() && _selectedTab.value == AudioSourceTab.LOCAL_DEVICE) {
                musicController.setPlaylist(scanned, startIndex = 0, playWhenReady = false)
            }
        }
    }

    // --- Action Methods: EqualizerController ---

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
    }

    fun setEqualizerBandLevel(band: Short, levelMb: Short) {
        equalizerController.setBandLevel(band, levelMb)
    }

    fun useEqualizerPreset(presetIndex: Short) {
        equalizerController.usePreset(presetIndex)
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        equalizerController.setBassBoostEnabled(enabled)
    }

    fun setBassBoostStrength(strength: Short) {
        equalizerController.setBassBoostStrength(strength)
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        equalizerController.setVirtualizerEnabled(enabled)
    }

    fun setVirtualizerStrength(strength: Short) {
        equalizerController.setVirtualizerStrength(strength)
    }

    // --- Action Methods: Sleep Timer & Audio Focus Simulation ---

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerMinutes.value = null
            return
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                _sleepTimerMinutes.value = remaining
                delay(60000L)
                remaining--
            }
            _sleepTimerMinutes.value = null
            musicController.pause()
        }
    }

    fun simulateIncomingCall(durationSeconds: Int = 6) {
        simulatedCallJob?.cancel()
        simulatedCallJob = viewModelScope.launch {
            _isSimulatingPhoneCall.value = true
            _simulatedFocusMsg.value = "Incoming Phone Call Simulation (Loss of Audio Focus)"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AndroidAudioAttributes.Builder()
                    .setUsage(AndroidAudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AndroidAudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { /* Focus change listener */ }
                    .build()
                simulatedFocusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            delay(durationSeconds * 1000L)
            abandonSimulatedFocus()
        }
    }

    fun abandonSimulatedFocus() {
        simulatedCallJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            simulatedFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            simulatedFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        _isSimulatingPhoneCall.value = false
        _simulatedFocusMsg.value = "Call Ended: Audio Focus Regained (ExoPlayer automatically resumes)"
    }

    override fun onCleared() {
        super.onCleared()
        abandonSimulatedFocus()
        musicController.release()
        equalizerController.release()
    }
}
