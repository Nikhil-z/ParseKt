package me.lekov.parsekt.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import me.lekov.parsekt.types.ACL
import me.lekov.parsekt.types.QueryConstraint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val string = synchronized(format) { format.format(value) }
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val string = decoder.decodeString()
        return synchronized(format) { LocalDateTime.parse(string, format) }
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("yourSerializerUniqueName", PrimitiveKind.STRING)
}

object LocalDateTimeQuerySerializer : KSerializer<LocalDateTime> {
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeSerializableValue(
            MapSerializer(String.serializer(), String.serializer()),
            mapOf(
                "__type" to "Date",
                "iso" to synchronized(format) { format.format(value) })
        )
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val string = decoder.decodeString()
        return synchronized(format) { LocalDateTime.parse(string, format) }
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("yourSerializerUniqueName", PrimitiveKind.STRING)
}

object ACLSerializer : KSerializer<ACL> {
    @ExperimentalSerializationApi
    override val descriptor: SerialDescriptor =
        mapSerialDescriptor<String, Map<String, Boolean>>()

    override fun deserialize(decoder: Decoder): ACL {
        val res = decoder.decodeSerializableValue(
            MapSerializer(
                String.serializer(),
                MapSerializer(String.serializer(), Boolean.serializer())
            )
        )
        val mutable = mutableMapOf<String, MutableMap<String, Boolean>>()
        for (entry in res.entries) {
            mutable[entry.key] = entry.value.toMutableMap()
        }

        return ACL(mutable)
    }

    @ExperimentalSerializationApi
    override fun serialize(encoder: Encoder, value: ACL) {
        encoder.encodeNullableSerializableValue(
            MapSerializer(
                String.serializer(),
                MapSerializer(String.serializer(), Boolean.serializer())
            ), value.acl
        )
    }
}


object QueryConstraintsSerializer :
    KSerializer<MutableList<QueryConstraint>> {

    override fun serialize(
        encoder: Encoder,
        value: MutableList<QueryConstraint>
    ) {

        val groupedIterable = value.groupBy { it.key }.asIterable()
        val elements = arrayListOf<JsonElement>()

        if (value.any { it.key == "\$or" || it.key == "\$and" }) {
            val nestedQueries = value.first { it.key == "\$or" || it.key == "\$and" }
            val encodedNested = Json.encodeToJsonElement(
                MapSerializer(
                    String.serializer(),
                    ListSerializer(JsonElement.serializer()),
                ), mapOf(nestedQueries.key to nestedQueries.value as JsonArray)
            )

            elements.add(encodedNested)
        }

        if (value.any { it.key == "\$relatedTo" }) {
            val nestedQueries = value.first { it.key == "\$relatedTo" }
            val encodedNested = Json.encodeToJsonElement(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer(),
                ), mapOf(nestedQueries.key to nestedQueries.value)
            )

            elements.add(encodedNested)
        }

        val where = groupedIterable.filter { it.key != "\$or" &&  it.key != "\$and" && it.key != "\$relatedTo" }
            .fold(mutableMapOf<String, Map<String, JsonElement>>()) { acc, entry ->
                acc[entry.key] = entry.value.associate {
                    it.comparator to it.value
                }

                acc
            }

        val encodedNested = Json.encodeToJsonElement(where)
        elements.add(encodedNested)


        val result = buildJsonObject {
            elements.forEach { element ->
                element.jsonObject.keys.forEach {
                    element.jsonObject[it]?.let { data -> this.put(it, data) }
                }
            }
        }

        encoder.encodeSerializableValue(JsonElement.serializer(), result)
    }

    override val descriptor: SerialDescriptor
        get() = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): MutableList<QueryConstraint> {
        TODO("Not yet implemented")
    }
}