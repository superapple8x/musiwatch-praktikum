// TAMBAHAN: model JSON dan klien API sinkronisasi (kontrak sama dengan api/app/sync.py).
package id.unsri.musiwatch.sync

import id.unsri.musiwatch.data.MeasurementEntity
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class Change(
    val id: String,
    @SerialName("base_version") val baseVersion: Int?,
    @SerialName("station_id") val stationId: String,
    @SerialName("officer_id") val officerId: String,
    val ph: Double?, @SerialName("turbidity_ntu") val turbidityNtu: Double?,
    @SerialName("temp_c") val tempC: Double?, val note: String?,
    @SerialName("measured_at") val measuredAt: String,
    val deleted: Boolean,
)
@Serializable data class PushBatch(val changes: List<Change>)
@Serializable data class PushResult(val id: String, val status: String, val version: Int? = null,
                                    @SerialName("server_version") val serverVersion: Int? = null)
@Serializable data class PushResponse(val results: List<PushResult>)
@Serializable
data class ServerChange(
    val id: String, @SerialName("station_id") val stationId: String,
    @SerialName("officer_id") val officerId: String,
    val ph: Double?, @SerialName("turbidity_ntu") val turbidityNtu: Double?,
    @SerialName("temp_c") val tempC: Double?, val note: String?,
    @SerialName("measured_at") val measuredAt: String,
    val version: Int, val deleted: Boolean, @SerialName("change_seq") val changeSeq: Long,
) {
    fun toEntity() = MeasurementEntity(id, stationId, officerId, ph, turbidityNtu, tempC, note,
        OffsetDateTime.parse(measuredAt).toInstant().toEpochMilli(), version, deleted, "SYNCED")
}
@Serializable data class PullResponse(val changes: List<ServerChange>,
                                      @SerialName("next_cursor") val nextCursor: Long,
                                      @SerialName("has_more") val hasMore: Boolean)

fun MeasurementEntity.toChange() = Change(id, version, stationId, officerId, ph, turbidityNtu, tempC, note,
    Instant.ofEpochMilli(measuredAt).toString(), deleted)

class SyncApi(private val client: OkHttpClient, private val baseUrl: String) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    fun push(batch: PushBatch): PushResponse {
        val req = Request.Builder().url("$baseUrl/v1/sync/push")
            .post(json.encodeToString(batch).toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("push gagal: ${r.code}")
            return json.decodeFromString(r.body!!.string())
        }
    }

    fun pull(since: Long): PullResponse {
        client.newCall(Request.Builder().url("$baseUrl/v1/sync/pull?since=$since").build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("pull gagal: ${r.code}")
            return json.decodeFromString(r.body!!.string())
        }
    }
}
