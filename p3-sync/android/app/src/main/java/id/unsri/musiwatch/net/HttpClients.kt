// Sumber: Modul Praktikum P1, Langkah 4 (konfigurasi klien).
// TAMBAHAN: fungsi uji /v1/flaky dan muat stasiun, sebagai kerangka layar praktik.
// TAMBAHAN Langkah 3: BASE_URL dapat diganti ke proksi throttle (scripts/throttle_proxy.py, port 9000)
// karena profil jaringan emulator tidak diterapkan pada lalu lintas data (lihat docs/P1.md).
package id.unsri.musiwatch.net

import android.os.SystemClock
import android.util.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response

const val BASE_URL = "http://10.0.2.2:8000"   // localhost laptop dilihat dari emulator

/** Proksi throttle di laptop (scripts/throttle_proxy.py). 10.0.2.2:9000 = host:9000. */
const val BASE_URL_THROTTLED = "http://10.0.2.2:9000"

/**
 * TAMBAHAN: perekam waktu OkHttp. Memisahkan connect / kirim header / terima header / unduh
 * body, supaya terlihat di mana ~500 ms yang konstan itu sebenarnya dihabiskan.
 */
class TimingListener(private val label: String) : EventListener() {
    private var t0 = SystemClock.elapsedRealtime()
    private fun ms() = SystemClock.elapsedRealtime() - t0
    private fun log(ev: String) {
        Log.d("MusiWatchTIMING", "$label +${ms()} ms $ev")
    }

    override fun callStart(call: Call) {
        t0 = SystemClock.elapsedRealtime()
        log("callStart ${call.request().url}")
    }
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) = log("connectStart")
    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) = log("connectEnd $protocol")
    override fun requestHeadersEnd(call: Call, request: Request) = log("requestHeadersEnd")
    override fun responseHeadersStart(call: Call) = log("responseHeadersStart")
    override fun responseHeadersEnd(call: Call, response: Response) = log("responseHeadersEnd ${response.code}")
    override fun responseBodyEnd(call: Call, byteCount: Long) = log("responseBodyEnd $byteCount B")
    override fun callEnd(call: Call) = log("callEnd")
    override fun callFailed(call: Call, ioe: IOException) = log("callFailed ${ioe.message}")
}

fun buildClient(withRetry: Boolean, label: String? = null): OkHttpClient {
    val b = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
    if (withRetry) b.addInterceptor(RetryInterceptor())
    if (label != null) b.eventListener(TimingListener(label))
    return b.build()
}

data class CallResult(val ok: Boolean, val code: Int?, val durationMs: Long)

/** TAMBAHAN: hasil muat dengan waktu ke header (TTFB) dan waktu total (termasuk unduh body). */
data class LoadResult(val headMs: Long, val totalMs: Long, val bytes: Int, val body: String)

/** Panggil /v1/flaky sebanyak [n] kali; jalankan dari coroutine (bukan main thread). */
suspend fun runFlakyTest(client: OkHttpClient, n: Int = 20): List<CallResult> =
    withContext(Dispatchers.IO) {
        (1..n).map {
            val t0 = SystemClock.elapsedRealtime()
            try {
                client.newCall(Request.Builder().url("$BASE_URL/v1/flaky").build()).execute().use { r ->
                    CallResult(r.isSuccessful, r.code, SystemClock.elapsedRealtime() - t0)
                }
            } catch (e: IOException) {
                CallResult(false, null, SystemClock.elapsedRealtime() - t0)
            }
        }
    }

/** Muat satu halaman stasiun; kembalikan durasi (ms) dan body JSON mentah. */
suspend fun loadStations(client: OkHttpClient, limit: Int = 50, offset: Int = 0): Pair<Long, String> =
    withContext(Dispatchers.IO) {
        val t0 = SystemClock.elapsedRealtime()
        client.newCall(Request.Builder().url("$BASE_URL/v1/stations?limit=$limit&offset=$offset").build())
            .execute().use { r -> (SystemClock.elapsedRealtime() - t0) to (r.body?.string() ?: "") }
    }

// TAMBAHAN: varian yang menerima baseUrl, agar layar bisa membandingkan langsung vs proksi throttle.
suspend fun runFlakyTest(client: OkHttpClient, baseUrl: String, n: Int = 20): List<CallResult> =
    withContext(Dispatchers.IO) {
        (1..n).map {
            val t0 = SystemClock.elapsedRealtime()
            try {
                client.newCall(Request.Builder().url("$baseUrl/v1/flaky").build()).execute().use { r ->
                    val ok = r.isSuccessful
                    val code = r.code
                    r.body?.string()   // baca body agar durasi mencakup transfer penuh
                    CallResult(ok, code, SystemClock.elapsedRealtime() - t0)
                }
            } catch (e: IOException) {
                CallResult(false, null, SystemClock.elapsedRealtime() - t0)
            }
        }
    }

/**
 * Muat stasiun lewat [baseUrl]; ukur TTFB (sampai header) dan total (sampai body habis).
 *
 * Catatan: versi sebelumnya mencatat `(elapsed) to (body.string())`, sehingga `elapsed`
 * dievaluasi SEBELUM body dibaca dan angka yang tampil hanya waktu-ke-header. Diukur
 * terpisah di sini supaya selisih profil kecepatan (yang muncul saat mengunduh body)
 * tidak hilang.
 */
suspend fun loadStations(client: OkHttpClient, baseUrl: String, limit: Int = 50, offset: Int = 0): LoadResult =
    withContext(Dispatchers.IO) {
        val t0 = SystemClock.elapsedRealtime()
        // TAMBAHAN: minta body tanpa kompresi. OkHttp otomatis meminta gzip, sehingga
        // 13 358 bita JSON menjadi ~973 bita di kabel dan batas kecepatan profil nyaris
        // tidak terasa. Dengan "identity", batas bitrate benar-benar mengukur transfer.
        client.newCall(
            Request.Builder()
                .url("$baseUrl/v1/stations?limit=$limit&offset=$offset")
                .header("Accept-Encoding", "identity")
                .build(),
        )
            .execute().use { r ->
                val headMs = SystemClock.elapsedRealtime() - t0
                val body = r.body?.string() ?: ""
                LoadResult(headMs, SystemClock.elapsedRealtime() - t0, body.length, body)
            }
    }
