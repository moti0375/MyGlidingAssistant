package data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "flights")
data class Flight @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String? = null,
    val takeOffTime: Long? = null,
    val landingTime: Long? = null,
    val overallDistance: Float,
    val maxDistance: Float,
    val kml: String? = null,
    val duration: Long,
    val maxAlt: Double,
    val name: String? = null,
    val imageFileName: String? = null)
