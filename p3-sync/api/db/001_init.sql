-- Sumber: Modul Praktikum P3, Langkah 1 (verbatim)
CREATE SEQUENCE change_seq_gen;
CREATE TABLE measurements (
  id            UUID PRIMARY KEY,               -- dibuat klien
  station_id    TEXT NOT NULL,
  officer_id    TEXT NOT NULL,
  ph            NUMERIC(4,2) CHECK (ph BETWEEN 0 AND 14),
  turbidity_ntu NUMERIC(7,2) CHECK (turbidity_ntu >= 0),
  temp_c        NUMERIC(4,1),
  note          TEXT,
  measured_at   TIMESTAMPTZ NOT NULL,
  version       INT NOT NULL DEFAULT 1,
  deleted       BOOLEAN NOT NULL DEFAULT FALSE,  -- tombstone
  change_seq    BIGINT NOT NULL                  -- penanda perubahan dari server
);
CREATE INDEX idx_meas_change_seq ON measurements (change_seq);
CREATE INDEX idx_meas_station_time ON measurements (station_id, measured_at DESC);
