package me.lekov.parsekt

import kotlinx.serialization.Serializable
import me.lekov.parsekt.types.ClassNameDelegate
import me.lekov.parsekt.types.IDefineable
import me.lekov.parsekt.types.ParseObject

@Serializable
class GameScore: ParseObject() {
    var score: Int? = null
    var playerName: String? = null
    var cheatMode: Boolean? = null

    override fun toString(): String {
        return "GameScore(objectId=$objectId, score=$score, playerName=$playerName, cheatMode=$cheatMode, createdAt=$createdAt, updatedAt=$updatedAt)"
    }

    companion object : IDefineable {
        override val className: String by ClassNameDelegate()
    }

}