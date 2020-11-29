package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/**
 * Parse relation
 *
 * @property __type
 * @property className
 * @constructor Create empty Parse relation
 */
@Serializable
internal data class ParseRelation(
    @Required
    private val __type: String = "Relation",
    var className: String? = null
)