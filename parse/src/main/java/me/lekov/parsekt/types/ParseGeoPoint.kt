package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class ParseGeoPoint(
    @Required
    private val __type: String = "GeoPoint",
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        const val earthRadiusMiles = 3958.8
        const val earthRadiusKilometers = 6371.0
    }
}