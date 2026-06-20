package com.dunihuliapps.myglidingassistnat.domain.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dunihuliapps.myglidingassistnat.domain.datasources.flights.FlightDao
import data.model.Flight

@Database(entities = [Flight::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao


    companion object {
        // Define migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Use standard SQLite syntax to add columns
//                db.execSQL("ALTER TABLE trips ADD COLUMN avgAltitude REAL NOT NULL DEFAULT 0.0")
//                db.execSQL("ALTER TABLE trips ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}