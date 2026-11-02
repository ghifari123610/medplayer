package com.example.ui

/**
 * Tab sumber audio untuk memfilter antara semua lagu, lagu lokal di perangkat,
 * atau katalog streaming kurasi.
 */
enum class AudioSourceTab {
    ALL,
    LOCAL_DEVICE,
    CURATED_STREAM
}

/**
 * State tambahan untuk UI kontrol seperti sleep timer dan simulasi telepon.
 */
data class UiExtraState(
    val isSimulatingPhoneCall: Boolean = false,
    val sleepTimerMinutesRemaining: Int? = null,
    val simulatedFocusMessage: String? = null
)
