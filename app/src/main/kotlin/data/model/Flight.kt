package data.model

import kotlinx.serialization.Serializable


@Serializable
data class Flight @JvmOverloads constructor(
    val id: Long = 0,
    val date: String? = null,
    val distance: Float = 0f,
    val kml: String? = null,
    val duration: Long = 0,
    val moveTime: Long = 0,
    val stopTime: Long = 0,
    val averageSpeed: Double = 0.0,
    val moveAverageSpeed: Double = 0.0,
    val startAddress: String? = null,
    val stopAddress: String? = null,
    val maxSpeed: Double = 0.0,
    val maxAlt: Double = 0.0,
    val tripName: String? = null,
    val imageFileName: String? = null)
