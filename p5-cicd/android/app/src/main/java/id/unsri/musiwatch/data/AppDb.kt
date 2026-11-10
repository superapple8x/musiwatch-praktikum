// TAMBAHAN: database Room (modul memakai AppDb.get(context) tanpa definisi).
package id.unsri.musiwatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MeasurementEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "musiwatch.db")
                .build().also { instance = it }
        }
    }
}
