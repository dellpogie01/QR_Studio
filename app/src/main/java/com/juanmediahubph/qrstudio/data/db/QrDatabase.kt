package com.juanmediahubph.qrstudio.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [QrEntity::class],
    version = QrDatabase.VERSION,
    exportSchema = true,
)
abstract class QrDatabase : RoomDatabase() {
    abstract fun qrDao(): QrDao

    companion object {
        const val VERSION = 1
        const val NAME = "qr_studio.db"
    }
}

/**
 * Migrations are registered in order. Version 1 is the initial schema; every future
 * version bump must add a Migration(previous, next) here instead of allowing destructive
 * upgrades. The exported JSON schemas live in app/schemas and are verified by Room.
 */
object QrMigrations {
    val ALL: Array<Migration> = emptyArray()
}
