#!/usr/bin/env bash
# TAMBAHAN: muat tabel users ke database yang volumenya sudah ada sejak P3
# (skrip init Postgres hanya berjalan saat volume masih kosong).
set -euo pipefail
docker compose -f deploy/docker-compose.yml exec -T db psql -U musiwatch -d musiwatch < api/db/002_users.sql
