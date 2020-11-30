package me.lekov.parsekt.api

import kotlinx.serialization.Serializable
import me.lekov.parsekt.types.ParsePointer

@Serializable
internal open class Operation(val __op: String)

@Serializable
internal data class AddOperation<T>(val objects: List<T>) : Operation("Add")

@Serializable
internal data class AddUniqueOperation<T>(val objects: List<T>) : Operation("AddUnique")

@Serializable
internal data class AddRelationOperation(val objects: List<ParsePointer>) : Operation("AddRelation")

@Serializable
internal class DeleteOperation<T> : Operation("Delete")

@Serializable
internal data class IncrementOperation(val amount: Int) : Operation("Increment")

@Serializable
internal data class RemoveOperation<T>(val objects: List<T>) : Operation("Remove")

@Serializable
internal data class RemoveRelationOperation(val objects: List<ParsePointer>) :
    Operation("RemoveRelation")