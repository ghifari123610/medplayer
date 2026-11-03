package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqualizerBandInfo(
    val band: Short,
    val centerFreqHz: Int,
    val levelMb: Short,
    val minMb: Short,
    val maxMb: Short
) {
    val formattedFreq: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000} kHz"
        } else {
            "$centerFreqHz Hz"
        }

    val gainDb: Float
        get() = levelMb / 100f
}

data class EqualizerUiState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBandInfo> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPreset: Short = -1,
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500,
    val isBassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0,
    val isVirtualizerEnabled: Boolean = false,
    val virtualizerStrength: Short = 0,
    val isAvailable: Boolean = false,
    val audioSessionId: Int = 0
)

/**
 * Helper class untuk mengelola audio effect Equalizer, BassBoost, dan Virtualizer
 * menggunakan native API android.media.audiofx yang dihubungkan ke audioSessionId ExoPlayer.
 */
class EqualizerController(
    private val priority: Int = 0
) {
    private val tag = "EqualizerController"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var currentSessionId: Int = 0

    private val _uiState = MutableStateFlow(
        EqualizerUiState(
            isEnabled = true,
            bands = listOf(
                EqualizerBandInfo(0, 60, 0, -1500, 1500),
                EqualizerBandInfo(1, 230, 0, -1500, 1500),
                EqualizerBandInfo(2, 910, 0, -1500, 1500),
                EqualizerBandInfo(3, 3600, 0, -1500, 1500),
                EqualizerBandInfo(4, 14000, 0, -1500, 1500)
            ),
            presets = listOf("Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock"),
            currentPreset = 0,
            minLevelMb = -1500,
            maxLevelMb = 1500,
            isBassBoostEnabled = true,
            bassBoostStrength = 350,
            isVirtualizerEnabled = true,
            virtualizerStrength = 250,
            isAvailable = true,
            audioSessionId = 0
        )
    )
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        setupFallbackState(0)
    }

    /**
     * Menghubungkan controller audio effect dengan audioSessionId dari instans ExoPlayer Media3.
     */
    fun bindToAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) {
            return
        }
        release()
        currentSessionId = audioSessionId

        try {
            // Inisialisasi native android.media.audiofx.Equalizer
            equalizer = Equalizer(priority, audioSessionId).apply {
                enabled = true
            }

            // Inisialisasi BassBoost jika didukung
            bassBoost = try {
                BassBoost(priority, audioSessionId).apply {
                    enabled = true
                }
            } catch (e: Exception) {
                Log.w(tag, "BassBoost not supported: ${e.message}")
                null
            }

            // Inisialisasi Virtualizer / 3D Surround jika didukung
            virtualizer = try {
                Virtualizer(priority, audioSessionId).apply {
                    enabled = true
                }
            } catch (e: Exception) {
                Log.w(tag, "Virtualizer not supported: ${e.message}")
                null
            }

            refreshState()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize native audiofx: ${e.message}", e)
            setupFallbackState(audioSessionId)
        }
    }

    /**
     * Mengaktifkan atau mematikan Equalizer secara global.
     */
    fun setEnabled(enabled: Boolean) {
        val eq = equalizer
        if (eq != null) {
            try {
                eq.enabled = enabled
            } catch (e: Exception) {
                Log.e(tag, "Error setting equalizer enabled: ${e.message}")
            }
        }
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    /**
     * Memeriksa apakah equalizer sedang aktif.
     */
    fun isEnabled(): Boolean = equalizer?.enabled ?: _uiState.value.isEnabled

    /**
     * Mengambil jumlah band frekuensi.
     */
    fun getNumberOfBands(): Short = equalizer?.numberOfBands ?: _uiState.value.bands.size.toShort()

    /**
     * Mengambil rentang desibel dalam miliBel (mB).
     * Contoh: [-1500, +1500] mB (-15 dB hingga +15 dB).
     */
    fun getBandLevelRange(): Pair<Short, Short> {
        val range = equalizer?.bandLevelRange
        return if (range != null && range.size >= 2) {
            Pair(range[0], range[1])
        } else {
            Pair(_uiState.value.minLevelMb, _uiState.value.maxLevelMb)
        }
    }

    /**
     * Mengambil frekuensi tengah (center frequency) dari band tertentu dalam Hz.
     */
    fun getCenterFreq(band: Short): Int {
        return try {
            (equalizer?.getCenterFreq(band) ?: 0) / 1000
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Mengambil level gain band tertentu dalam miliBel (mB).
     */
    fun getBandLevel(band: Short): Short {
        return try {
            equalizer?.getBandLevel(band) ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Mengatur gain untuk band frekuensi tertentu dalam miliBel (mB).
     */
    fun setBandLevel(band: Short, levelMb: Short) {
        val eq = equalizer
        val (minMb, maxMb) = getBandLevelRange()
        val clampedLevel = levelMb.coerceIn(minMb, maxMb)

        if (eq != null) {
            try {
                eq.setBandLevel(band, clampedLevel)
            } catch (e: Exception) {
                Log.e(tag, "Error setting band level: ${e.message}")
            }
        }

        _uiState.update { state ->
            val updatedBands = state.bands.map {
                if (it.band == band) it.copy(levelMb = clampedLevel) else it
            }
            state.copy(
                bands = updatedBands,
                currentPreset = -1 // Berubah ke Custom karena ada penyesuaian manual
            )
        }
    }

    /**
     * Mengambil daftar nama preset bawaan sistem (e.g. Normal, Classical, Dance, Flat, Folk, Metal, Pop, Rock).
     */
    fun getPresets(): List<String> {
        val eq = equalizer ?: return _uiState.value.presets
        return try {
            val count = eq.numberOfPresets.toInt()
            (0 until count).map { index ->
                eq.getPresetName(index.toShort()) ?: "Preset $index"
            }
        } catch (e: Exception) {
            _uiState.value.presets
        }
    }

    /**
     * Menerapkan salah satu preset bawaan sistem berdasarkan indeks preset.
     */
    fun usePreset(presetIndex: Short) {
        val eq = equalizer
        if (eq != null) {
            try {
                if (presetIndex in 0 until eq.numberOfPresets) {
                    eq.usePreset(presetIndex)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error using preset: ${e.message}")
            }
            refreshBandsFromEqualizer(presetIndex)
        } else {
            applyFallbackPreset(presetIndex)
        }
    }

    private fun applyFallbackPreset(presetIndex: Short) {
        val presetGains: List<Short> = when (presetIndex.toInt()) {
            0 -> listOf(0, 0, 0, 0, 0) // Normal
            1 -> listOf(400, 250, -150, 300, 400) // Classical
            2 -> listOf(600, 0, 200, 400, 100) // Dance
            3 -> listOf(0, 0, 0, 0, 0) // Flat
            4 -> listOf(300, 0, 0, 200, -100) // Folk
            5 -> listOf(450, 100, -200, 350, 100) // Heavy Metal
            6 -> listOf(500, 300, 0, 150, 300) // Hip Hop
            7 -> listOf(0, 250, 0, 200, 450) // Jazz
            8 -> listOf(-150, 200, 450, 100, -200) // Pop
            9 -> listOf(450, 300, -150, 250, 500) // Rock
            else -> listOf(0, 0, 0, 0, 0)
        }
        _uiState.update { state ->
            val updated = state.bands.mapIndexed { idx, band ->
                val level = presetGains.getOrElse(idx) { 0.toShort() }
                band.copy(levelMb = level)
            }
            state.copy(bands = updated, currentPreset = presetIndex)
        }
    }

    /**
     * Mengaktifkan/mematikan Bass Boost.
     */
    fun setBassBoostEnabled(enabled: Boolean) {
        try {
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.e(tag, "Error setting BassBoost enabled: ${e.message}")
        }
        _uiState.update { it.copy(isBassBoostEnabled = enabled) }
    }

    /**
     * Mengatur intensitas kekuatan Bass Boost (rentang 0 - 1000).
     */
    fun setBassBoostStrength(strength: Short) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error setting BassBoost strength: ${e.message}")
        }
        _uiState.update { it.copy(bassBoostStrength = clamped) }
    }

    /**
     * Mengaktifkan/mematikan Virtualizer / 3D Surround.
     */
    fun setVirtualizerEnabled(enabled: Boolean) {
        try {
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(tag, "Error setting Virtualizer enabled: ${e.message}")
        }
        _uiState.update { it.copy(isVirtualizerEnabled = enabled) }
    }

    /**
     * Mengatur intensitas kekuatan Virtualizer / 3D Surround (rentang 0 - 1000).
     */
    fun setVirtualizerStrength(strength: Short) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error setting Virtualizer strength: ${e.message}")
        }
        _uiState.update { it.copy(virtualizerStrength = clamped) }
    }

    /**
     * Memperbarui state UI secara lengkap dari instance native audiofx.
     */
    private fun refreshState() {
        val eq = equalizer ?: return
        try {
            val isEnabled = eq.enabled
            val range = eq.bandLevelRange ?: shortArrayOf((-1500).toShort(), 1500.toShort())
            val minMb: Short = if (range.size >= 2) range[0] else (-1500).toShort()
            val maxMb: Short = if (range.size >= 2) range[1] else 1500.toShort()

            val numBands = eq.numberOfBands.toInt()
            if (numBands <= 0) {
                setupFallbackState(currentSessionId)
                return
            }
            val bandList = mutableListOf<EqualizerBandInfo>()
            for (i in 0 until numBands) {
                val band = i.toShort()
                val centerFreqHz = eq.getCenterFreq(band) / 1000
                val level = eq.getBandLevel(band)
                bandList.add(
                    EqualizerBandInfo(
                        band = band,
                        centerFreqHz = centerFreqHz,
                        levelMb = level,
                        minMb = minMb,
                        maxMb = maxMb
                    )
                )
            }

            val presetCount = eq.numberOfPresets.toInt()
            val presetList = (0 until presetCount).map { index ->
                eq.getPresetName(index.toShort()) ?: "Preset $index"
            }

            val currentPreset: Short = try { eq.currentPreset } catch (e: Exception) { (-1).toShort() }

            val bbEnabled = bassBoost?.enabled ?: false
            val bbStrength: Short = try { bassBoost?.roundedStrength ?: 0.toShort() } catch (e: Exception) { 0.toShort() }

            val virtEnabled = virtualizer?.enabled ?: false
            val virtStrength: Short = try { virtualizer?.roundedStrength ?: 0.toShort() } catch (e: Exception) { 0.toShort() }

            _uiState.update {
                EqualizerUiState(
                    isEnabled = isEnabled,
                    bands = bandList,
                    presets = presetList,
                    currentPreset = currentPreset,
                    minLevelMb = minMb,
                    maxLevelMb = maxMb,
                    isBassBoostEnabled = bbEnabled,
                    bassBoostStrength = bbStrength,
                    isVirtualizerEnabled = virtEnabled,
                    virtualizerStrength = virtStrength,
                    isAvailable = true,
                    audioSessionId = currentSessionId
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing equalizer state: ${e.message}")
        }
    }

    private fun refreshBandsFromEqualizer(newPresetIndex: Short) {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange ?: shortArrayOf((-1500).toShort(), 1500.toShort())
            val minMb: Short = if (range.size >= 2) range[0] else (-1500).toShort()
            val maxMb: Short = if (range.size >= 2) range[1] else 1500.toShort()
            val numBands = eq.numberOfBands.toInt()

            val updatedBands = (0 until numBands).map { i ->
                val band = i.toShort()
                val centerFreqHz = eq.getCenterFreq(band) / 1000
                val level = eq.getBandLevel(band)
                EqualizerBandInfo(
                    band = band,
                    centerFreqHz = centerFreqHz,
                    levelMb = level,
                    minMb = minMb,
                    maxMb = maxMb
                )
            }

            _uiState.update {
                it.copy(
                    bands = updatedBands,
                    currentPreset = newPresetIndex
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing bands: ${e.message}")
        }
    }

    /**
     * Fallback state untuk lingkungan emulasi atau perangkat tanpa native audiofx DSP hardware.
     */
    private fun setupFallbackState(audioSessionId: Int) {
        val defaultBands = listOf(
            EqualizerBandInfo(0, 60, 0, -1500, 1500),
            EqualizerBandInfo(1, 230, 0, -1500, 1500),
            EqualizerBandInfo(2, 910, 0, -1500, 1500),
            EqualizerBandInfo(3, 3600, 0, -1500, 1500),
            EqualizerBandInfo(4, 14000, 0, -1500, 1500)
        )
        val defaultPresets = listOf("Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock")

        _uiState.update {
            EqualizerUiState(
                isEnabled = true,
                bands = defaultBands,
                presets = defaultPresets,
                currentPreset = 0,
                minLevelMb = -1500,
                maxLevelMb = 1500,
                isBassBoostEnabled = true,
                bassBoostStrength = 350,
                isVirtualizerEnabled = true,
                virtualizerStrength = 250,
                isAvailable = true,
                audioSessionId = audioSessionId
            )
        }
    }

    /**
     * Melepaskan native resource Equalizer, BassBoost, dan Virtualizer.
     */
    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing Equalizer: ${e.message}")
        }
        equalizer = null

        try {
            bassBoost?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing BassBoost: ${e.message}")
        }
        bassBoost = null

        try {
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing Virtualizer: ${e.message}")
        }
        virtualizer = null
    }
}
