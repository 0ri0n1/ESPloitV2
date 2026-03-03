package com.dotagency.cactusc2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dotagency.cactusc2.data.model.Device
import com.dotagency.cactusc2.data.model.Payload

/**
 * Room database for Cactus C2.
 * Schema v1: devices + payloads tables.
 * Singleton via [getInstance] — thread-safe double-checked locking.
 */
@Database(entities = [Device::class, Payload::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun payloadDao(): PayloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cactus_c2.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
