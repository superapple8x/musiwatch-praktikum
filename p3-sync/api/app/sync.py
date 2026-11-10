# Sumber: Modul Praktikum P3, Langkah 2 (verbatim)
import os
from datetime import datetime
from uuid import UUID
from fastapi import APIRouter
from pydantic import BaseModel, Field
from psycopg.rows import dict_row
from psycopg_pool import ConnectionPool

pool = ConnectionPool(os.environ["DATABASE_URL"], min_size=1, max_size=10, open=True)
router = APIRouter(prefix="/v1/sync")

class Change(BaseModel):
    id: UUID
    base_version: int | None = None     # None = data baru dari klien
    station_id: str
    officer_id: str
    ph: float | None = Field(None, ge=0, le=14)
    turbidity_ntu: float | None = Field(None, ge=0)
    temp_c: float | None = None
    note: str | None = None
    measured_at: datetime
    deleted: bool = False

class PushBatch(BaseModel):
    changes: list[Change] = Field(max_length=500)

@router.post("/push")
def push(batch: PushBatch):
    results = []
    with pool.connection() as conn:          # satu transaksi; commit di akhir blok
        conn.execute("SELECT pg_advisory_xact_lock(42)")  # serialisasi penulis (lihat catatan)
        for c in batch.changes:
            row = conn.execute("SELECT version FROM measurements WHERE id = %s FOR UPDATE", (c.id,)).fetchone()
            if row is None:
                conn.execute(
                    """INSERT INTO measurements (id, station_id, officer_id, ph, turbidity_ntu, temp_c,
                       note, measured_at, deleted, change_seq)
                       VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s, nextval('change_seq_gen'))""",
                    (c.id, c.station_id, c.officer_id, c.ph, c.turbidity_ntu, c.temp_c, c.note, c.measured_at, c.deleted))
                results.append({"id": str(c.id), "status": "created", "version": 1})
            elif c.base_version is None:
                results.append({"id": str(c.id), "status": "duplicate", "version": row[0]})
            elif c.base_version == row[0]:
                conn.execute(
                    """UPDATE measurements SET ph=%s, turbidity_ntu=%s, temp_c=%s, note=%s, deleted=%s,
                       version = version + 1, change_seq = nextval('change_seq_gen') WHERE id = %s""",
                    (c.ph, c.turbidity_ntu, c.temp_c, c.note, c.deleted, c.id))
                results.append({"id": str(c.id), "status": "updated", "version": row[0] + 1})
            else:
                results.append({"id": str(c.id), "status": "conflict", "server_version": row[0]})
    return {"results": results}

@router.get("/pull")
def pull(since: int = 0, limit: int = 200):
    limit = max(1, min(limit, 500))
    with pool.connection() as conn:
        rows = conn.cursor(row_factory=dict_row).execute(
            "SELECT * FROM measurements WHERE change_seq > %s ORDER BY change_seq LIMIT %s",
            (since, limit)).fetchall()
    next_cursor = rows[-1]["change_seq"] if rows else since
    return {"changes": rows, "next_cursor": next_cursor, "has_more": len(rows) == limit}
