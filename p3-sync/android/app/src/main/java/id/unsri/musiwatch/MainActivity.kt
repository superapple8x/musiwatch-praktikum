// Layar sinkronisasi P3 dan pemeriksaan latensi P1.
package id.unsri.musiwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.unsri.musiwatch.data.MeasurementEntity
import id.unsri.musiwatch.net.BASE_URL
import id.unsri.musiwatch.net.BASE_URL_THROTTLED
import kotlinx.coroutines.launch

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
fun Screen(
    measurementVm: MeasurementViewModel = viewModel(),
    latencyVm: MainViewModel = viewModel(),
) {
    val form by measurementVm.form.collectAsStateWithLifecycle()
    val measurements by measurementVm.measurements.collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("MusiWatch · Praktik 3", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            MeasurementFields(form, measurementVm)
        }
        item {
            Text("Pengukuran", style = MaterialTheme.typography.titleLarge)
            if (measurements.isEmpty()) Text("Belum ada pengukuran.")
        }
        items(measurements, key = { it.id }) { measurement ->
            MeasurementRow(measurement, canEdit = !form.saving) {
                measurementVm.edit(measurement)
                scope.launch { listState.scrollToItem(0) }
            }
        }
        item {
            HorizontalDivider()
        }
        item {
            LatencyControls(latencyVm)
        }
    }
}

@Composable
private fun MeasurementFields(form: MeasurementForm, vm: MeasurementViewModel) {
    val isEditing = form.editing != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (isEditing) "Ubah pengukuran" else "Tambah pengukuran",
            style = MaterialTheme.typography.titleLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.stationId,
                onValueChange = { value -> vm.updateForm { it.copy(stationId = value) } },
                modifier = Modifier.weight(1f),
                label = { Text("ID stasiun") },
                enabled = !form.saving && !isEditing,
                singleLine = true,
            )
            OutlinedTextField(
                value = form.officerId,
                onValueChange = { value -> vm.updateForm { it.copy(officerId = value) } },
                modifier = Modifier.weight(1f),
                label = { Text("ID petugas") },
                enabled = !form.saving && !isEditing,
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = form.ph,
            onValueChange = { value -> vm.updateForm { it.copy(ph = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("pH") },
            enabled = !form.saving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = form.note,
            onValueChange = { value -> vm.updateForm { it.copy(note = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Catatan (opsional)") },
            enabled = !form.saving,
            singleLine = true,
        )
        form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = vm::save, enabled = !form.saving, modifier = Modifier.fillMaxWidth()) {
            Text("Simpan pengukuran")
        }
        if (isEditing) {
            TextButton(onClick = vm::cancelEdit, enabled = !form.saving) { Text("Batal") }
        }
        TextButton(onClick = vm::syncNow) { Text("Sinkronkan sekarang") }
    }
}

@Composable
private fun MeasurementRow(measurement: MeasurementEntity, canEdit: Boolean, onEdit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${measurement.stationId} · ${measurement.officerId}", style = MaterialTheme.typography.titleMedium)
        Text("pH ${measurement.ph ?: "—"} · ${measurement.syncState}")
        measurement.note?.let { Text(it) }
        if (measurement.syncState == "CONFLICT") {
            Text("Data di server sudah berubah. Perubahan lokal tetap tersimpan.")
        }
        if (measurement.syncState == "SYNCED") {
            TextButton(onClick = onEdit, enabled = canEdit) { Text("Ubah") }
        }
        HorizontalDivider()
    }
}

@Composable
private fun LatencyControls(vm: MainViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Praktik 1 · Latensi API", style = MaterialTheme.typography.titleLarge)
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
