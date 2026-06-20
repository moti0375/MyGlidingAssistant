package com.dunihuliapps.myglidingassistnat.domain.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dunihuliapps.myglidingassistnat.domain.datasources.flights.FlightDao
import data.model.Flight

@Database(entities = [Flight::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
}