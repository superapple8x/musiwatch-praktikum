-- Sumber: Modul Praktikum P4a, Langkah 2 (tabel users).
-- TAMBAHAN: data contoh dengan hash bcrypt dibuat oleh pgcrypto (format $2a$, diterima bcrypt.checkpw).
-- Kata sandi berikut HANYA untuk lab. Bila volume DB dari P3 sudah ada, file ini tidak dijalankan otomatis:
-- gunakan scripts/apply_users.sh.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE IF NOT EXISTS users (
  id            TEXT PRIMARY KEY,
  username      TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  role          TEXT NOT NULL CHECK (role IN ('petugas', 'supervisor', 'admin')),
  region        TEXT
);
INSERT INTO users (id, username, password_hash, role, region) VALUES
  ('u-ib-1',  'petugas.ib', crypt('musiwatch-lab-1',     gen_salt('bf', 10)), 'petugas', 'ilir-barat'),
  ('u-su-1',  'petugas.su', crypt('musiwatch-lab-2',     gen_salt('bf', 10)), 'petugas', 'seberang-ulu'),
  ('u-adm-1', 'admin',      crypt('musiwatch-lab-admin', gen_salt('bf', 10)), 'admin',   NULL)
ON CONFLICT (id) DO NOTHING;
