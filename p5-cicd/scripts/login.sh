#!/usr/bin/env bash
# TAMBAHAN: ambil access token lewat Caddy. Pemakaian: export TOKEN=$(scripts/login.sh petugas.ib musiwatch-lab-1)
set -euo pipefail
U=${1:-petugas.ib}; P=${2:-musiwatch-lab-1}
curl -s --cacert "$(mkcert -CAROOT)/rootCA.pem" https://localhost:8443/v1/auth/login \
  -H 'content-type: application/json' -d "{\"username\":\"$U\",\"password\":\"$P\"}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
