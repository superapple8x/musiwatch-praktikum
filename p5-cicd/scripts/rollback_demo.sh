#!/usr/bin/env bash
# TAMBAHAN untuk Modul P5 Langkah 4: latihan rollback di kind lokal.
# v0.5.0 = versi baik; v0.5.1 = versi rusak (readiness selalu 503), dibuat otomatis dari salinan kode.
set -euo pipefail
CLUSTER=musiwatch-rb
kind create cluster --name $CLUSTER || true
docker build -t musiwatch/api:0.5.0 api/
TMP=$(mktemp -d); cp -r api "$TMP/"
python3 - "$TMP/api/app/main.py" <<'PY'
import sys; p = sys.argv[1]; s = open(p).read()
s = s.replace('def ready(response: Response):\n', 'def ready(response: Response):\n    response.status_code = 503\n    return {"status": "rusak-sengaja"}\n', 1)
open(p, "w").write(s)
PY
docker build -t musiwatch/api:0.5.1 "$TMP/api"; rm -rf "$TMP"
kind load docker-image musiwatch/api:0.5.0 musiwatch/api:0.5.1 --name $CLUSTER
kubectl apply -f deploy/k8s/staging/
./deploy/scripts/create-staging-secrets.sh
kubectl apply -f deploy/k8s/api.yaml
kubectl set resources deployment/musiwatch-api --requests=cpu=100m,memory=128Mi
kubectl set image deployment/musiwatch-api api=musiwatch/api:0.5.0
kubectl rollout status deployment/musiwatch-api --timeout=300s
echo ">>> Deploy versi rusak v0.5.1 (rollout akan macet; pod lama tetap melayani)"
kubectl set image deployment/musiwatch-api api=musiwatch/api:0.5.1
kubectl rollout status deployment/musiwatch-api --timeout=60s || echo ">>> Rollout macet seperti diharapkan"
kubectl get pods -l app=musiwatch-api
START=$(date +%s)
kubectl rollout undo deployment/musiwatch-api
kubectl rollout status deployment/musiwatch-api --timeout=300s
echo ">>> Pulih dalam $(( $(date +%s) - START )) detik"
kubectl rollout history deployment/musiwatch-api
