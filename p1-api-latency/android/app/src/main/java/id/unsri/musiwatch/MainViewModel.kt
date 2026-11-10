// TAMBAHAN: layar praktik P1 (Modul Langkah 4 hanya mendeskripsikan dua tombol).
// Status disimpan di ViewModel agar hasil tidak hilang saat rotasi layar.
package id.unsri.musiwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.unsri.musiwatch.net.BASE_URL
import id.unsri.musiwatch.net.CallResult
import id.unsri.musiwatch.net.buildClient
import id.unsri.musiwatch.net.loadStations
import id.unsri.musiwatch.net.runFlakyTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlakySummary(val label: String, val total: Int, val ok: Int, val failed: Int, val durationMs: Long)

data class UiState(
    val baseUrl: String = BASE_URL,
    val busy: Boolean = false,
    val stationsLine: String = "Belum dijalankan.",
    val flaky: List<FlakySummary> = emptyList(),
)

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // TAMBAHAN: klien dipakai ulang antar-tap. Membuat OkHttpClient baru setiap kali
    // memaksa koneksi TCP baru, dan koneksi baru ke 10.0.2.2 selalu memakan ~500 ms
    // (terukur: logcat OkHttp + log proksi). Dengan satu klien, panggilan kedua dst.
    // memakai koneksi yang sudah terbuka sehingga selisih profil terlihat.
    private val client = buildClient(withRetry = false, label = "muat50")

    fun setBaseUrl(url: String) = _state.update { it.copy(baseUrl = url) }

    /** Tombol (a): muat 50 stasiun, tampilkan durasi. */
    fun load50() {
        if (_state.value.busy) return
        viewModelScope.launch {
            val base = _state.value.baseUrl
            _state.update { it.copy(busy = true) }
            // TAMBAHAN: tanpa runCatching, IOException (mis. 9000 mati) mematikan aplikasi.
            runCatching {
                // Panggilan pemanasan: membuka/menghangatkan koneksi TCP. Hasilnya dibuang,
                // karena koneksi baru ke 10.0.2.2 selalu ~500 ms dan itu bukan efek profil.
                val warm = loadStations(client, base, limit = 50)
                warm to loadStations(client, base, limit = 50)
            }.onSuccess { (warm, res) ->
                val count = Regex("\"id\":").findAll(res.body).count()
                _state.update {
                    it.copy(
                        busy = false,
                        stationsLine = "50 stasiun: ${res.totalMs} ms (TTFB ${res.headMs} ms), " +
                            "${res.bytes} bita, $count item, pemanasan ${warm.totalMs} ms, base=$base",
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(busy = false, stationsLine = "gagal: ${e.javaClass.simpleName}: ${e.message}, base=$base") }
            }
        }
    }

    /** Tombol (b): /v1/flaky 20x dengan dan tanpa retry. */
    fun runFlaky() {
        if (_state.value.busy) return
        viewModelScope.launch {
            val base = _state.value.baseUrl
            _state.update { it.copy(busy = true) }
            val without = runFlakyTest(buildClient(withRetry = false), base, n = 20)
            val with = runFlakyTest(buildClient(withRetry = true), base, n = 20)
            _state.update {
                it.copy(busy = false, flaky = listOf(summarize("tanpa retry", without), summarize("dengan retry", with)))
            }
        }
    }

    /** Total durasi = jumlah waktu tiap panggilan (termasuk backoff pada retry). */
    private fun summarize(label: String, results: List<CallResult>) = FlakySummary(
        label = label,
        total = results.size,
        ok = results.count { it.ok },
        failed = results.count { !it.ok },
        durationMs = results.sumOf { it.durationMs },
    )
}
