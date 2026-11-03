# 📌 CHECKPOINT PROYEK: Aura Audio (MedPlayer)

Dokumen ini mencatat ringkasan status perkembangan, arsitektur, fitur yang telah diimplementasikan, serta catatan perbaikan build aplikasi **Aura Audio**.

---

## 🚀 Status Terkini Proyek
- **Status Build**: ✅ **Siap Digunakan (Production-ready Debug APK)**
- **Metode Build**: Cloud CI/CD via GitHub Actions (`.github/workflows/build.yml`)
- **Target OS**: Android 7.0 (API 24) hingga Android 16 (API 36)
- **Bahasa & UI**: Kotlin 2.2.10 + Jetpack Compose + Material 3 (Dark Obsidian)

---

## 🛠️ Arsitektur & Tumpukan Teknologi (Tech Stack)

| Komponen | Teknologi | Deskripsi |
| :--- | :--- | :--- |
| **UI Framework** | Jetpack Compose + Material 3 | Desain modern bertema gelap (*Obsidian Dark*), animasi transisi, dan *edge-to-edge*. |
| **Audio Core** | AndroidX Media3 (ExoPlayer 1.5.1) | Pemutaran audio lokal & streaming, audio focus, buffering dinamis, becoming noisy listener. |
| **Background Service** | `MediaSessionService` | Menjalankan musik di latar belakang (Foreground Service) dengan notifikasi media sistem. |
| **Audio DSP / Effect** | `android.media.audiofx` | Multi-band Equalizer native, penguat Bass (*Bass Boost*), dan efek 3D Surround (*Virtualizer*). |
| **Data Storage** | `MediaStore` & ContentResolver | Memindai file audio internal perangkat (`.mp3`, `.m4a`, dll.) otomatis saat izin diberikan. |
| **Architecture Pattern**| MVVM + StateFlow | `MainViewModel`, `MusicController`, dan `EqualizerController` berbasis Coroutines reaktif. |
| **CI/CD Build** | GitHub Actions | Otomasi perakitan APK di cloud tanpa perlu instalasi Android Studio / Java di laptop lokal. |

---

## ✅ Checkpoint Fitur yang Sudah Selesai

### 1. Sistem Pemutar Audio (Playback Engine)
- [x] Background playback tanpa terhenti saat layar mati atau aplikasi diminimalkan.
- [x] Kontrol playback lengkap: Play, Pause, Seek (Scrubber), Next, Previous, Repeat Mode, Shuffle.
- [x] Pengatur kecepatan pemutaran (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x).
- [x] Integrasi Media Notification di status bar dan layar kunci (Lock Screen).
- [x] Penanganan Audio Focus (otomatis mengecil/pause saat ada panggilan masuk atau notifikasi telepon).
- [x] Sleep Timer (penghitung waktu mundur untuk mematikan musik otomatis).

### 2. Equalizer & Efek Suara Lanjutan
- [x] 5-Band Audio Equalizer terhubung langsung ke `audioSessionId` ExoPlayer.
- [x] Visualisasi Realtime Graphic EQ Curve (kurva respon frekuensi neon interaktif).
- [x] Studio Vertical Fader dengan drag vertikal alami, tap-to-position, dan tombol presisi +/- 1 dB.
- [x] Preset suara bawaan sistem (Normal, Classical, Dance, Flat, Heavy Metal, Hip Hop, Jazz, Pop, Rock).
- [x] Slider kustomisasi gain per band frekuensi (Hz / kHz).
- [x] Kontrol Bass Boost dengan pengatur intensitas kekuatan.
- [x] Kontrol Virtualizer 3D Surround dengan pengatur kekuatan spasial.

### 3. Pustaka & Manajemen Trek (Library)
- [x] Pemindaian otomatis file musik lokal di penyimpanan HP via `READ_MEDIA_AUDIO`.
- [x] Pencarian instan berdasarkan judul lagu atau nama artis.
- [x] Filter kategori musik (All, Jazz, Electronic, Synthwave, Cinematic, dll.).
- [x] Daftar trek online demo bawaan untuk pengujian langsung tanpa lagu lokal.

### 4. Navigasi & Antarmuka (UI/UX)
- [x] Bottom Navigation Bar Material 3 (Library, Now Playing, Equalizer).
- [x] Mini Player persisten mengambang di atas bar navigasi saat berpindah menu.
- [x] Now Playing screen dengan cover art visual modern dan kontrol lengkap.
- [x] Android System BackHandler (tombol kembali kembali ke halaman utama terlebih dahulu).

---

## 🔧 Catatan Perbaikan & Konfigurasi Penting (Fixes Log)

1. **Pembuatan File `.env`**:
   - Menambahkan file `.env` dengan variabel `GEMINI_API_KEY=dummy_api_key_placeholder`.
   - Mengatasi error fatal compiler Java: `public static final String GEMINI_API_KEY = ;` pada `BuildConfig.java`.
2. **Kustomisasi Keystore Debug**:
   - Mematikan baris `signingConfig = signingConfigs.getByName("debugConfig")` di `app/build.gradle.kts`.
   - Menghindari error ketiadaan file `debug.keystore` saat proses compile.
3. **Penyusunan Pipeline Cloud CI/CD**:
   - Menyiapkan workflow GitHub Actions di `.github/workflows/build.yml`.
   - Menyesuaikan dependensi JDK 21, Gradle 9.3.1, dan lisensi Android SDK 36.
   - Menghasilkan artifact APK yang langsung dapat diunduh dari halaman GitHub Actions.

---

## 🗺️ Rencana Pengembangan Selanjutnya (Next Roadmap)

- [ ] **Database Favorit & Playlist Kustom**: Menyimpan daftar lagu favorit pengguna ke Room Database lokal.
- [ ] **Lirik Lagu Sinkron**: Penampil lirik berjalan sinkron dengan posisi waktu audio.
- [ ] **Fitur Rekomendasi Musik AI**: Integrasi nyata Google Gemini API untuk rekomendasi playlist cerdas berdasarkan suasana hati.
- [ ] **Pengatur Tema Dinamis**: Pilihan tema warna kustom atau Material You Dynamic Theming.

