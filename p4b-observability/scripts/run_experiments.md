# Urutan eksperimen P4b (TAMBAHAN, merangkum Modul P4b Langkah 3-4)

Token berlaku 15 menit; ambil ulang sebelum setiap eksperimen.

    export DC="docker compose -f deploy/docker-compose.yml"
    $DC up -d --build
    $DC exec -T db psql -U musiwatch -d musiwatch < api/db/seed_300k.sql
    $DC exec -T db psql -U musiwatch -d musiwatch < api/db/explain_one_day.sql      # catat: Seq Scan

    # E1: N+1, tanpa indeks
    export TOKEN=$(scripts/login.sh)
    BASE_URL=https://localhost:8443 k6 run -e OUT=e1.json loadtest/summary.js

    # E2: tambah indeks
    $DC exec -T db psql -U musiwatch -d musiwatch < api/db/e2_create_index.sql
    $DC exec -T db psql -U musiwatch -d musiwatch < api/db/explain_one_day.sql      # catat: Index/Bitmap Scan
    export TOKEN=$(scripts/login.sh)
    BASE_URL=https://localhost:8443 k6 run -e OUT=e2.json loadtest/summary.js

    # E3: satu query GROUP BY
    SUMMARY_MODE=groupby $DC up -d api
    export TOKEN=$(scripts/login.sh)
    BASE_URL=https://localhost:8443 k6 run -e OUT=e3.json loadtest/summary.js

Selama tiap eksperimen jalankan `docker stats` untuk CPU database, dan buka Grafana di http://127.0.0.1:3000
dengan tiga query panel RED dari modul.

Setiap run mencetak throughput, p50/p95/p99, error rate, dan status threshold di layar, lalu menyimpan
ringkasan lengkap ke file `OUT` (e1.json, e2.json, e3.json). Opsi lama `--summary-export` tidak dipakai lagi.

Catatan: uji 5 menit 20 VU memakai token yang sama; token kedaluwarsa setelah 15 menit, jadi cukup untuk satu eksperimen.
