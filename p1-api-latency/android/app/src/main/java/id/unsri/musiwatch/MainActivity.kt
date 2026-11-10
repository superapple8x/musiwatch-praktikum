// TAMBAHAN: layar praktik P1. Dua tombol sesuai Modul Langkah 4, plus pemilih base URL
// untuk membandingkan jalur langsung (:8000) dan proksi throttle (:9000).
package id.unsri.musiwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.unsri.musiwatch.net.BASE_URL
import id.unsri.musiwatch.net.BASE_URL_THROTTLED

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // targetSdk 36 memaksa edge-to-edge
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { Screen() }
            }
        }
    }
}

@Composable
fun Screen(vm: MainViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)   // agar tidak tertutup status/navigation bar
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("MusiWatch - Praktik 1", style = MaterialTheme.typography.headlineSmall)
        Text("Base URL", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = s.baseUrl == BASE_URL,
                onClick = { vm.setBaseUrl(BASE_URL) },
                label = { Text("langsung :8000") },
            )
            FilterChip(
                selected = s.baseUrl == BASE_URL_THROTTLED,
                onClick = { vm.setBaseUrl(BASE_URL_THROTTLED) },
                label = { Text("proksi :9000") },
            )
        }
        Text(s.baseUrl, style = MaterialTheme.typography.bodySmall)

        Button(onClick = vm::load50, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Muat 50 stasiun")
        }
        Text(s.stationsLine)

        HorizontalDivider()

        Button(onClick = vm::runFlaky, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Uji /v1/flaky 20x (dengan & tanpa retry)")
        }
        s.flaky.forEach { f ->
            Text("${f.label}: sukses ${f.ok}/${f.total}, gagal ${f.failed}, total ${f.durationMs} ms")
        }
        if (s.busy) Text("Berjalan...", style = MaterialTheme.typography.bodySmall)
    }
}
