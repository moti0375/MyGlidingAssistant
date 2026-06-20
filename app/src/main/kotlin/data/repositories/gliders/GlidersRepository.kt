package com.dunihuliapps.myglidingassistnat.data.repositories.gliders

import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.domain.datasources.gliders.GlidersLocalDatasource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GlidersRepository{
    suspend fun getAllGliders(): Flow<List<Glider>>
    suspend fun findById(gliderId: Long): Glider?
    suspend fun insertGlider(glider: Glider): Long
    suspend fun deleteGlider(glider: Glider) : Int
    suspend fun update(glider: Glider) : Int
    suspend fun updateGliderImage(id: Long, imageFileName: String) : Int
}

class GlidersRepositoryImpl @Inject constructor(private val glidersLocalDatasource: GlidersLocalDatasource) : GlidersRepository{

    override suspend fun getAllGliders(): Flow<List<Glider>> {
        return glidersLocalDatasource.getAllGliders()
    }

    override suspend fun findById(gliderId: Long): Glider? {
        return glidersLocalDatasource.findById(gliderId)
    }

    override suspend fun insertGlider(glider: Glider): Long {
        return glidersLocalDatasource.insertGlider(glider)
    }

    override suspend fun deleteGlider(glider: Glider) : Int {
        return glidersLocalDatasource.deleteGlider(glider)
    }

    override suspend fun update(glider: Glider) : Int {
        return glidersLocalDatasource.update(glider)
    }

    override suspend fun updateGliderImage(id: Long, imageFileName: String) : Int {
        return glidersLocalDatasource.updateGliderImage(id, imageFileName)
    }
}

