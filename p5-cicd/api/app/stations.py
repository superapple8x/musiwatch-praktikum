# TAMBAHAN P4a: data stasiun dipindah dari main.py (isi sama dengan P1) agar dapat dipakai
# router measurements dan sync tanpa impor melingkar.
STATIONS = [{"id": f"S-{i:03d}", "name": f"Stasiun {i}",
             "region": "ilir-barat" if i % 2 else "seberang-ulu",
             "lat": round(-2.99 + i * 0.001, 6), "lon": round(104.75 + i * 0.001, 6),
             "description": "Titik sampel tepian Sungai Musi. " * 5}
            for i in range(1, 501)]
STATIONS_BY_ID = {s["id"]: s for s in STATIONS}

def station_ids_in_region(region: str | None) -> list[str]:
    return [s["id"] for s in STATIONS if s["region"] == region]
