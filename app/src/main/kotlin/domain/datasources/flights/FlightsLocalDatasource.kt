package com.dunihuliapps.myglidingassistnat.domain.datasources.flights

import data.model.Flight
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface FlightsLocalDatasource {
    suspend fun getAllFlights(): Flow<List<Flight>>

    suspend fun findById(tripId: Long): Flight?

    suspend fun insertFlight(flight: Flight): Long
    suspend fun deleteFlight(flight: Flight) : Int
    suspend fun deleteFlights(ids: List<Long>) : Int
    suspend fun update(flight: Flight) : Int
    suspend fun updateFlightName(id: Long, name: String) : Int

    suspend fun updateFlightImage(id: Long, imageFileName: String) : Int
}


class FlightsLocalDatasourceImpl @Inject constructor(private val flightDao: FlightDao) : FlightsLocalDatasource {
    override suspend fun getAllFlights(): Flow<List<Flight>> {
        return flightDao.getAllFlights()
    }

    override suspend fun findById(tripId: Long): Flight? {
        return flightDao.findById(tripId)
    }

    override suspend fun insertFlight(flight: Flight): Long {
        return flightDao.insert(flight)
    }

    override suspend fun deleteFlight(flight: Flight) : Int {
        return flightDao.delete(flight)
    }

    override suspend fun deleteFlights(ids: List<Long>) : Int {
        return flightDao.deleteFlights(ids)
    }

    override suspend fun update(flight: Flight) : Int {
        return flightDao.update(flight)
    }

    override suspend fun updateFlightName(id: Long, name: String) : Int {
        return flightDao.updateFlightName(id, name)
    }

    override suspend fun updateFlightImage(id: Long, imageFileName: String): Int {
        return flightDao.updateFlightImage(id, imageFileName)
    }

}