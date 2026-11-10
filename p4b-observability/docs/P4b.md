# Lembar Kerja Praktik 4b - Observability dan Uji Beban (P4.2)

Nama/NIM:            Tag Git: p4b-final

## 1. Bukti observability
Tiga baris log JSON dengan `rid` yang sama dengan header respons; tangkapan layar tiga panel RED.

## 2. Tabel eksperimen
| Eksperimen | Throughput | p50 | p95 | p99 | Error rate | CPU DB | Threshold k6 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E1 N+1, tanpa indeks | | | | | | | |
| E2 dengan indeks | | | | | | | |
| E3 satu query GROUP BY | | | | | | | |

## 3. Diagnosis
Di mana waktu habis pada E1 (EXPLAIN ANALYZE, jumlah query per permintaan); mengapa E2 dan E3 memperbaikinya;
perbaikan mana paling berpengaruh, dengan angka.

## 4. Usulan SLO dan alert
SLI, target, periode; satu aturan alert berbasis gejala.

## 5. Health check
Perilaku /health/live dan /health/ready saat kontainer database dihentikan.

## Sumber dan bantuan (termasuk AI generatif)
