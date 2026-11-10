-- Sumber: Modul Praktikum P4b, Langkah 3 (verbatim). Jalankan manual (bukan skrip init):
--   docker compose -f deploy/docker-compose.yml exec -T db psql -U musiwatch -d musiwatch < api/db/seed_300k.sql
-- isi 300.000 pengukuran sintetis
INSERT INTO measurements (id, station_id, officer_id, ph, turbidity_ntu, temp_c, measured_at, change_seq)
SELECT gen_random_uuid(), 'S-' || lpad((1 + g % 500)::text, 3, '0'), 'u-seed',
       6 + random() * 3, random() * 100, 26 + random() * 6,
       now() - make_interval(mins => g), nextval('change_seq_gen')
FROM generate_series(1, 300000) AS g;
DROP INDEX IF EXISTS idx_meas_station_time;   -- kondisi awal untuk eksperimen
