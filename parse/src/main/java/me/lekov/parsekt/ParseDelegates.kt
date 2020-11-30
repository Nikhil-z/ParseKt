package me.lekov.parsekt

import me.lekov.parsekt.types.ParseObject
import me.lekov.parsekt.types.ParseRelation
import kotlin.reflect.KProperty

class ParseRelationDelegate<T : ParseObject<T>> {

    operator fun getValue(parseObject: T, property: KProperty<*>): ParseRelation {
        return ParseRelation(className = parseObject.className, `object` = parseObject)
    }
}