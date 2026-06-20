package com.dunihuliapps.myglidingassistnat.domain.datasources.gliders

import com.dunihuliapps.myglidingassistnat.data.model.Glider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GlidersLocalDatasource {
    suspend fun getAllGliders(): Flow<List<Glider>>
    suspend fun findById(gliderId: Long): Glider?
    suspend fun insertGlider(glider: Glider): Long
    suspend fun deleteGlider(glider: Glider) : Int
    suspend fun update(glider: Glider) : Int
    suspend fun updateGliderImage(id: Long, imageFileName: String) : Int
}


class GlidersLocalDatasourceImpl @Inject constructor(private val glidersDao: GlidersDao) : GlidersLocalDatasource{
    override suspend fun getAllGliders(): Flow<List<Glider>> {
        return glidersDao.getAllFlights()
    }

    override suspend fun findById(gliderId: Long): Glider? {
        return glidersDao.findById(gliderId)
    }

    override suspend fun insertGlider(glider: Glider): Long {
        return glidersDao.insert(glider)
    }

    override suspend fun deleteGlider(glider: Glider) : Int {
        return glidersDao.delete(glider)
    }

    override suspend fun update(glider: Glider) : Int {
        return glidersDao.update(glider)
    }

    override suspend fun updateGliderImage(id: Long, imageFileName: String) : Int {
        return glidersDao.updateFlightImage(id, imageFileName)
    }
}