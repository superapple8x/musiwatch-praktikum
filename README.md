# Kode Praktikum FSK4110 - MusiWatch

Kode dari Modul Praktikum FSK4110, dipisah per praktik. Setiap folder adalah **keadaan repositori di akhir praktik itu**
(solusi referensi kumulatif): `p2-container` = isi `p1` + tambahan P2, dan seterusnya. Untuk melihat apa yang
berubah antarpraktik: `diff -ru p3-sync p4a-security`.

| Folder | Praktik | Tag Git mahasiswa |
| --- | --- | --- |
| p1-api-latency | P1 - API, latency, timeout, retry (Mg 3) | p1-final |
| p2-container | P2 - Docker, Compose, Kubernetes lokal (Mg 5) | p2-final |
| p3-sync | P3 - Backend sinkronisasi offline-first (Mg 6-7) | p3-final |
| p4a-security | P4a - JWT, otorisasi objek, TLS, secret (Mg 10) | p4a-final |
| p4b-observability | P4b - Log, metrik, uji beban k6 (Mg 12) | p4b-final |
| p5-cicd | P5 - GitHub Actions, staging kind, rollback (Mg 14) | p5-final |

Setiap folder memuat `docs/Pn.md` (lembar kerja kosong sesuai pertanyaan modul).

## Penanda di dalam file

- `Sumber: Modul Praktikum Pn, Langkah m (verbatim)`: sama dengan teks modul.
- `TAMBAHAN`: kode yang di modul hanya dideskripsikan atau dirujuk tanpa isi, ditulis agar bisa dijalankan.
- `PERUBAHAN dari modul`: kode modul diubah karena akan gagal bila dijalankan apa adanya.

## Revisi 2 (12 Oktober 2026)

Perubahan menyusul pemeriksaan kemutakhiran materi:
- **P5** - workflow CI/CD: semua action dikunci ke SHA commit (versi dicatat di komentar), dinaikkan ke rilis
  Node 24, izin token per job, `persist-credentials: false`, gate pemindaian Grype sebelum push, SBOM dan
  provenance pada image, `timeout-minutes`, dan `.github/dependabot.yml`. Lembar kerja P5 mendapat bagian bukti
  gate rantai pasok. `api/ruff.toml` kini menulis himpunan aturan secara eksplisit karena rilis ruff baru
  mengaktifkan lebih banyak aturan default (ruff 0.16 melaporkan 23 temuan baru pada kode yang sama).
- **P4b** - `loadtest/summary.js` memakai `handleSummary()` (file JSON via `-e OUT=e1.json` dan ringkasan
  singkat di layar, termasuk p99). Opsi `--summary-export` tidak dipakai lagi karena k6 v2 menghapus mode
  ringkasan lama.
- **Android** - compileSdk/targetSdk 36 (wajib Google Play sejak 31 Agustus 2026), catatan edge-to-edge,
  larangan EncryptedSharedPreferences (deprecated), catatan RFC 9700 (ROPC dilarang, PKCE S256).
- **Compose** - catatan path volume bila pindah ke postgres:18.

Tidak berubah: logika API, skema database, manifest Kubernetes, dan versi pustaka Python/Android
(PyPI dan Maven tidak dapat dijangkau dari lingkungan penyusun, jadi versi tidak dinaikkan tanpa diuji).

## Versi alat yang diasumsikan (Oktober 2026)

| Alat | Versi | Catatan |
| --- | --- | --- |
| Python | 3.12 | sama dengan base image `python:3.12-slim` |
| Docker Engine/Desktop | dengan Compose v2 | perintah `docker compose`, bukan `docker-compose` |
| kind | v0.31 atau lebih baru (rilis terakhir saat disusun: v0.33.0) | sejak v0.31 host harus memakai cgroup v2; node image default Kubernetes 1.35 ke atas |
| k6 | v1 atau v2 (rilis terakhir saat disusun: v2.3.0) | skrip memakai `handleSummary()`, jalan di keduanya |
| Android Studio + emulator | image API 36 | compileSdk/targetSdk 36; API 37 boleh untuk emulator |
| PostgreSQL | 16 (image `postgres:16-alpine`) | bila pindah ke 18, mount volume ke `/var/lib/postgresql` |

## Status verifikasi (penting dibaca sebelum dry run)

Sudah diperiksa di lingkungan pembuat:
- Semua file Python lolos kompilasi sintaks; tidak ada impor yang tidak terpakai.
- Semua YAML (Compose, Kubernetes, GitHub Actions, Prometheus, Grafana) lolos parsing.
- Semua skrip shell lolos `bash -n`; skrip k6 lolos pemeriksaan sintaks JavaScript.
- Logika validasi JWT (pemanggilan `jwt.decode` yang sama dengan `auth.py`) diuji dengan PyJWT: token sah 200;
  audiens salah, kedaluwarsa, penerbit salah, `alg: none`, payload diubah, dan serangan HS256 semuanya 401.

BELUM dijalankan (tidak tersedia Docker, PostgreSQL, FastAPI, Android SDK, dan GitHub Actions di lingkungan pembuat):
- API FastAPI terhadap PostgreSQL sungguhan, pytest P4a, Docker/Compose, kind, k6, pipeline CI.
- Kode Kotlin belum dikompilasi; perlakukan sebagai kerangka.
- Versi pustaka di requirements.txt dikunci ke versi yang sudah ada per 2024; perbarui dengan `pip freeze` saat dry run
  (Dependabot di P5 juga akan mengusulkan pembaruan).
- SHA action di P5 diambil langsung dari tag rilis di GitHub pada 12 Oktober 2026, tetapi pipeline belum dijalankan.
  Bila `kind load docker-image` gagal dengan pesan "content digest ... not found" (Docker dengan containerd image
  store dan image ber-attestation), catat di laporan dry run; jalan pintasnya adalah `provenance: false` dan
  `sbom: false` di langkah push, dengan konsekuensi SBOM hilang dari image.

## Perbedaan dari teks modul yang perlu dibawa kembali ke modul

Temuan ini muncul saat kode modul dijadikan file utuh. Butir bertanda (!) akan membuat langkah modul gagal.

1. (!) **P4a - celah keamanan di modul sendiri.** `/v1/sync/pull` di modul mengembalikan pengukuran semua wilayah
   kepada petugas mana pun (BOLA, sama dengan Latihan 10.3). Di sini push dan pull dibatasi ke wilayah pengguna.
2. (!) **P4a - impor melingkar.** `auth.find_user` butuh pool dari `sync.py`, sedangkan `sync.py` mengimpor
   `current_user` dari `auth.py`. Diselesaikan dengan impor di dalam fungsi.
3. (!) **P4a - emulator tidak bisa memakai `api.musiwatch.test`.** Emulator melihat laptop sebagai `10.0.2.2`.
   Caddyfile diubah ke `:8443` dan sertifikat mkcert memuat `10.0.2.2` (scripts/gen_certs.sh).
4. (!) **P4a - tabel users tidak terbuat.** Skrip init Postgres hanya berjalan pada volume kosong, padahal volume
   dari P3 sudah ada. Gunakan scripts/apply_users.sh.
5. (!) **P4a - izin kunci.** Kunci privat dari `openssl genpkey` (mode 600) tidak terbaca oleh `appuser` di
   kontainer bila UID berbeda. scripts/gen_keys.sh memberi chmod 644 (khusus lab).
6. (!) **P5 - ruff gagal.** Kode modul memakai `import a, b, c` (E401) dan `auth.py` mengimpor `Depends` tanpa
   dipakai (F401). Ditambahkan api/ruff.toml (abaikan E401) dan impor `Depends` dihapus.
7. (!) **P5 - staging tidak muat di runner.** Runner repositori privat 2 vCPU tidak muat 3 replika x 500m CPU
   ditambah komponen kind, sehingga `rollout status` timeout. Workflow menurunkan request dan replika di staging.
8. (!) **P5 - nama image.** GHCR menolak huruf besar; nama image kini dihitung huruf kecil otomatis.
9. **P1** - pada perintah curl dua URL, opsi `-w` berlaku untuk keduanya; kolom `koneksi_baru` (num_connects)
   dipakai untuk membedakan koneksi baru dan dipakai ulang.
10. **P3 Android** - DAO dibuat abstract class (Room + `@Transaction` pada fungsi berisi logika), dan tanggal dari
    server diparse dengan `OffsetDateTime` karena Postgres mengirim offset `+00:00`.
11. **P4b** - kedua versi endpoint ringkasan tersedia lewat env `SUMMARY_MODE` (n1/groupby) agar E1-E3 tidak perlu
    mengedit kode; data source Grafana disiapkan otomatis.

## Urutan dry run yang disarankan

1. p1: `cd api && pip install -r requirements.txt && uvicorn app.main:app --port 8000`, lalu scripts/measure_latency.sh.
2. p2: `cp deploy/.env.example deploy/.env`, `docker compose -f deploy/docker-compose.yml up -d --build`, scripts/p2_kind.sh.
3. p3: `docker compose ... down -v` lalu `up -d --build`, scripts/test_sync.sh.
4. p4a: scripts/gen_keys.sh, scripts/gen_certs.sh, `up -d --build`, scripts/apply_users.sh,
   `cd api && DATABASE_URL=postgresql://musiwatch:<pw>@localhost:5432/musiwatch pytest -q`, scripts/login.sh.
5. p4b: ikuti scripts/run_experiments.md (k6 v1 atau v2).
6. p5: dorong folder ke repositori GitHub uji, amati run CI (termasuk gate Grype); jalankan scripts/rollback_demo.sh.

Catat setiap error dan waktu per praktik; perbaiki modul berdasarkan catatan itu.
