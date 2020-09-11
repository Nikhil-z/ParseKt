package me.lekov.parsekt.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


enum class Access { read, write }

inline class Role(private val roleName: String) {
    fun name() = "role:$roleName"
}

class ACL {

    private var acl: MutableMap<String, MutableMap<String, Boolean>>? = null

    var publicRead: Boolean
        get() = get(publicScope, Access.read)
        set(value) = set(publicScope, Access.read, value)

    var publicWrite: Boolean
        get() = get(publicScope, Access.write)
        set(value) = set(publicScope, Access.write, value)

    fun getReadAccess(userId: String): Boolean {
        return get(userId, Access.read)
    }

    fun getWriteAccess(userId: String): Boolean {
        return get(userId, Access.write)
    }

    fun getReadAccess(role: Role): Boolean {
        return get(role.name(), Access.read)
    }

    fun getWriteAccess(role: Role): Boolean {
        return get(role.name(), Access.write)
    }

    fun setReadAccess(userId: String, value: Boolean) {
        return set(userId, Access.read, value)
    }

    fun setWriteAccess(userId: String, value: Boolean) {
        return set(userId, Access.write, value)
    }

    fun get(key: String, access: Access): Boolean {
        return acl?.get(key)?.get(access) ?: false
    }

    fun set(key: String, access: Access, value: Boolean) {
        // initialized the backing dictionary if needed
        if (acl == null && value) { // do not create if value is false (no-op)
            acl = mutableMapOf()
        }

        // initialize the scope dictionary
        if (acl?.get(key) == null && value) {  // do not create if value is false (no-op)
            acl?.put(key, mutableMapOf())
        }

        if (value) {
            acl?.get(key)?.put(access.name, value)
        } else {
            acl?.get(key)?.remove(access)
            if (acl?.get(key)?.isEmpty() == true) {
                acl?.remove(key)
            }
            if (acl?.isEmpty() == true) {
                acl = null
            }
        }

    }

    @Serializer(forClass = ACL::class)
    object ACLSerializer : KSerializer<ACL> {
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

            return ACL().apply {
                acl = mutable
            }
        }

        override fun serialize(encoder: Encoder, value: ACL) {
            encoder.encodeNullableSerializableValue(
                MapSerializer(
                    String.serializer(),
                    MapSerializer(String.serializer(), Boolean.serializer())
                ), value.acl
            )
        }
    }

    companion object {
        const val publicScope = "*"
    }
}