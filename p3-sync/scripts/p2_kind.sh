#!/usr/bin/env bash
# Sumber: Modul Praktikum P2, Langkah 4 (dirangkai menjadi skrip). Jalankan dari akar folder ini.
set -euo pipefail
docker build -t musiwatch/api:0.2.0 api/
kind create cluster --name musiwatch || true
kind load docker-image musiwatch/api:0.2.0 --name musiwatch
kubectl create secret generic api-secret \
  --from-literal=DATABASE_URL='postgresql://user:pass@db:5432/musiwatch' \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f deploy/k8s/api.yaml
kubectl rollout status deployment/musiwatch-api --timeout=180s
echo "Lanjutkan manual: port-forward, hapus pod, scale (lihat Modul P2 Langkah 4-5)."
