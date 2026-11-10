# Sumber: Modul Praktikum P4a, Langkah 2 (verbatim).
# TAMBAHAN: implementasi find_user (modul hanya menyebut "SELECT dari tabel users").
# Catatan Revisi 2: /v1/auth/login menerima password langsung karena API dan aplikasi milik sendiri (lab).
# Untuk login lewat penyedia identitas OAuth 2.0, RFC 9700 melarang ROPC; pakai Authorization Code + PKCE.
import os, time, uuid, bcrypt, jwt
from fastapi import APIRouter, Header, HTTPException  # modul: + Depends (tidak terpakai, F401 di ruff)
from pydantic import BaseModel

PRIVATE_KEY = open(os.environ["JWT_PRIVATE_KEY_FILE"]).read()
PUBLIC_KEY = open(os.environ["JWT_PUBLIC_KEY_FILE"]).read()
ISS, AUD, TTL = "https://auth.musiwatch.local/", "musiwatch-api", 900
router = APIRouter()

class Login(BaseModel):
    username: str
    password: str

def find_user(username: str) -> dict | None:
    # Impor di dalam fungsi untuk menghindari impor melingkar (sync.py mengimpor current_user dari modul ini)
    from psycopg.rows import dict_row
    from .sync import pool
    with pool.connection() as conn:
        return conn.cursor(row_factory=dict_row).execute(
            "SELECT id, username, password_hash, role, region FROM users WHERE username = %s",
            (username,)).fetchone()

@router.post("/v1/auth/login")
def login(body: Login):
    user = find_user(body.username)        # SELECT dari tabel users
    if not user or not bcrypt.checkpw(body.password.encode(), user["password_hash"].encode()):
        raise HTTPException(401, "username atau kata sandi salah")   # pesan sama untuk keduanya
    now = int(time.time())
    claims = {"sub": user["id"], "role": user["role"], "region": user["region"],
              "iss": ISS, "aud": AUD, "iat": now, "exp": now + TTL, "jti": str(uuid.uuid4())}
    return {"access_token": jwt.encode(claims, PRIVATE_KEY, algorithm="RS256"),
            "token_type": "Bearer", "expires_in": TTL}

def current_user(authorization: str | None = Header(default=None)) -> dict:
    scheme, _, token = (authorization or "").partition(" ")
    if scheme.lower() != "bearer" or not token:
        raise HTTPException(401, "token tidak ada")
    try:
        return jwt.decode(token, PUBLIC_KEY, algorithms=["RS256"], audience=AUD, issuer=ISS)
    except jwt.PyJWTError:
        raise HTTPException(401, "token tidak valid")

def require_station_access(station_region: str, user: dict) -> None:
    if user["role"] != "admin" and station_region != user["region"]:
        raise HTTPException(403, "stasiun di luar wilayah Anda")
