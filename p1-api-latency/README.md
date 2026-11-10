# MusiWatch - Praktik 1: API dan Latency

Repositori jawaban Praktik 1 mata kuliah FSK4110. Isinya server FastAPI dari modul,
skrip pengukuran curl, proksi throttle pengganti profil jaringan emulator, aplikasi
Android (OkHttp + retry), dan lembar kerja `docs/P1.md`.

Tag penilaian: **p1-final**.

## Struktur

```
api/                     server FastAPI modul (verbatim) + requirements.txt
  app/main.py            /v1/stations (paginasi, ETag, Cache-Control), /v1/flaky
android/                 aplikasi id.unsri.musiwatch (Kotlin + Compose)
  app/src/main/java/id/unsri/musiwatch/
    MainActivity.kt      layar: 2 chip base URL, 2 tombol
    MainViewModel.kt     state, klien dipakai ulang, pemanasan, runCatching
    net/HttpClients.kt   timeout, baseUrl, identity encoding, EventListener
    net/RetryInterceptor.kt  verbatim modul
docs/
  P1.md                  lembar kerja (yang dinilai)
  p1_prosedur.md         cara mengulang semua pengukuran + temuan emulator
  p1_curl.csv            a/b/c, 10 pengulangan
  p1_curl_d_contoh.csv   d vs example.com (baru vs dipakai ulang)
  p1_profil_jaringan.csv sapuan host lewat proksi (3x, dengan/tanpa kompresi)
  p1_profil_aplikasi.csv sapuan aplikasi (3x per profil)
  p1_bukti.csv           indeks bukti: sha256 + isi tiap tangkapan layar
  bukti/*.png            tangkapan layar 01-06
scripts/
  measure_latency.sh     pengukur curl (versi modul + perbaikan ETag/HEAD)
  throttle_proxy.py      proksi TCP: full / hsdpa / edge
```

## Mulai cepat

```bash
# 1. API
cd api && python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000

# 2. curl (terminal lain, dari akar repo)
BASE=http://127.0.0.1:8000 N=10 ./scripts/measure_latency.sh > /tmp/curl.csv

# 3. proksi profil (opsional; untuk tabel aplikasi)
python3 scripts/throttle_proxy.py --profile edge    # :9000, 473,6 kbit/s, +240 ms
python3 scripts/throttle_proxy.py --profile hsdpa   # :9000, 13,98 Mbit/s, +117 ms
python3 scripts/throttle_proxy.py --profile full    # :9000, tanpa batas

# 4. aplikasi
cd android && export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n id.unsri.musiwatch/.MainActivity
```

Detail langkah dan cara membaca hasil ada di `docs/p1_prosedur.md`.

## Ringkasan hasil

| Ukuran | Angka |
| --- | --- |
| curl a -> b (kompresi) | size -95,45 %, total +16,30 % (6,413 -> 7,459 ms) |
| curl a -> c (ETag 304) | size -100 %, total -81,79 % (6,413 -> 1,168 ms) |
| koneksi HTTPS dipakai ulang | total 53,995 -> 21,817 ms (-59,6 %) |
| aplikasi 50 stasiun: full / hsdpa / edge | 3 / 129 / 473 ms (median, n=3) |
| flaky 20x tanpa / dengan retry | 15/20 sukses (3,0 s) -> 20/20 (8,1 s) |

## Penyimpangan dari modul (dan alasannya)

1. **compileSdk/targetSdk 36, minSdk 24** sesuai modul. Namun rilis AndroidX terbaru
   (core-ktx 1.19.x, lifecycle 2.11.x, activity-compose 1.13.x) menuntut compileSdk 37.
   Karena hanya `android-36` yang terpasang dan modul mewajibkan 36, versi dipin ke rilis
   terakhir yang masih menerima 36: core-ktx 1.17.0, lifecycle 2.9.4,
   activity-compose 1.12.4, Compose BOM 2026.06.01.
2. **AGP 9.5.1 + Gradle 9.6.1** (AGP 9.4/9.5 menuntut Gradle >= 9.6.0). AGP 9 menolak
   plugin `org.jetbrains.kotlin.android` ("no longer required since AGP 9.0"), jadi plugin
   itu dihapus; hanya `kotlin.plugin.compose` yang tersisa.
3. **JDK 25** satu-satunya JDK di mesin ini, sehingga `jvmToolchain(17)` dicabut dan diganti
   `compilerOptions.jvmTarget = JVM_17` + `compileOptions` Java 17.
4. **OkHttp 4.12.0** dipertahankan sesuai modul.
5. **Profil jaringan emulator tidak bekerja** (`-netdelay`/`-netspeed` diabaikan untuk lalu
   lintas data; lihat `docs/p1_prosedur.md` bagian 5). Diganti `scripts/throttle_proxy.py`.
6. **Baris d** memakai `https://example.com` karena URL endpoint dosen tidak ada di modul;
   angka itu contoh bentuk, bukan server MusiWatch.
7. **Pengukuran aplikasi** memakai `Accept-Encoding: identity` dan koneksi hangat, karena
   gzip menyusutkan body ke ~1 kB dan koneksi baru ke `10.0.2.2` menambah ~503 ms yang
   bukan efek profil. Tanpa dua penyesuaian ini, ketiga profil tampak sama.
8. **`measure_latency.sh`**: `curl -sI` mengirim HEAD -> FastAPI 405 tanpa ETag -> skrip
   mati karena `set -e`. Diganti GET `-D - -o /dev/null`.
9. **Tambahan kecil**: `runCatching` di `load50` agar IOException (mis. proksi mati) tidak
   mematikan aplikasi; `EventListener` untuk bukti waktu connect/header/body.

## Keamanan

Tidak ada kredensial atau token di repositori. Alamat yang muncul hanya
`10.0.2.2:8000` / `10.0.2.2:9000` (host dari emulator) dan URL contoh publik.
Izin cleartext dibatasi pada `10.0.2.2` lewat `network_security_config.xml`.
