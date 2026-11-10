// Sumber: Modul Praktikum P3, Langkah 4 (alur verbatim).
// TAMBAHAN: dependensi (api, syncPrefs) dibuat konkret agar bisa dikompilasi.
package id.unsri.musiwatch.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.unsri.musiwatch.data.AppDb
import id.unsri.musiwatch.net.BASE_URL
import id.unsri.musiwatch.net.buildClient
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val api = SyncApi(buildClient(withRetry = false), BASE_URL)  // retry diatur WorkManager
    private val prefs = applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = AppDb.get(applicationContext).measurementDao()
            val pending = dao.pending()                       // syncState = PENDING
            if (pending.isNotEmpty()) {
                val resp = api.push(PushBatch(pending.map { it.toChange() }))
                dao.applyPushResults(resp.results)            // created/updated/duplicate -> SYNCED; conflict -> CONFLICT
            }
            var cursor = prefs.getLong("cursor", 0L)
            do {
                val page = api.pull(cursor)
                dao.upsertFromServer(page.changes)             // jangan timpa baris yang masih PENDING
                cursor = page.nextCursor
                prefs.edit().putLong("cursor", cursor).apply()
            } while (page.hasMore)
            Result.success()
        } catch (e: IOException) {
            Result.retry()                                    // WorkManager mengatur backoff
        }
    }
}
