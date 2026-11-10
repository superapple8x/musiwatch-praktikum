# Klien Android - kerangka (P1)

Status: KERANGKA. File Kotlin di sini belum dikompilasi; masukkan ke proyek Android Studio
(Empty Activity, Kotlin, minSdk 24, compileSdk 36, targetSdk 36) lalu sesuaikan nama paket.

Dependensi `app/build.gradle.kts`:

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

AndroidManifest.xml:

    <uses-permission android:name="android.permission.INTERNET" />
    <application android:networkSecurityConfig="@xml/network_security_config" ... >

Layar praktik cukup dua tombol: "Muat 50 stasiun" (tampilkan durasi) dan
"Uji /v1/flaky 20x" dengan dan tanpa retry (tampilkan jumlah sukses/gagal dan durasi).
Panggil fungsi di HttpClients.kt dari lifecycleScope/viewModelScope.

## Catatan versi (Revisi 2, Oktober 2026)

- Sejak 31 Agustus 2026 Google Play mewajibkan aplikasi baru dan pembaruan menargetkan API 36 (Android 16),
  jadi proyek praktik memakai compileSdk dan targetSdk 36 dan emulator dengan image API 36.
  Android 17 (API 37) sudah stabil sejak Juni 2026; boleh dipakai untuk emulator, tidak wajib.
- Target API 35 ke atas memaksa tampilan edge-to-edge. Untuk layar praktik yang sederhana cukup panggil
  `enableEdgeToEdge()` dan beri padding dari `WindowInsets` agar tombol tidak tertutup status bar.
- Versi pustaka di atas adalah versi yang dipakai saat modul disusun. Android Studio akan menyarankan versi
  lebih baru; terima saran itu, lalu catat versi akhir di laporan. OkHttp 4.12 dipertahankan agar cocok dengan
  teks modul. OkHttp 5.x sudah stabil, tetapi uji dulu sebelum pindah karena struktur artefaknya berubah.

