-- Eksperimen E2 (Modul P4b Langkah 4)
CREATE INDEX IF NOT EXISTS idx_meas_station_time ON measurements (station_id, measured_at DESC);
ANALYZE measurements;
