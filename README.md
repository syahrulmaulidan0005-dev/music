# Live Music

Tampilan "now playing" untuk TikTok LIVE (screen share): piringan di balik sampul artis, lirik berjalan,
gelombang batang, dan komentar live dengan transisi. Android (Kotlin + Jetpack Compose).

## Build APK
Push ke GitHub (branch `main`). GitHub Actions membuat APK otomatis: tab **Actions → Build APK → Artifacts**.
Atau lokal: `gradle assembleDebug` (butuh Android SDK + JDK 17).

## Cara pakai
1. Pasang APK, buka aplikasi.
2. Izinkan akses notifikasi (untuk membaca lagu dari Spotify/YT Music/dll).
3. (Opsional) izinkan audio untuk gelombang asli. Kalau diblokir sistem, gelombang memakai simulasi.
4. Masukkan username TikTok lalu mulai. Bagikan layar lewat TikTok LIVE.

## Catatan
- Komentar memakai library tidak resmi (TikTok-Live-Java); bisa berhenti jalan kalau TikTok mengubah sistemnya.
- Lirik dari LRCLIB, foto artis dari Deezer (cadangan: cover album).
- Gunakan musik bebas hak cipta atau musik resmi TikTok agar live tidak dibisukan.
