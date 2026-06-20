package com.dunihuliapps.myglidingassistnat.data.repositories.flights

import android.graphics.Bitmap
import android.util.Log
import com.dunihuliapps.myglidingassistnat.domain.datasources.flights.FlightsLocalDatasource
import data.model.Flight
import kotlinx.coroutines.flow.Flow
import java.io.FileOutputStream
import javax.inject.Inject

interface FlightsRepository{
    suspend fun getAllFlights(): Flow<List<Flight>>

    suspend fun findById(tripId: Long): Flight?

    suspend fun insertFlight(flight: Flight): Long
    suspend fun deleteFlight(flight: Flight) : Int
    suspend fun deleteFlights(ids: List<Long>) : Int
    suspend fun update(flight: Flight) : Int
    suspend fun updateFlightName(id: Long, name: String) : Int

    suspend fun updateFlightImage(image: Bitmap, id: Long, imageFileName: String) : Int

}

class FlightsRepositoryImpl @Inject constructor(private val flightsLocalDatasource: FlightsLocalDatasource): FlightsRepository {
    override suspend fun getAllFlights(): Flow<List<Flight>> {
        return flightsLocalDatasource.getAllFlights()
    }

    override suspend fun findById(tripId: Long): Flight? {
        return flightsLocalDatasource.findById(tripId)
    }

    override suspend fun insertFlight(flight: Flight): Long {
        return flightsLocalDatasource.insertFlight(flight)
    }

    override suspend fun deleteFlight(flight: Flight): Int {
        return flightsLocalDatasource.deleteFlight(flight)
    }

    override suspend fun deleteFlights(ids: List<Long>): Int {
        return flightsLocalDatasource.deleteFlights(ids)
    }

    override suspend fun update(flight: Flight): Int {
        return flightsLocalDatasource.update(flight)
    }

    override suspend fun updateFlightName(id: Long, name: String): Int {
        return flightsLocalDatasource.updateFlightName(id, name)
    }

    override suspend fun updateFlightImage(image: Bitmap, id: Long, imageFileName: String): Int {
        try {
            FileOutputStream(imageFileName).use {
                val bm: Bitmap = Bitmap.createScaledBitmap(
                    image, 500,
                    500, false
                )
                bm.compress(Bitmap.CompressFormat.JPEG, 50, it)
                bm.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                "com.bartovapps.gpstriprec.core.map_helper.com.bartovapps.gpstriprec.domain.map_helper.MapHelper",
                "There was an exception: " + e.message
            )
        }
        return flightsLocalDatasource.updateFlightImage(id, imageFileName)
    }
}