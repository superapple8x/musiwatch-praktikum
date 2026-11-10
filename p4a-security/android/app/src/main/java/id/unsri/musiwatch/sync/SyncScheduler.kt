// Sumber: Modul Praktikum P3, Langkah 4 (penjadwalan WorkManager sesuai deskripsi modul).
package id.unsri.musiwatch.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private val netConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

/** Panggil sekali saat aplikasi mulai. */
fun schedulePeriodicSync(ctx: Context) {
    val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(netConstraint)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, req)
}

/** Panggil setiap kali pengguna menyimpan data. */
fun requestSyncNow(ctx: Context) {
    val req = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(netConstraint)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(ctx).enqueueUniqueWork("sync-now", ExistingWorkPolicy.REPLACE, req)
}
