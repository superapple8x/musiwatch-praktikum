# Sumber: Modul Praktikum P4b, Langkah 1 (verbatim)
import json, logging, time, uuid
from prometheus_fastapi_instrumentator import Instrumentator

logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("musiwatch")

def setup(app):
    @app.middleware("http")
    async def access_log(request, call_next):
        rid = request.headers.get("x-request-id") or str(uuid.uuid4())
        t0 = time.perf_counter()
        response = await call_next(request)
        response.headers["x-request-id"] = rid
        log.info(json.dumps({"rid": rid, "method": request.method, "path": request.url.path,
                             "status": response.status_code,
                             "duration_ms": round((time.perf_counter() - t0) * 1000, 1)}))
        return response
    Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)
