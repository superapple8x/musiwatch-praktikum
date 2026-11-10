// TAMBAHAN (modul hanya menyebut nama fungsinya): DAO untuk alur sinkronisasi P3.
// Dibuat abstract class (bukan interface) agar @Transaction pada fungsi berisi logika didukung Room.
package id.unsri.musiwatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.unsri.musiwatch.sync.PushResult
import id.unsri.musiwatch.sync.ServerChange
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MeasurementDao {
    @Query("SELECT * FROM measurements WHERE deleted = 0 ORDER BY measuredAt DESC")
    abstract fun observeAll(): Flow<List<MeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(e: MeasurementEntity)

    @Query("SELECT * FROM measurements WHERE syncState = 'PENDING'")
    abstract suspend fun pending(): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE id = :id")
    abstract suspend fun byId(id: String): MeasurementEntity?

    @Query("UPDATE measurements SET syncState = :state, version = :version WHERE id = :id")
    abstract suspend fun setSyncState(id: String, state: String, version: Int?)

    /** created/updated/duplicate -> SYNCED (versi dari server); conflict -> CONFLICT (versi lokal dipertahankan). */
    @Transaction
    open suspend fun applyPushResults(results: List<PushResult>) {
        for (r in results) when (r.status) {
            "created", "updated", "duplicate" -> setSyncState(r.id, "SYNCED", r.version)
            "conflict" -> setSyncState(r.id, "CONFLICT", byId(r.id)?.version)
        }
    }

    /** Terapkan perubahan server, tetapi JANGAN timpa baris lokal yang masih PENDING atau CONFLICT. */
    @Transaction
    open suspend fun upsertFromServer(changes: List<ServerChange>) {
        for (c in changes) {
            val local = byId(c.id)
            if (local != null && local.syncState != "SYNCED") continue
            upsert(c.toEntity())
        }
    }
}
