package me.lekov.parsekt.types

import kotlinx.serialization.Serializable
import me.lekov.parsekt.api.*
import me.lekov.parsekt.api.AddOperation
import me.lekov.parsekt.api.AddRelationOperation
import me.lekov.parsekt.api.AddUniqueOperation
import me.lekov.parsekt.api.IncrementOperation
import me.lekov.parsekt.api.RemoveOperation

class ParseOperation<T: ParseObject<T>>(var target: T? = null) {

    @Serializable
    internal var operations: MutableMap<String, Any?> = mutableMapOf()

    fun <V> set(key: String, value: V): ParseOperation<T> {
        operations[key] = value
        return this
    }

    fun <V> forceSet(key: String, value: V): ParseOperation<T> {
        return this
    }

    fun unset(key: String): ParseOperation<T> {
        operations[key] = DeleteOperation
        return this
    }

    fun <V: Number> increment(key: String, value: V): ParseOperation<T> {
        operations[key] = IncrementOperation(value)
        return this
    }

    fun add(key: String, objects: List<T>): ParseOperation<T> {
        operations[key] = AddOperation(objects)
        return this
    }

    fun addUnique(key: String, objects: List<T>): ParseOperation<T> {
        operations[key] = AddUniqueOperation(objects)
        return this
    }

    fun addRelation(key: String, objects: List<T>): ParseOperation<T> {
        operations[key] = AddRelationOperation(objects.map { ParsePointer(it) })
        return this
    }

    fun remove(key: String, objects: List<T>): ParseOperation<T> {
        operations[key] = RemoveOperation(objects)
        return this
    }

    fun removeRelation(key: String, objects: List<T>): ParseOperation<T> {
        operations[key] = RemoveRelationOperation(objects.map { ParsePointer(it) })
        return this
    }
}