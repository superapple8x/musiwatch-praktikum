# Sumber: Modul Praktikum P1 L1 + P2 L1 + P3 L2 + P4a L2 + P4b L1 (log terstruktur dan /metrics)
import hashlib, json, os, random, time
from datetime import datetime, timezone
from fastapi import Depends, FastAPI, Request, Response
from fastapi.middleware.gzip import GZipMiddleware
from .auth import current_user, router as auth_router
from .measurements import router as measurements_router
from .observability import setup as setup_observability
from .stations import STATIONS
from .sync import pool, router as sync_router

app = FastAPI(title="MusiWatch API - Praktik 4b")
app.add_middleware(GZipMiddleware, minimum_size=1000)
app.include_router(auth_router)
app.include_router(sync_router)
app.include_router(measurements_router)
setup_observability(app)

@app.get("/v1/stations")
def list_stations(request: Request, response: Response, limit: int = 50, offset: int = 0,
                  user: dict = Depends(current_user)):
    limit = max(1, min(limit, 500))
    page = STATIONS[offset: offset + limit]
    etag = '"' + hashlib.sha256(json.dumps(page, sort_keys=True).encode()).hexdigest()[:16] + '"'
    if request.headers.get("if-none-match") == etag:
        return Response(status_code=304, headers={"ETag": etag})
    response.headers["ETag"] = etag
    response.headers["Cache-Control"] = "private, max-age=60"   # P4a: respons bertoken jangan di-cache publik
    return {"items": page, "next_offset": offset + limit if offset + limit < len(STATIONS) else None}

@app.get("/v1/flaky")
def flaky(user: dict = Depends(current_user)):
    """Simulasi server tidak stabil: 30% 503, sisanya lambat acak."""
    if random.random() < 0.3:
        return Response(status_code=503)
    time.sleep(random.uniform(0.05, 0.4))
    return {"ok": True, "ts": datetime.now(timezone.utc).isoformat()}

DATABASE_URL = os.environ.get("DATABASE_URL", "")

@app.get("/health/live")
def live():
    return {"status": "alive"}            # tidak memeriksa dependensi

@app.get("/health/ready")
def ready(response: Response):
    try:
        with pool.connection(timeout=2) as conn:
            conn.execute("SELECT 1")
        return {"status": "ready"}
    except Exception:
        response.status_code = 503
        return {"status": "not-ready"}
