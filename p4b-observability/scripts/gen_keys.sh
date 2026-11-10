#!/usr/bin/env bash
# Sumber: Modul Praktikum P4a, Langkah 1. Jalankan dari akar folder.
# TAMBAHAN: chmod 644 karena kontainer berjalan sebagai appuser yang UID-nya bisa berbeda dari UID Anda.
# Ini HANYA untuk lab; di produksi kunci diberikan lewat secret manager.
set -euo pipefail
mkdir -p deploy/secrets
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out deploy/secrets/jwt_private.pem
openssl rsa -in deploy/secrets/jwt_private.pem -pubout -out deploy/secrets/jwt_public.pem
chmod 644 deploy/secrets/*.pem
git check-ignore -q deploy/secrets/jwt_private.pem && echo "OK: deploy/secrets diabaikan Git" \
  || echo "PERINGATAN: deploy/secrets belum ada di .gitignore"
