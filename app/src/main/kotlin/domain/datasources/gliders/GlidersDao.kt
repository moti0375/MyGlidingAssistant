package com.dunihuliapps.myglidingassistnat.domain.datasources.gliders

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dunihuliapps.myglidingassistnat.data.model.Glider
import data.model.Flight
import kotlinx.coroutines.flow.Flow

@Dao
interface GlidersDao {
    @Query("SELECT * FROM gliders ORDER BY id DESC")
    fun getAllFlights(): Flow<List<Glider>>

    @Query("SELECT * FROM gliders WHERE id = :gliderId LIMIT 1")
    suspend fun findById(gliderId: Long): Flight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(glider: Glider): Long

    @Delete
    suspend fun delete(glider: Glider) : Int

    @Query("DELETE FROM gliders WHERE id IN (:ids)")
    suspend fun deleteFlights(ids: List<Long>) : Int

    @Update
    suspend fun update(glider: Glider): Int

    @Query("UPDATE gliders SET gliderImage = :imageFileName WHERE id = :id")
    suspend fun updateFlightImage(id: Long, imageFileName: String) : Int
}