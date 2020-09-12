package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class ParseGeoPoint(
    @Required
    private val __type: String = "GeoPoint",
    val latitude: Double,
    val longitude: Double
)