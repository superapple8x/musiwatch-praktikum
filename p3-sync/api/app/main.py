# Sumber: Modul Praktikum P1 Langkah 1 + P2 Langkah 1 + P3 Langkah 2 (router sync, readiness ke DB)
import hashlib, json, os, random, time
from datetime import datetime, timezone
from fastapi import FastAPI, Request, Response
from fastapi.middleware.gzip import GZipMiddleware
from .sync import pool, router as sync_router

app = FastAPI(title="MusiWatch API - Praktik 1")
app.add_middleware(GZipMiddleware, minimum_size=1000)
app.include_router(sync_router)

STATIONS = [{"id": f"S-{i:03d}", "name": f"Stasiun {i}",
             "region": "ilir-barat" if i % 2 else "seberang-ulu",
             "lat": round(-2.99 + i * 0.001, 6), "lon": round(104.75 + i * 0.001, 6),
             "description": "Titik sampel tepian Sungai Musi. " * 5}
            for i in range(1, 501)]

@app.get("/v1/stations")
def list_stations(request: Request, response: Response, limit: int = 50, offset: int = 0):
    limit = max(1, min(limit, 500))
    page = STATIONS[offset: offset + limit]
    etag = '"' + hashlib.sha256(json.dumps(page, sort_keys=True).encode()).hexdigest()[:16] + '"'
    if request.headers.get("if-none-match") == etag:
        return Response(status_code=304, headers={"ETag": etag})
    response.headers["ETag"] = etag
    response.headers["Cache-Control"] = "max-age=60"
    return {"items": page, "next_offset": offset + limit if offset + limit < len(STATIONS) else None}

@app.get("/v1/flaky")
def flaky():
    """Simulasi server tidak stabil: 30% 503, sisanya lambat acak."""
    if random.random() < 0.3:
        return Response(status_code=503)
    time.sleep(random.uniform(0.05, 0.4))
    return {"ok": True, "ts": datetime.now(timezone.utc).isoformat()}

# --- Praktik 2, Langkah 1 ---
DATABASE_URL = os.environ.get("DATABASE_URL", "")

@app.get("/health/live")
def live():
    return {"status": "alive"}            # tidak memeriksa dependensi

@app.get("/health/ready")
def ready(response: Response):
    # Praktik 3: siap hanya bila database dapat dijangkau (gagal = 503)
    try:
        with pool.connection(timeout=2) as conn:
            conn.execute("SELECT 1")
        return {"status": "ready"}
    except Exception:
        response.status_code = 503
        return {"status": "not-ready"}
