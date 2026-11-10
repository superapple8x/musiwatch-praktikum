// Sumber: Modul Praktikum P1, Langkah 4 (konfigurasi klien).
// TAMBAHAN: fungsi uji /v1/flaky dan muat stasiun, sebagai kerangka layar praktik.
package id.unsri.musiwatch.net

import android.os.SystemClock
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

const val BASE_URL = "http://10.0.2.2:8000"   // localhost laptop dilihat dari emulator

fun buildClient(withRetry: Boolean): OkHttpClient {
    val b = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
    if (withRetry) b.addInterceptor(RetryInterceptor())
    return b.build()
}

data class CallResult(val ok: Boolean, val code: Int?, val durationMs: Long)

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
