package com.dunihuliapps.myglidingassistnat.domain.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.domain.datasources.flights.FlightDao
import com.dunihuliapps.myglidingassistnat.domain.datasources.gliders.GlidersDao
import data.model.Flight

@Database(entities = [Flight::class, Glider::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao

    abstract fun glidersDao(): GlidersDao

    companion object {
        // Define migration from version 1 to 2
// Inside AppDatabase.kt companion object
// In AppDatabase.kt
        val MIGRATION_2_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the table if it exists to ensure a clean start for this version
                db.execSQL("DROP TABLE IF EXISTS `gliders`")

                // Create the table with the EXACT columns Room is expecting
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS `gliders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `callsign` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `seats` INTEGER NOT NULL, 
                `ratio` INTEGER NOT NULL, 
                `gliderImage` TEXT
            )
        """.trimIndent()
                )
            }
        }
    }
}