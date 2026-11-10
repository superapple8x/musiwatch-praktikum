#!/usr/bin/env bash
# Sumber: Modul Praktikum P1, Langkah 2.
# TAMBAHAN: diulang N kali dan keluaran CSV agar mudah dihitung median/maksimumnya.
# Pemakaian: BASE=http://localhost:8000 HTTPS_URL=https://<endpoint-dosen> N=10 ./measure_latency.sh > hasil.csv
set -euo pipefail
BASE=${BASE:-http://localhost:8000}
HTTPS_URL=${HTTPS_URL:-}
N=${N:-10}
FMT='%{time_namelookup},%{time_connect},%{time_appconnect},%{time_starttransfer},%{time_total},%{size_download},%{http_code},%{num_connects}\n'
echo "kondisi,dns,conn,tls,ttfb,total,size,status,koneksi_baru"
for _ in $(seq "$N"); do
  curl -o /dev/null -s -w "a_tanpa_kompresi,$FMT" "$BASE/v1/stations?limit=500"
  curl -o /dev/null -s --compressed -w "b_kompresi,$FMT" "$BASE/v1/stations?limit=500"
done
# PERBAIKAN: `curl -sI` mengirim HEAD, dan FastAPI membalas 405 (allow: GET) tanpa ETag,
# sehingga grep gagal dan skrip berhenti karena `set -e`. Pakai GET + buang body.
ETAG=$(curl -s -D - -o /dev/null "$BASE/v1/stations?limit=50" | grep -i '^etag:' | cut -d' ' -f2 | tr -d '\r')
for _ in $(seq "$N"); do
  curl -o /dev/null -s -H "If-None-Match: $ETAG" -w "c_etag,$FMT" "$BASE/v1/stations?limit=50"
done
if [ -n "$HTTPS_URL" ]; then
  # Dua URL dalam satu perintah memakai ulang koneksi. Opsi -w berlaku untuk keduanya,
  # jadi baris pertama/kedua dibedakan lewat kolom koneksi_baru (1 = baru, 0 = dipakai ulang).
  for _ in $(seq "$N"); do
    curl -s -w "d_https,$FMT" -o /dev/null "$HTTPS_URL" -o /dev/null "$HTTPS_URL"
  done
fi
