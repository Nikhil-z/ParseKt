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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import me.lekov.parsekt.types.*
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

        val json = Json {
            serializersModule = SerializersModule {
                contextual(LocalDateTimeQuerySerializer)
            }
        }

        val groupedIterable = value.groupBy { it.key }.asIterable()

        if (value.any { it.key == "\$or" || it.key == "\$and" }) {
            val nestedQueries = value.first { it.key == "\$or" || it.key == "\$and" }
            encoder.encodeSerializableValue(
                MapSerializer(
                    String.serializer(),
                    ListSerializer(JsonElement.serializer()),
                ), mapOf(nestedQueries.key to nestedQueries.value as ArrayList<JsonElement>)
            )
        } else {
            val where = groupedIterable
                .fold(mutableMapOf<String, Map<String, JsonElement>>()) { acc, entry ->
                    acc[entry.key] = entry.value.associate {
                        it.comparator to when (it.value) {
                            is Number -> JsonPrimitive(it.value)
                            is Boolean -> JsonPrimitive(it.value)
                            is String -> JsonPrimitive(it.value)
                            is LocalDateTime -> json.parseToJsonElement(json.encodeToString(it.value))
                            is ParseClass -> json.parseToJsonElement(
                                json.encodeToString(
                                    ParsePointer(it.value)
                                )
                            )
                            is ParseUser -> json.parseToJsonElement(
                                json.encodeToString(
                                    ParsePointer(
                                        it.value
                                    )
                                )
                            )
                            else -> json.encodeToJsonElement(
                                ListSerializer(JsonElement.serializer()),
                                it.value as List<JsonElement>
                            )
                        }
                    }

                    acc
                }

            encoder.encodeSerializableValue(
                JsonElement.serializer(),
                Json.encodeToJsonElement(where)
            )
        }
    }

    override val descriptor: SerialDescriptor
        get() = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): MutableList<QueryConstraint> {
        TODO("Not yet implemented")
    }
}