package com.example.data.repository

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository untuk memindai file audio lokal dari penyimpanan perangkat
 * menggunakan ContentResolver dan MediaStore.Audio.Media.
 */
class LocalAudioRepository(
    private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

        /**
         * Mengembalikan string permission yang sesuai dengan versi Android runtime:
         * - Android 13+ (API 33): READ_MEDIA_AUDIO
         * - Android 12 ke bawah (API <= 32): READ_EXTERNAL_STORAGE
         */
        fun getRequiredAudioPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
    }

    /**
     * Memeriksa apakah izin akses audio/storage sudah diberikan oleh pengguna.
     */
    fun hasAudioPermission(): Boolean {
        val requiredPermission = getRequiredAudioPermission()
        return ContextCompat.checkSelfPermission(
            context,
            requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Mengamati dan memindai audio lokal secara reaktif menggunakan Flow dan ContentObserver.
     * Setiap kali file audio ditambahkan atau dihapus di penyimpanan perangkat,
     * ContentObserver akan memicu pemindaian ulang dan memancarkan list AudioTrack terbaru.
     */
    fun getLocalAudioTracks(): Flow<List<AudioTrack>> = callbackFlow {
        // Query pertama kali saat flow dikoleksi
        val initialTracks = queryAudioFromMediaStore()
        trySend(initialTracks)

        // Daftarkan ContentObserver untuk memantau perubahan pada MediaStore audio
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Pindai ulang saat ada perubahan file audio di perangkat
                val updatedTracks = queryAudioFromMediaStore()
                trySend(updatedTracks)
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        // Hapus observer saat flow dibatalkan/ditutup
        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Melakukan kueri langsung ke MediaStore.Audio.Media untuk mengekstrak metadata lengkap:
     * - Title
     * - Artist
     * - Album
     * - Duration
     * - Path (Content URI)
     * - Album Art URI
     */
    suspend fun scanAudioTracks(): List<AudioTrack> = withContext(Dispatchers.IO) {
        queryAudioFromMediaStore()
    }

    private fun queryAudioFromMediaStore(): List<AudioTrack> {
        if (!hasAudioPermission()) {
            return emptyList()
        }

        val trackList = mutableListOf<AudioTrack>()

        // Kolom proyeksi metadata yang dibutuhkan
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Filter hanya file musik (IS_MUSIC != 0) dan durasi lebih dari 5 detik untuk mengabaikan ringtone pendek
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn)
                    val rawArtist = cursor.getString(artistColumn)
                    val rawAlbum = cursor.getString(albumColumn)
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val filePath = cursor.getString(dataColumn) ?: ""
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/mpeg"

                    // Bangun Content URI pemutaran resmi Android
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    // Bangun Album Art URI berdasarkan album ID
                    val albumArtUri = ContentUris.withAppendedId(
                        ALBUM_ART_URI,
                        albumId
                    )

                    // Normalisasi teks metadata jika unknown
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else extractFileName(filePath)
                    val artist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist else "Unknown Artist"
                    val album = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") rawAlbum else "Unknown Album"

                    // Deteksi format audio
                    val formatLabel = when {
                        mimeType.contains("flac", ignoreCase = true) || filePath.endsWith(".flac", ignoreCase = true) -> "FLAC Lossless"
                        mimeType.contains("wav", ignoreCase = true) || filePath.endsWith(".wav", ignoreCase = true) -> "WAV Studio"
                        mimeType.contains("aac", ignoreCase = true) || filePath.endsWith(".m4a", ignoreCase = true) -> "AAC Audio"
                        mimeType.contains("ogg", ignoreCase = true) -> "OGG Vorbis"
                        else -> "MP3 Audio"
                    }

                    trackList.add(
                        AudioTrack(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            mediaUri = contentUri.toString(),
                            artworkUrl = albumArtUri.toString(),
                            genre = "Local Audio",
                            audioQuality = formatLabel
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return trackList
    }

    private fun extractFileName(path: String): String {
        return try {
            val fileName = path.substringAfterLast('/')
            if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        } catch (e: Exception) {
            "Audio File"
        }
    }
}
