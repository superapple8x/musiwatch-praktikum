# Sumber: Modul Praktikum P4a, Langkah 2 ("Tambahkan GET /v1/stations/{id}/measurements").
# Kode endpoint ditulis lengkap di sini karena modul hanya mendeskripsikannya.
from fastapi import APIRouter, Depends, HTTPException
from psycopg.rows import dict_row
from .auth import current_user, require_station_access
from .stations import STATIONS_BY_ID
from .sync import pool

router = APIRouter(prefix="/v1/stations")

def station_or_404(station_id: str) -> dict:
    station = STATIONS_BY_ID.get(station_id)
    if station is None:
        raise HTTPException(404, "stasiun tidak ditemukan")
    return station

@router.get("/{station_id}/measurements")
def list_measurements(station_id: str, limit: int = 100, user: dict = Depends(current_user)):
    station = station_or_404(station_id)
    require_station_access(station["region"], user)          # otorisasi objek (cegah BOLA)
    limit = max(1, min(limit, 500))
    with pool.connection() as conn:
        rows = conn.cursor(row_factory=dict_row).execute(
            """SELECT id, ph, turbidity_ntu, temp_c, note, measured_at, version FROM measurements
               WHERE station_id = %s AND NOT deleted ORDER BY measured_at DESC LIMIT %s""",
            (station_id, limit)).fetchall()
    return {"station_id": station_id, "items": rows}


# ---------------------------------------------------------------------------
# Praktik 4b, Langkah 3: endpoint ringkasan untuk eksperimen E1-E3.
# TAMBAHAN: kedua versi disediakan dan dipilih lewat env SUMMARY_MODE agar dry run tidak perlu mengedit kode.
#   SUMMARY_MODE=n1      -> versi awal modul (satu query per hari)          [E1, E2]
#   SUMMARY_MODE=groupby -> satu query GROUP BY (perbaikan E3)
# Mahasiswa tetap diminta menulis versi groupby sendiri; di repositori mereka hanya ada satu versi.
import os  # noqa: E402

SUMMARY_MODE = os.environ.get("SUMMARY_MODE", "n1")

@router.get("/{station_id}/summary")
def summary(station_id: str, days: int = 30, user: dict = Depends(current_user)):
    station = station_or_404(station_id)
    require_station_access(station["region"], user)
    days = max(1, min(days, 90))
    if SUMMARY_MODE == "groupby":
        return _summary_groupby(station_id, days)
    out = []
    with pool.connection() as conn:
        for d in range(days):
            avg_ph, max_ntu, n = conn.execute(
                """SELECT avg(ph), max(turbidity_ntu), count(*) FROM measurements
                   WHERE station_id = %s
                     AND measured_at >= date_trunc('day', now()) - make_interval(days => %s)
                     AND measured_at <  date_trunc('day', now()) - make_interval(days => %s) + interval '1 day'""",
                (station_id, d, d)).fetchone()
            out.append({"day_offset": d, "avg_ph": avg_ph, "max_ntu": max_ntu, "n": n})
    return out

def _summary_groupby(station_id: str, days: int):
    with pool.connection() as conn:
        rows = conn.execute(
            """SELECT (date_trunc('day', now())::date - date_trunc('day', measured_at)::date) AS day_offset,
                      avg(ph), max(turbidity_ntu), count(*)
               FROM measurements
               WHERE station_id = %s
                 AND measured_at >= date_trunc('day', now()) - make_interval(days => %s - 1)
                 AND measured_at <  date_trunc('day', now()) + interval '1 day'
               GROUP BY 1""", (station_id, days)).fetchall()
    by_day = {r[0]: r for r in rows}
    return [{"day_offset": d,
             "avg_ph": by_day[d][1] if d in by_day else None,
             "max_ntu": by_day[d][2] if d in by_day else None,
             "n": by_day[d][3] if d in by_day else 0} for d in range(days)]
