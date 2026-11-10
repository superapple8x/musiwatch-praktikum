# Prosedur Rilis (template, lengkapi sesuai proyek)

## Langkah rilis
1. Pastikan `main` hijau di CI (test, build, staging).
2. Perbarui CHANGELOG.md, lalu `git tag vX.Y.Z && git push --tags`.
3. ...

## Kriteria lulus staging
- `/health/ready` 200, `/v1/stations` tanpa token 401, rollout selesai < 180 detik.
- ...

## Prosedur rollback
- Kubernetes: `kubectl rollout undo deployment/musiwatch-api`, verifikasi dengan `rollout status` dan smoke test.
- Waktu pemulihan terukur saat latihan: ... detik.

## Aturan migrasi database
- Expand-contract; tidak ada perubahan merusak (hapus/ganti nama/ubah tipe kolom) dalam satu rilis.
- ...
