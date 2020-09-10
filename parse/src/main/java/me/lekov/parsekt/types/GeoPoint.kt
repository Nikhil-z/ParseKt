package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
class GeoPoint(
    @Required
    private val __type: String = "GeoPoint",
    val latitude: Double,
    val longitude: Double
)