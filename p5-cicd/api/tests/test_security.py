# Sumber: Modul Praktikum P4a, Langkah 3 (verbatim) + TAMBAHAN: token kedaluwarsa dan push lintas wilayah.
import base64, json, uuid

def b64(d):
    return base64.urlsafe_b64encode(json.dumps(d).encode()).rstrip(b"=").decode()

def test_tanpa_token_401(client):
    assert client.get("/v1/stations/S-001/measurements").status_code == 401

def test_wilayah_lain_403(client, token_ilir_barat):     # S-002 milik seberang-ulu
    r = client.get("/v1/stations/S-002/measurements", headers={"Authorization": f"Bearer {token_ilir_barat}"})
    assert r.status_code == 403

def test_wilayah_sendiri_200(client, token_ilir_barat):
    r = client.get("/v1/stations/S-001/measurements", headers={"Authorization": f"Bearer {token_ilir_barat}"})
    assert r.status_code == 200

def test_alg_none_ditolak(client):
    forged = b64({"alg": "none", "typ": "JWT"}) + "." + b64({"sub": "x", "role": "admin"}) + "."
    assert client.get("/v1/stations", headers={"Authorization": f"Bearer {forged}"}).status_code == 401

def test_audiens_salah_ditolak(client, token_audiens_lain):
    assert client.get("/v1/stations", headers={"Authorization": f"Bearer {token_audiens_lain}"}).status_code == 401

def test_token_kedaluwarsa_ditolak(client, token_kedaluwarsa):
    assert client.get("/v1/stations", headers={"Authorization": f"Bearer {token_kedaluwarsa}"}).status_code == 401

def test_push_wilayah_lain_ditolak(client, token_ilir_barat):
    body = {"changes": [{"id": str(uuid.uuid4()), "station_id": "S-002", "ph": 7.0,
                         "measured_at": "2026-11-10T08:00:00Z"}]}
    r = client.post("/v1/sync/push", json=body, headers={"Authorization": f"Bearer {token_ilir_barat}"})
    assert r.status_code == 200 and r.json()["results"][0]["status"] == "forbidden"
