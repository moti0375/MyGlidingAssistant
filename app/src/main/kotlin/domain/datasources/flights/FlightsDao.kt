package com.dunihuliapps.myglidingassistnat.domain.datasources.flights

import androidx.room.Dao

import androidx.room.*
import data.model.Flight
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Query("SELECT * FROM flights ORDER BY id DESC")
    fun getAllFlights(): Flow<List<Flight>>

    @Query("SELECT * FROM flights WHERE id = :tripId LIMIT 1")
    suspend fun findById(tripId: Long): Flight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flight: Flight): Long

    @Delete
    suspend fun delete(flight: Flight) : Int

    @Query("DELETE FROM flights WHERE id IN (:ids)")
    suspend fun deleteFlights(ids: List<Long>) : Int

    @Update
    suspend fun update(flight: Flight): Int

    @Query("UPDATE flights SET name = :name WHERE id = :id")
    suspend fun updateFlightName(id: Long, name: String) : Int

    @Query("UPDATE flights SET imageFileName = :imageFileName WHERE id = :id")
    suspend fun updateFlightImage(id: Long, imageFileName: String) : Int

}