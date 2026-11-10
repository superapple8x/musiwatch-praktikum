// Sumber: Modul Praktikum P3, Langkah 4 (entity verbatim + import)
package id.unsri.musiwatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val stationId: String, val officerId: String,
    val ph: Double?, val turbidityNtu: Double?, val tempC: Double?, val note: String?,
    val measuredAt: Long,
    val version: Int? = null,          // null = belum pernah sampai server
    val deleted: Boolean = false,
    val syncState: String = "PENDING"  // PENDING | SYNCED | CONFLICT
)
