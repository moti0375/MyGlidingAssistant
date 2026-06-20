package com.dunihuliapps.myglidingassistnat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gliders")
data class Glider(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val model: String,
    val callsign: String,
    val seats: Int,
    val ratio: Int,
    val gliderImage: String? = null
)