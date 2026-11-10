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
