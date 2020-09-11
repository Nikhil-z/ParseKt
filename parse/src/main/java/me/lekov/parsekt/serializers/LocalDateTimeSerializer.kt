package me.lekov.parsekt.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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