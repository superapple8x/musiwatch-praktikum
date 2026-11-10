# Lembar Kerja Praktik 4a - Keamanan (P4.1)

Nama/NIM:            Tag Git: p4a-final

## 1. Threat model STRIDE
Untuk `POST /v1/sync/push` dan `GET /v1/stations/{id}/measurements`: minimal satu ancaman per kategori dan kontrolnya.

## 2. Hasil pytest
Sebelum perbaikan (uji harus gagal) dan sesudah perbaikan (lulus). Tempel output `pytest -q`.

## 3. Bukti TLS
Output `curl -v https://localhost:8443/health/live` (handshake TLS) dan bukti `http://localhost:8000` gagal.

## 4. Bukti secret
Output `git log --all -- deploy/secrets` (harus kosong) dan cara kunci dimuat ke kontainer.

## 5. Pemetaan MASVS dan risiko tersisa
STORAGE, NETWORK, AUTH; satu risiko yang masih tersisa.

## 6. (Opsional) Refresh token yang dirotasi dan dapat dicabut

## Sumber dan bantuan (termasuk AI generatif)
