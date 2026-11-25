package id.unsri.musiwatch

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.unsri.musiwatch.data.AppDb
import id.unsri.musiwatch.data.MeasurementEntity
import id.unsri.musiwatch.sync.requestSyncNow
import id.unsri.musiwatch.sync.schedulePeriodicSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeasurementForm(
    val stationId: String = "S-001",
    val officerId: String = "u-118",
    val ph: String = "",
    val note: String = "",
    val editing: MeasurementEntity? = null,
    val saving: Boolean = false,
    val error: String? = null,
)

class MeasurementViewModel(private val application: Application) : AndroidViewModel(application) {
    private val dao = AppDb.get(application).measurementDao()
    val measurements = dao.observeAll()

    private val _form = MutableStateFlow(MeasurementForm())
    val form = _form.asStateFlow()

    init {
        schedulePeriodicSync(application)
    }

    fun updateForm(update: (MeasurementForm) -> MeasurementForm) {
        _form.update { update(it).copy(error = null) }
    }

    fun edit(measurement: MeasurementEntity) {
        if (_form.value.saving || measurement.version == null) return
        _form.value = MeasurementForm(
            stationId = measurement.stationId,
            officerId = measurement.officerId,
            ph = measurement.ph?.toString().orEmpty(),
            note = measurement.note.orEmpty(),
            editing = measurement,
        )
    }

    fun cancelEdit() {
        val form = _form.value
        _form.value = MeasurementForm(stationId = form.stationId, officerId = form.officerId)
    }

    fun syncNow() {
        requestSyncNow(application)
    }

    fun save() {
        val form = _form.value
        if (form.saving) return

        val stationId = form.stationId.trim()
        val officerId = form.officerId.trim()
        val ph = form.ph.trim().replace(',', '.').toDoubleOrNull()
        if (stationId.isEmpty() || officerId.isEmpty() || ph == null || ph !in 0.0..14.0) {
            _form.value = form.copy(error = "Isi ID stasiun, ID petugas, dan pH antara 0 dan 14.")
            return
        }

        _form.value = form.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val note = form.note.trim().ifEmpty { null }
                val editing = form.editing
                if (editing == null) {
                    dao.upsert(
                        MeasurementEntity(
                            stationId = stationId,
                            officerId = officerId,
                            ph = ph,
                            turbidityNtu = null,
                            tempC = null,
                            note = note,
                            measuredAt = System.currentTimeMillis(),
                        ),
                    )
                } else if (dao.queueEdit(editing.id, requireNotNull(editing.version), ph, note) == 0) {
                    _form.value = form.copy(error = "Data berubah sejak dibuka. Batal dan buka ulang data.")
                    return@launch
                }
                _form.value = MeasurementForm(stationId = stationId, officerId = officerId)
                syncNow()
            } catch (e: SQLiteException) {
                _form.value = form.copy(error = "Data tidak dapat disimpan. Coba lagi.")
            }
        }
    }
}
