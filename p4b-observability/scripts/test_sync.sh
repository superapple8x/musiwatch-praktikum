#!/usr/bin/env bash
# Sumber: Modul Praktikum P3, Langkah 3.
# TAMBAHAN: dilengkapi uji updated, conflict, dan tombstone yang di modul hanya dideskripsikan.
# P4a: lewat Caddy (HTTPS) dan butuh TOKEN: export TOKEN=$(scripts/login.sh)
set -euo pipefail
BASE=${BASE:-https://localhost:8443}
CURL=(curl -s --cacert "$(mkcert -CAROOT)/rootCA.pem" -H "Authorization: Bearer ${TOKEN:?export TOKEN dulu}")
ID=$(python3 -c 'import uuid; print(uuid.uuid4())')
H='content-type: application/json'
body() {  # $1 = base_version (null|angka), $2 = ph, $3 = deleted
  printf '{"changes":[{"id":"%s","base_version":%s,"station_id":"S-001","officer_id":"u-118","ph":%s,"measured_at":"2026-11-10T08:00:00Z","deleted":%s}]}' "$ID" "$1" "$2" "$3"
}
echo "1) baru        -> created";   "${CURL[@]}" -X POST "$BASE/v1/sync/push" -H "$H" -d "$(body null 7.2 false)"; echo
echo "2) kirim ulang -> duplicate"; "${CURL[@]}" -X POST "$BASE/v1/sync/push" -H "$H" -d "$(body null 7.2 false)"; echo
echo "3) ubah v1     -> updated";   "${CURL[@]}" -X POST "$BASE/v1/sync/push" -H "$H" -d "$(body 1 7.4 false)"; echo
echo "4) ubah v1 lagi-> conflict";  "${CURL[@]}" -X POST "$BASE/v1/sync/push" -H "$H" -d "$(body 1 7.5 false)"; echo
echo "5) hapus v2    -> updated (tombstone)"; "${CURL[@]}" -X POST "$BASE/v1/sync/push" -H "$H" -d "$(body 2 7.4 true)"; echo
echo "6) pull        -> baris dengan deleted=true dan change_seq terbaru"
"${CURL[@]}" "$BASE/v1/sync/pull?since=0" | python3 -m json.tool | grep -E '"(id|version|deleted|change_seq)"' | tail -4
