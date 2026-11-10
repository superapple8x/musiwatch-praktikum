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

## Tambahan untuk P3

Dependensi tambahan:

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")          // butuh plugin com.google.devtools.ksp
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // plugin: id("org.jetbrains.kotlin.plugin.serialization")

Alur: layar simpan -> dao.upsert(entity PENDING) -> requestSyncNow(context).
Daftar pengukuran: dao.observeAll() dan tampilkan ikon dari kolom syncState.

## Tambahan untuk P4a

- `BASE_URL` kini `https://10.0.2.2:8443` (lewat Caddy). Pasang root CA mkcert di emulator (lihat scripts/gen_certs.sh).
- Pasang `AuthInterceptor` di `buildClient` dengan `tokenProvider = { TokenStore.load(context) }`.
- `SyncWorker`: bila push/pull mendapat 401, kembalikan `Result.failure()` dan tampilkan ajakan login;
  baris PENDING tetap di database lokal.
- Manifest: `android:allowBackup="false"`.
- Jangan memakai `androidx.security:security-crypto` (EncryptedSharedPreferences). Pustaka itu deprecated
  sejak versi 1.1.0. `TokenStore.kt` memakai kunci AES dari Android Keystore secara langsung; untuk aplikasi
  produksi, pola yang dianjurkan adalah Tink + DataStore dengan kunci induk di Keystore.
- Login di praktik ini mengirim username dan password langsung ke API milik sendiri, demi kesederhanaan lab.
  Bila login memakai penyedia identitas (OAuth 2.0), RFC 9700 (Januari 2025) melarang grant password (ROPC).
  Gunakan Authorization Code + PKCE (S256) lewat browser, misalnya dengan pustaka AppAuth. Refresh token untuk
  klien publik wajib dirotasi atau diikat ke pengirim (sender-constrained).

## Catatan versi (Revisi 2, Oktober 2026)

- Sejak 31 Agustus 2026 Google Play mewajibkan aplikasi baru dan pembaruan menargetkan API 36 (Android 16),
  jadi proyek praktik memakai compileSdk dan targetSdk 36 dan emulator dengan image API 36.
  Android 17 (API 37) sudah stabil sejak Juni 2026; boleh dipakai untuk emulator, tidak wajib.
- Target API 35 ke atas memaksa tampilan edge-to-edge. Untuk layar praktik yang sederhana cukup panggil
  `enableEdgeToEdge()` dan beri padding dari `WindowInsets` agar tombol tidak tertutup status bar.
- Versi pustaka di atas adalah versi yang dipakai saat modul disusun. Android Studio akan menyarankan versi
  lebih baru; terima saran itu, lalu catat versi akhir di laporan. OkHttp 4.12 dipertahankan agar cocok dengan
  teks modul. OkHttp 5.x sudah stabil, tetapi uji dulu sebelum pindah karena struktur artefaknya berubah.

