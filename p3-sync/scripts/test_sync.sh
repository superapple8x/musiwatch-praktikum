#!/usr/bin/env bash
# Integraretest för P3: created, duplicate, updated, conflict, tombstone och pull.
# Kräver API aktif di BASE (default http://localhost:8000).
set -euo pipefail

BASE="${BASE:-http://localhost:8000}"
ID="$(python3 -c 'import uuid; print(uuid.uuid4())')"
HEADER='content-type: application/json'

body() {
  # $1 = base_version (null|angka), $2 = pH, $3 = deleted
  printf '{"changes":[{"id":"%s","base_version":%s,"station_id":"S-001","officer_id":"u-118","ph":%s,"measured_at":"2026-11-10T08:00:00Z","deleted":%s}]}' "$ID" "$1" "$2" "$3"
}

push() {
  curl --fail-with-body --silent --show-error \
    -X POST "$BASE/v1/sync/push" -H "$HEADER" -d "$1"
}

assert_result() {
  local response="$1" expected_status="$2" expected_version="$3"
  printf '%s' "$response" | python3 -c '
import json,sys
d=json.load(sys.stdin)
r=d["results"][0]
assert r["status"] == sys.argv[1], f"status: expected {sys.argv[1]}, got {r}"
if sys.argv[2] != "-":
    key = "version" if "version" in r else "server_version"
    assert r[key] == int(sys.argv[2]), f"version: expected {sys.argv[2]}, got {r}"
print(json.dumps(r, ensure_ascii=False))
' "$expected_status" "$expected_version"
}

echo "[1/6] Push data baru"
r="$(push "$(body null 7.2 false)")"
assert_result "$r" created 1

echo "[2/6] Kirim ulang ID sama (idempotensi)"
r="$(push "$(body null 7.2 false)")"
assert_result "$r" duplicate 1

echo "[3/6] Update dengan base_version=1"
r="$(push "$(body 1 7.4 false)")"
assert_result "$r" updated 2

echo "[4/6] Konflik dengan base_version lama"
r="$(push "$(body 1 7.5 false)")"
assert_result "$r" conflict 2

echo "[5/6] Hapus secara logis (tombstone) dengan base_version=2"
r="$(push "$(body 2 7.4 true)")"
assert_result "$r" updated 3

echo "[6/6] Pull dan verifikasi tombstone"
pull="$(curl --fail-with-body --silent --show-error "$BASE/v1/sync/pull?since=0")"
printf '%s' "$pull" | python3 -c '
import json,sys
d=json.load(sys.stdin)
matches=[x for x in d["changes"] if x["id"] == sys.argv[1]]
assert matches, "ID hasil pengujian tidak ditemukan pada pull"
row=matches[-1]
assert row["deleted"] is True, f"deleted bukan true: {row}"
assert row["version"] == 3, f"versi tombstone bukan 3: {row}"
assert row["change_seq"] > 0, f"change_seq tidak valid: {row}"
print(json.dumps({"id":row["id"],"version":row["version"],"deleted":row["deleted"],"change_seq":row["change_seq"]}, ensure_ascii=False))
' "$ID"

echo "PASS: seluruh 6 skenario sinkronisasi backend berhasil."
