package me.lekov.parsekt.types

import kotlinx.serialization.Serializable
import me.lekov.parsekt.api.ACLSerializer

/**
 * Access
 *
 * @constructor Create empty Access
 */
enum class Access { read, write }

/**
 * Role
 *
 * @property roleName
 * @constructor Create empty Role
 */
class Role(private val roleName: String) {
    fun name() = "role:$roleName"
}

/**
 * A c l
 *
 * @property acl
 * @constructor Create empty A c l
 */
@Serializable(with = ACLSerializer::class)
class ACL(internal var acl: MutableMap<String, MutableMap<String, Boolean>>? = null) {

    /**
     * Public read
     */
    var publicRead: Boolean
        get() = get(publicScope, Access.read)
        set(value) = set(publicScope, Access.read, value)

    /**
     * Public write
     */
    var publicWrite: Boolean
        get() = get(publicScope, Access.write)
        set(value) = set(publicScope, Access.write, value)

    /**
     * Get read access
     *
     * @param userId
     * @return
     */
    fun getReadAccess(userId: String): Boolean {
        return get(userId, Access.read)
    }

    /**
     * Get write access
     *
     * @param userId
     * @return
     */
    fun getWriteAccess(userId: String): Boolean {
        return get(userId, Access.write)
    }

    /**
     * Get read access
     *
     * @param role
     * @return
     */
    fun getReadAccess(role: Role): Boolean {
        return get(role.name(), Access.read)
    }

    /**
     * Get write access
     *
     * @param role
     * @return
     */
    fun getWriteAccess(role: Role): Boolean {
        return get(role.name(), Access.write)
    }

    /**
     * Set read access
     *
     * @param userId
     * @param value
     */
    fun setReadAccess(userId: String, value: Boolean) {
        return set(userId, Access.read, value)
    }

    /**
     * Set write access
     *
     * @param userId
     * @param value
     */
    fun setWriteAccess(userId: String, value: Boolean) {
        return set(userId, Access.write, value)
    }

    /**
     * Get
     *
     * @param key
     * @param access
     * @return
     */
    fun get(key: String, access: Access): Boolean {
        return acl?.get(key)?.get(access.name) ?: false
    }

    /**
     * Set
     *
     * @param key
     * @param access
     * @param value
     */
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
            acl?.get(key)?.remove(access.name)
            if (acl?.get(key)?.isEmpty() == true) {
                acl?.remove(key)
            }
            if (acl?.isEmpty() == true) {
                acl = null
            }
        }

    }


    companion object {
        const val publicScope = "*"
    }
}