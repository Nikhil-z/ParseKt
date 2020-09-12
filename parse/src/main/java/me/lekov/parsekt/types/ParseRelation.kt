package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class ParseRelation(
    @Required
    private val __type: String = "Relation",
    var className: String? = null
)