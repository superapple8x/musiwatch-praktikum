# TAMBAHAN: fixture untuk Modul Praktikum P4a Langkah 3 (modul hanya menyebut nama fixture).
# Token dibuat langsung dengan kunci privat, jadi tabel users tidak dibutuhkan untuk pengujian.
# Butuh database dengan skema 001_init.sql: set DATABASE_URL sebelum menjalankan pytest.
import os, tempfile, time
import pytest
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

def _ensure_keys():
    """Bila JWT_*_FILE belum diset (mis. di laptop), buat pasangan kunci sementara."""
    if os.environ.get("JWT_PRIVATE_KEY_FILE") and os.environ.get("JWT_PUBLIC_KEY_FILE"):
        return
    d = tempfile.mkdtemp()
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    priv, pub = os.path.join(d, "jwt.pem"), os.path.join(d, "jwt.pub")
    with open(priv, "wb") as f:
        f.write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                                  serialization.NoEncryption()))
    with open(pub, "wb") as f:
        f.write(key.public_key().public_bytes(serialization.Encoding.PEM,
                                              serialization.PublicFormat.SubjectPublicKeyInfo))
    os.environ["JWT_PRIVATE_KEY_FILE"], os.environ["JWT_PUBLIC_KEY_FILE"] = priv, pub

_ensure_keys()

import jwt  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402
from app.auth import AUD, ISS, PRIVATE_KEY  # noqa: E402
from app.main import app  # noqa: E402

def make_token(**override) -> str:
    now = int(time.time())
    claims = {"sub": "u-ib-1", "role": "petugas", "region": "ilir-barat",
              "iss": ISS, "aud": AUD, "iat": now, "exp": now + 900}
    claims.update(override)
    return jwt.encode(claims, PRIVATE_KEY, algorithm="RS256")

@pytest.fixture(scope="session")
def client():
    with TestClient(app) as c:
        yield c

@pytest.fixture
def token_ilir_barat():
    return make_token()

@pytest.fixture
def token_audiens_lain():
    return make_token(aud="dashboard-web")

@pytest.fixture
def token_kedaluwarsa():
    return make_token(iat=int(time.time()) - 1000, exp=int(time.time()) - 60)
