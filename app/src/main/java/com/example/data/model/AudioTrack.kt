package com.example.data.model

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String,
    val artworkUrl: String,
    val genre: String,
    val audioQuality: String = "Hi-Res Audio • 320 kbps"
) {
    fun toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putString("AUDIO_QUALITY", audioQuality)
            putString("GENRE", genre)
            putLong("DURATION_MS", durationMs)
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(mediaUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(Uri.parse(artworkUrl))
                    .setGenre(genre)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    companion object {
        fun fromMediaItem(item: MediaItem, fallbackDuration: Long = 0L): AudioTrack {
            val meta = item.mediaMetadata
            val extras = meta.extras
            val quality = extras?.getString("AUDIO_QUALITY") ?: "Standard 320 kbps"
            val genre = meta.genre?.toString() ?: extras?.getString("GENRE") ?: "Music"
            val duration = extras?.getLong("DURATION_MS") ?: fallbackDuration

            return AudioTrack(
                id = item.mediaId,
                title = meta.title?.toString() ?: "Unknown Title",
                artist = meta.artist?.toString() ?: "Unknown Artist",
                album = meta.albumTitle?.toString() ?: "Unknown Album",
                durationMs = duration,
                mediaUri = item.localConfiguration?.uri?.toString() ?: "",
                artworkUrl = meta.artworkUri?.toString() ?: "",
                genre = genre,
                audioQuality = quality
            )
        }

        val sampleTracks = listOf(
            AudioTrack(
                id = "track_1",
                title = "Jazz In Paris",
                artist = "Media Soundscapes",
                album = "Parisian Nights",
                durationMs = 184000L,
                mediaUri = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                genre = "Jazz",
                audioQuality = "FLAC 24-bit • 96 kHz"
            ),
            AudioTrack(
                id = "track_2",
                title = "Dizzying Groove",
                artist = "The Rhythm Collective",
                album = "Neon Velocity",
                durationMs = 135000L,
                mediaUri = "https://storage.googleapis.com/exoplayer-test-media-1/mp3/dizzy.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                genre = "Electronic",
                audioQuality = "Master Audio • 48 kHz"
            ),
            AudioTrack(
                id = "track_3",
                title = "Synthesized Horizon",
                artist = "Cyber Pulse",
                album = "Retro Waveform 2088",
                durationMs = 210000L,
                mediaUri = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                genre = "Synthwave",
                audioQuality = "Hi-Res Lossless • 320 kbps"
            ),
            AudioTrack(
                id = "track_4",
                title = "Midnight Odyssey",
                artist = "Aura Ensemble",
                album = "Chamber Echoes",
                durationMs = 176000L,
                mediaUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
                genre = "Cinematic",
                audioQuality = "Spatial Dolby • 24-bit"
            ),
            AudioTrack(
                id = "track_5",
                title = "Acoustic Rain",
                artist = "Luna Flores",
                album = "Organic Reverie",
                durationMs = 158000L,
                mediaUri = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=600&auto=format&fit=crop&q=80",
                genre = "Acoustic",
                audioQuality = "FLAC Studio • 96 kHz"
            )
        )
    }
}
