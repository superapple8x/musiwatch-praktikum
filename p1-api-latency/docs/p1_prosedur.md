# Prosedur reproduksi P1

Semua perintah dijalankan dari akar repositori ini (`p1-api-latency/`), kecuali
disebutkan lain. Lingkungan acuan: Fedora 44, JDK 25, Android SDK `android-36` +
`build-tools 36.1.0`, emulator `Pixel_10_API_36` (API 36, google_apis, x86_64).

## 1. API

```bash
cd api
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Cek cepat:

```bash
curl -s -D - -o /dev/null 'http://127.0.0.1:8000/v1/stations?limit=2'
# harap: 200, ada ETag + Cache-Control: max-age=60, body {"items":[...],"next_offset":2}
curl -s 'http://127.0.0.1:8000/v1/stations?limit=2' | head -c 200
```

## 2. Pengukuran curl (P1.1 bagian 1)

```bash
BASE=http://127.0.0.1:8000 N=10 ./scripts/measure_latency.sh > docs/p1_curl.csv
# baris d butuh endpoint HTTPS; contoh di laporan memakai example.com:
BASE=http://127.0.0.1:8000 HTTPS_URL=https://example.com N=10 \
  ./scripts/measure_latency.sh > /tmp/ukur.csv
```

Catatan: skrip asli modul memakai `curl -sI` untuk mengambil ETag. Itu mengirim **HEAD**,
dan FastAPI membalas **405** tanpa ETag; dengan `set -euo pipefail` skrip langsung mati.
Versi di repositori ini memakai `curl -s -D - -o /dev/null` (GET, body dibuang).

Median dihitung dari CSV, misalnya:

```bash
python3 - <<'PY'
import csv, statistics as st
rows = list(csv.DictReader(open("docs/p1_curl.csv")))
for k in ("a_tanpa_kompresi", "b_kompresi", "c_etag"):
    r = [x for x in rows if x["kondisi"] == k]
    print(k, st.median(float(x["total"]) for x in r))
PY
```

## 3. Proksi throttle (pengganti profil emulator)

```bash
python3 scripts/throttle_proxy.py --profile edge   # 473,6 kbit/s, +240 ms
python3 scripts/throttle_proxy.py --profile hsdpa  # 13,98 Mbit/s, +117 ms
python3 scripts/throttle_proxy.py --profile full   # tanpa batas (kontrol)
```

Dari laptop: `http://127.0.0.1:9000`; dari emulator: `http://10.0.2.2:9000`.
Proksi hanya membatasi arah **unduh** (server -> klien); permintaan HTTP terlalu kecil
untuk berarti. Latensi diterapkan **per permintaan HTTP** (terdeteksi dari akhir header
`\r\n\r\n`), bukan sekali per koneksi, dan jadwal bitrate dimulai setelah latensi sehingga
`total = latensi + transfer`.

Verifikasi dari host (harus keluar angka seperti ini, n=2 pada satu koneksi keep-alive):

```bash
for p in full hsdpa edge; do
  python3 scripts/throttle_proxy.py --profile $p & PID=$!; sleep 1
  curl -s -o /dev/null -o /dev/null \
    -w "$p %{time_total} s\n" \
    'http://127.0.0.1:9000/v1/stations?limit=50' \
    'http://127.0.0.1:9000/v1/stations?limit=50'
  kill $PID; wait $PID 2>/dev/null
done
```

Keluaran acuan: full ~0,002-0,004 s; hsdpa ~0,127 s; edge ~0,471 s (13.358 bita,
tanpa kompresi). Dengan `--compressed` (gzip) edge turun ke ~0,257 s.

## 4. Aplikasi Android

```bash
cd android
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n id.unsri.musiwatch/.MainActivity
```

Layar punya dua chip base URL (`langsung :8000`, `proksi :9000`) dan dua tombol:
`Muat 50 stasiun` serta `Uji /v1/flaky 20x (dengan & tanpa retry)`.

### 4.1 Mengukur tabel profil

1. Nyalakan proksi profil yang diuji (port 9000 harus bebas).
2. Di aplikasi pilih chip `proksi :9000`.
3. Tekan `Muat 50 stasiun`. Aplikasi melakukan **dua** permintaan: pemanasan (dibuang)
   dan pengukuran. Baca angka `50 stasiun: N ms` (bukan `pemanasan`).
4. Ulangi 3x per profil, ganti proksi di antara profil.

Kenapa harus begini:

- **Klien dipakai ulang + pemanasan.** Koneksi TCP baru ke `10.0.2.2` selalu memakan
  ~503 ms, apa pun profilnya. Tanpa pemanasan, ketiga profil tampak ~502 ms.
- **`Accept-Encoding: identity`.** OkHttp otomatis meminta gzip; 13.358 bita JSON menjadi
  ~973 bita di kabel sehingga batas bitrate nyaris tidak terasa. Dengan identity, body
  benar-benar 13.358 bita.
- **Angka TTFB vs total.** `LoadResult` mencatat keduanya. Pada edge, TTFB bisa ~247 ms
  (header dikirim setelah latensi) dan total ~473 ms (setelah transfer), karena seluruh
  balasan dikirim sebagai satu potongan yang kena bitrate.

### 4.2 Otomasi tap (opsional)

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb shell cat /sdcard/ui.xml | grep -o 'text="Muat 50 stasiun"[^>]*bounds="[^"]*"'
# ambil titik tengah bounds, lalu:
adb shell input tap <x> <y>
```

Teks yang memuat `&` muncul sebagai `&amp;` di XML; cocokkan substring seperti
`flaky 20x`, jangan teks lengkap.

## 5. Temuan emulator (bukti di `logs/`)

1. **Profil jaringan emulator tidak bekerja untuk data.** `adb emu network speed edge`
   melaporkan 473.600 bit/s di `network status`, tetapi 133.994 bita tetap selesai ~10 ms
   dan `ping 8.8.8.8` tetap ~13 ms di semua profil. Tamu memakai netstack `eth0`
   (10.0.2.15); penskalaan hanya berlaku pada modem seluler lama. Karena itu proksi TCP
   dipakai.
2. **Koneksi baru ke 10.0.2.2 ~503 ms.** Garis waktu OkHttp (`adb logcat -s
   MusiWatchTIMING`): `connectEnd +2 ms`, `requestHeadersEnd +3 ms`,
   `responseHeadersStart +502 ms`, padahal log proksi mencatat permintaan +0,7 ms dan
   bita pertama balasan +2,3 ms. Jadi tunda ~500 ms ada di jalur host->tamu untuk koneksi
   baru. Koneksi yang sudah hangat: 3-4 ms.
3. **Bukan kuantisasi 500 ms.** Delay proksi 600/1100/1600 ms lewat apa adanya
   (605/1105/1605 ms), jadi bukan "tick" yang menyerap delay < 500 ms; yang tertunda
   hanya data pertama pada koneksi baru.
4. **gzip menutupi bitrate.** Tanpa `identity`, body 13.358 B menjadi ~973 B; edge hanya
   ~250 ms dan perbedaan profil mengecil.

## 6. Retry (P1.3)

Tekan `Uji /v1/flaky 20x` pada base URL `langsung :8000`. Butuh ~15 s. Hasil acuan
(30 % 503 di server): tanpa retry 15/20 sukses, dengan retry 20/20; total ~3,0 s vs ~8,1 s.
Angka bervariasi karena `/v1/flaky` acak; yang stabil adalah pola "gagal -> sukses dengan
biaya waktu".

## 7. Hasil acuan

| Berkas | Isi |
| --- | --- |
| `docs/p1_curl.csv` | a/b/c, 10 pengulangan, limit=500 |
| `docs/p1_curl_d_contoh.csv` | d vs `example.com`, baru vs dipakai ulang |
| `docs/p1_profil_jaringan.csv` | sapuan host lewat proksi, 3x, dengan/tanpa kompresi |
| `docs/p1_profil_aplikasi.csv` | sapuan aplikasi, 3x per profil, TTFB + total |
| `docs/bukti/*.png` | tangkapan layar 01-06 (01 awal, 02 full langsung, 03 retry, 04 edge, 05 hsdpa, 06 full lewat proksi) |
| `docs/p1_bukti.csv` | indeks bukti: sha256, waktu berkas, dan isi tiap tangkapan layar |
