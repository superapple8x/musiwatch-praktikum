-- Untuk lembar kerja: rencana eksekusi satu query hari, sebelum dan sesudah indeks.
EXPLAIN ANALYZE
SELECT avg(ph), max(turbidity_ntu), count(*) FROM measurements
WHERE station_id = 'S-001'
  AND measured_at >= date_trunc('day', now()) - make_interval(days => 3)
  AND measured_at <  date_trunc('day', now()) - make_interval(days => 3) + interval '1 day';
