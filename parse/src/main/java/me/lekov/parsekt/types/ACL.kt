package me.lekov.parsekt.types

import kotlinx.serialization.Serializable


enum class Access { read, write }

inline class Role(private val roleName: String) {
    fun name() = "role:$roleName"
}

@Serializable
class ACL {

    private var acl: MutableMap<String, MutableMap<Access, Boolean>>? = null

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
            acl?.get(key)?.put(access, value)
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

    companion object {
        val publicScope = "*"
    }
}