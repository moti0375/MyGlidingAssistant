package com.dunihuliapps.myglidingassistnat.data.repositories.gliders

import com.dunihuliapps.myglidingassistnat.data.model.Glider
import com.dunihuliapps.myglidingassistnat.domain.datasources.gliders.GlidersLocalDatasource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GlidersRepository{
    fun getAllGliders(): Flow<List<Glider>>
    suspend fun findById(gliderId: Long): Glider?
    suspend fun insertGlider(callsign: String = "", type: String = "", ratio: Int, seats: Int, gliderImage: String? = null): Long
    suspend fun deleteGlider(glider: Glider) : Int
    suspend fun update(id: Long, callsign: String = "", type: String = "", ratio: Int, seats: Int, gliderImage: String? = null) : Int
    suspend fun updateGliderImage(id: Long, imageFileName: String) : Int
}

class GlidersRepositoryImpl @Inject constructor(private val glidersLocalDatasource: GlidersLocalDatasource) : GlidersRepository{

    override fun getAllGliders(): Flow<List<Glider>> {
        return glidersLocalDatasource.getAllGliders()
    }

    override suspend fun findById(gliderId: Long): Glider? {
        return glidersLocalDatasource.findById(gliderId)
    }

    override suspend fun insertGlider(callsign: String, type: String, ratio: Int, seats: Int, gliderImage: String? ): Long  {
        return glidersLocalDatasource.insertGlider(Glider(callsign = callsign, type = type, ratio = ratio, seats = seats, gliderImage = gliderImage))
    }

    override suspend fun deleteGlider(glider: Glider) : Int {
        return glidersLocalDatasource.deleteGlider(glider)
    }

    override suspend fun update(id: Long, callsign: String, type: String, ratio: Int, seats: Int, gliderImage: String?) : Int {
        return glidersLocalDatasource.update(Glider(id, type = type,  callsign = callsign, seats = seats, ratio = ratio, gliderImage = gliderImage))
    }

    override suspend fun updateGliderImage(id: Long, imageFileName: String) : Int {
        return glidersLocalDatasource.updateGliderImage(id, imageFileName)
    }
}

