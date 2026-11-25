# Klien Android - MusiWatch P3

Buka folder `p3-sync/android` di Android Studio. Proyek memakai minSdk 24 dan
compileSdk/targetSdk 36. Versi Room, WorkManager, KSP, dan Kotlin serialization
ada di `gradle/libs.versions.toml`.

Jalankan API P3 pada port 8000 dan buka aplikasi di emulator. Aplikasi
mengakses port tersebut melalui `http://10.0.2.2:8000`.

1. Aktifkan mode pesawat dan pastikan Wi-Fi mati. Masukkan pH dan simpan
   pengukuran. Daftar menampilkan `PENDING`.
2. Tutup lalu buka kembali aplikasi. Pengukuran `PENDING` tetap tampil dari Room.
3. Pulihkan jaringan. WorkManager mengirim pengukuran dan status berubah
   menjadi `SYNCED`. Gunakan **Sinkronkan sekarang** untuk meminta pull segera
   setelah ada perubahan dari klien lain.
4. Untuk memeriksa konflik, ubah pengukuran `SYNCED` saat offline, lalu ubah
   versi yang sama di server sebelum memulihkan jaringan. Status `CONFLICT`
   menandai bahwa perubahan lokal masih tersimpan.
5. Untuk memeriksa penghapusan, kirim tombstone ke server untuk pengukuran
   `SYNCED`. Setelah pull, pengukuran hilang dari daftar.

Kontrol latensi P1 masih tersedia di bawah daftar pengukuran. Pilih port 8000
untuk jalur langsung atau port 9000 jika proksi throttle sedang berjalan.
