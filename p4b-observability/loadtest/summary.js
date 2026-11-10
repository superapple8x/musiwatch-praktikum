// Sumber: Modul Praktikum P4b, Langkah 4.
// PERUBAHAN Revisi 2: hasil disimpan lewat handleSummary(), bukan opsi --summary-export.
//   k6 v2 menghapus mode ringkasan lama; handleSummary() berjalan di k6 v0.30 ke atas, v1, dan v2.
// Jalankan: BASE_URL=https://localhost:8443 TOKEN=$(scripts/login.sh) k6 run -e OUT=e1.json loadtest/summary.js
// Bila k6 menolak sertifikat mkcert, tambahkan env K6_INSECURE_SKIP_TLS_VERIFY=true (lab saja).
import http from 'k6/http';
import { check, sleep } from 'k6';
export const options = {
  stages: [{ duration: '1m', target: 20 }, { duration: '3m', target: 20 }, { duration: '30s', target: 0 }],
  thresholds: { http_req_duration: ['p(95)<800'], http_req_failed: ['rate<0.01'] },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],   // p99 tidak ada di ringkasan bawaan
};
const stations = ['S-001', 'S-003', 'S-005', 'S-007', 'S-009'];
export default function () {
  const id = stations[Math.floor(Math.random() * stations.length)];
  const res = http.get(`${__ENV.BASE_URL}/v1/stations/${id}/summary?days=30`,
                       { headers: { Authorization: `Bearer ${__ENV.TOKEN}` } });
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}

// Ringkasan: satu file JSON lengkap untuk lampiran, dan beberapa baris di layar untuk tabel lembar kerja.
export function handleSummary(data) {
  const d = data.metrics.http_req_duration.values;
  const f = data.metrics.http_req_failed.values;
  const r = data.metrics.http_reqs.values;
  const lulus = Object.values(data.metrics)
    .flatMap((m) => Object.values(m.thresholds || {}))
    .every((t) => t.ok);
  const ms = (x) => `${x.toFixed(1)} ms`;
  const baris = [
    `throughput : ${r.rate.toFixed(2)} req/s (${r.count} permintaan)`,
    `p50/p95/p99: ${ms(d.med)} / ${ms(d['p(95)'])} / ${ms(d['p(99)'])}`,
    `error rate : ${(f.rate * 100).toFixed(2)} %`,
    `threshold  : ${lulus ? 'LULUS' : 'GAGAL'}`,
  ];
  return {
    stdout: baris.join('\n') + '\n',
    [__ENV.OUT || 'summary.json']: JSON.stringify(data, null, 2),
  };
}
