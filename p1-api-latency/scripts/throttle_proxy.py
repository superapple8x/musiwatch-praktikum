# TAMBAHAN untuk Modul Praktikum P1 Langkah 3.
# Mengapa ada berkas ini: pada emulator 37.2.13 dengan AVD API 36, profil jaringan
# `-netdelay`/`-netspeed` (dan `adb emu network speed|delay`) TIDAK diterapkan pada lalu
# lintas data. Tamu memakai netstack eth0 (10.0.2.15), sedangkan penskalaan hanya berlaku
# pada modem seluler lama, sehingga transfer 133994 bita ke 10.0.2.2 selesai ~10 ms
# meskipun `network status` melaporkan 473600 bit/s. Bukti lengkap ada di
# logs/p1-langkah3-2026-11-10/shell-history.txt.
#
# Berkas ini menggantikan alat itu: proksi TCP lokal yang meniru profil kecepatan/latensi
# emulator, sehingga tabel "full / hsdpa / edge" pada lembar kerja bisa diisi dengan angka
# yang benar-benar berbeda. Arahkan BASE_URL aplikasi/curl ke http://10.0.2.2:9000
# (dari emulator) atau http://127.0.0.1:9000 (dari laptop).
#
# Pemakaian:
#   python3 scripts/throttle_proxy.py --profile edge      # 473,6 kbit/s, +240 ms
#   python3 scripts/throttle_proxy.py --profile hsdpa     # 13,98 Mbit/s, +117 ms
#   python3 scripts/throttle_proxy.py --profile full      # tanpa batas (kontrol)
#
# Hanya arah server->klien (unduh) yang dibatasi; permintaan HTTP terlalu kecil untuk berarti.
import argparse
import socket
import threading
import time

# bit/s diturunkan dari preset emulator: edge 473600, hsdpa 13980000, full 0 (tanpa batas).
# Latensi memakai titik tengah rentang preset: edge 80-400 ms, hsdpa 35-200 ms.
PROFILES = {
    "full": {"rate": 0, "delay": 0.0},
    "hsdpa": {"rate": 13980000 // 8, "delay": 0.117},
    "edge": {"rate": 473600 // 8, "delay": 0.240},
}


def relay(src, dst, rate, delay, throttle, stats=None, key=None, state=None):
    """Salin src->dst. Bila throttle, kecepatan byte kumulatif dipatok ke `rate` B/dtk.

    Latensi (`delay`) diterapkan PER PERMINTAAN HTTP, bukan sekali per koneksi: saat
    arah unggah melihat akhir header (`\\r\\n\\r\\n`), state["delay_left"] diisi, lalu
    arah unduh menunggu selama itu sebelum mengirim bita pertama balasan. Tanpa ini,
    permintaan kedua pada koneksi keep-alive tidak akan pernah terkena delay.
    """
    total = 0
    try:
        sent = 0
        start = time.monotonic()
        while True:
            chunk = src.recv(16384)
            if not chunk:
                break
            if stats is not None and key is not None and (key + "_t") not in stats:
                stats[key + "_t"] = time.monotonic()   # saat bita pertama arah ini terlihat
            if state is not None and key == "up" and b"\r\n\r\n" in chunk:
                state["delay_left"] = delay            # satu delay per permintaan
            if throttle and state is not None and key == "down" and state.get("delay_left", 0.0) > 0:
                d = state.pop("delay_left")
                time.sleep(d)
                # Jadwal bitrate mulai SETELAH latensi, supaya total = latensi + transfer
                # (bukan max keduanya).
                start = time.monotonic()
                sent = 0
            if throttle and rate > 0:
                sent += len(chunk)
                ahead = sent / rate - (time.monotonic() - start)
                if ahead > 0:
                    time.sleep(ahead)
            dst.sendall(chunk)
            total += len(chunk)
    except OSError:
        pass
    finally:
        for s in (src, dst):
            try:
                s.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
        if stats is not None and key is not None:
            stats[key] = total
    return total


def handle(client, target, profile, name):
    peer = client.getpeername()[0]
    t0 = time.monotonic()
    print(f"[{name}] koneksi dari {peer}", flush=True)
    upstream = socket.create_connection(target, timeout=10)
    print(f"[{name}] tersambung ke {target[0]}:{target[1]} pada +{(time.monotonic() - t0) * 1000:.1f} ms", flush=True)
    for s in (client, upstream):
        s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    # unggah (klien->server) tanpa batas; unduh (server->klien) dibatasi
    stats = {}
    state = {}
    down = threading.Thread(target=relay, args=(upstream, client, profile["rate"], profile["delay"], True, stats, "down", state), daemon=True)
    up = threading.Thread(target=relay, args=(client, upstream, 0, profile["delay"], False, stats, "up", state), daemon=True)
    down.start()
    up.start()
    down.join()
    up.join()
    client.close()
    upstream.close()
    def at(k):
        return f"+{(stats[k] - t0) * 1000:.1f} ms" if k in stats else "-"
    print(
        f"[{name}] selesai {peer}: permintaan {at('up_t')}, bita-pertama-balasan {at('down_t')}, "
        f"turun {stats.get('down', 0)} bita / naik {stats.get('up', 0)} bita, total {(time.monotonic() - t0) * 1000:.0f} ms",
        flush=True,
    )


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--listen", type=int, default=9000)
    ap.add_argument("--target", default="127.0.0.1:8000")
    ap.add_argument("--profile", choices=sorted(PROFILES), default="edge")
    # TAMBAHAN: nilai eksplisit untuk percobaan (mis. menguji batas kuantisasi emulator).
    ap.add_argument("--delay-ms", type=float, default=None, help="ganti latensi profil (ms)")
    ap.add_argument("--rate-bps", type=float, default=None, help="ganti kecepatan profil (bit/s, 0 = tanpa batas)")
    args = ap.parse_args()

    host, port = args.target.rsplit(":", 1)
    target = (host, int(port))
    profile = dict(PROFILES[args.profile])
    if args.delay_ms is not None:
        profile["delay"] = args.delay_ms / 1000.0
    if args.rate_bps is not None:
        profile["rate"] = args.rate_bps / 8.0
    label = "tanpa batas" if not profile["rate"] else f"{profile['rate'] * 8:.0f} bit/s"
    print(f"proksi {args.profile} ({label}, +{profile['delay'] * 1000:.0f} ms) :{args.listen} -> {args.target}", flush=True)

    srv = socket.socket()
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("0.0.0.0", args.listen))
    srv.listen(64)
    while True:
        client, _ = srv.accept()
        threading.Thread(target=handle, args=(client, target, profile, args.profile), daemon=True).start()


if __name__ == "__main__":
    main()
