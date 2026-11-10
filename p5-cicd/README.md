# MusiWatch (template README - lengkapi untuk P5.3)

## Menjalankan dari nol
1. Prasyarat: ...
2. `cp deploy/.env.example deploy/.env` dan isi.
3. `scripts/gen_keys.sh` dan `scripts/gen_certs.sh`.
4. `docker compose -f deploy/docker-compose.yml up -d --build`
5. ...

## Pengujian
- `cd api && pytest -q` (butuh DATABASE_URL ke database dengan skema 001_init.sql)

## Deployment dan rollback
Lihat docs/RELEASING.md.
