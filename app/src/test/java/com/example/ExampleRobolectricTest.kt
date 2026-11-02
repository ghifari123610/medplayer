package com.example

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AudioTrack
import com.example.data.repository.LocalAudioRepository
import com.example.playback.EqualizerController
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Audio", appName)
  }

  @Test
  fun `test time formatting utility`() {
    assertEquals("00:00", TimeUtils.formatDuration(0L))
    assertEquals("01:05", TimeUtils.formatDuration(65000L))
    assertEquals("03:30", TimeUtils.formatDuration(210000L))
  }

  @Test
  fun `sample tracks are valid and convert to MediaItem`() {
    assertTrue(AudioTrack.sampleTracks.isNotEmpty())
    val first = AudioTrack.sampleTracks.first()
    val mediaItem = first.toMediaItem()
    assertNotNull(mediaItem.mediaId)
    assertEquals(first.id, mediaItem.mediaId)
    assertEquals(first.title, mediaItem.mediaMetadata.title.toString())
  }

  @Test
  fun `local audio repository verifies permission requirement for Android 13 plus`() {
    val expectedPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_AUDIO
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
    assertEquals(expectedPermission, LocalAudioRepository.getRequiredAudioPermission())
  }

  @Test
  fun `local audio repository flow emits initial list`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LocalAudioRepository(context)
    val tracks = repo.getLocalAudioTracks().first()
    assertNotNull(tracks)
  }

  @Test
  fun `equalizer controller operations and state updates`() {
    val controller = EqualizerController()
    controller.bindToAudioSession(1)

    // Test enable/disable
    controller.setEnabled(true)
    assertTrue(controller.isEnabled())

    // Test bands
    val bands = controller.getNumberOfBands()
    assertTrue(bands >= 0)

    val (minMb, maxMb) = controller.getBandLevelRange()
    assertTrue(minMb <= maxMb)

    // Test setting band level
    controller.setBandLevel(0, 300)
    val state = controller.uiState.value
    assertTrue(state.isEnabled)

    // Test BassBoost
    controller.setBassBoostEnabled(true)
    controller.setBassBoostStrength(500)
    assertEquals(500.toShort(), controller.uiState.value.bassBoostStrength)

    // Test Virtualizer
    controller.setVirtualizerEnabled(true)
    controller.setVirtualizerStrength(400)
    assertEquals(400.toShort(), controller.uiState.value.virtualizerStrength)

    // Test presets
    val presets = controller.getPresets()
    assertNotNull(presets)
    if (presets.isNotEmpty()) {
      controller.usePreset(0)
      assertEquals(0.toShort(), controller.uiState.value.currentPreset)
    }

    controller.release()
  }

  @Test
  fun `main view model exposes reactive uiState and methods`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.MainViewModel(application)
    assertNotNull(viewModel.uiState.value)
    assertNotNull(viewModel.playlist.value)
    assertNotNull(viewModel.equalizerBands.value)
    assertNotNull(viewModel.equalizerPresets.value)
  }
}
