#!/usr/bin/env bash
# TAMBAHAN: dirujuk Modul P5 Langkah 3 tanpa isi. Membuat ConfigMap skema, kata sandi DB acak,
# DATABASE_URL, dan kunci JWT sementara. Tidak ada yang disimpan ke repositori.
# Catatan: jalankan SEKALI per cluster; menjalankan ulang membuat kata sandi baru yang tidak cocok
# dengan database yang sudah terinisialisasi.
set -euo pipefail
DB_PASSWORD=$(openssl rand -hex 16)
apply() { kubectl apply -f -; }
kubectl create configmap db-schema \
  --from-file=001_init.sql=api/db/001_init.sql --from-file=002_users.sql=api/db/002_users.sql \
  --dry-run=client -o yaml | apply
kubectl create secret generic db-secret --from-literal=password="$DB_PASSWORD" --dry-run=client -o yaml | apply
kubectl create secret generic api-secret \
  --from-literal=DATABASE_URL="postgresql://musiwatch:${DB_PASSWORD}@db:5432/musiwatch" \
  --dry-run=client -o yaml | apply
TMP=$(mktemp -d)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP/jwt_private.pem" 2>/dev/null
openssl rsa -in "$TMP/jwt_private.pem" -pubout -out "$TMP/jwt_public.pem" 2>/dev/null
kubectl create secret generic jwt-keys \
  --from-file=jwt_private.pem="$TMP/jwt_private.pem" --from-file=jwt_public.pem="$TMP/jwt_public.pem" \
  --dry-run=client -o yaml | apply
rm -rf "$TMP"
