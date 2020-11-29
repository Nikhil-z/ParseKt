package me.lekov.parsekt.types

import kotlinx.serialization.Serializable

/**
 * Parse error
 *
 * @property code
 * @property error
 * @constructor Create empty Parse error
 */
@Serializable
data class ParseError(val code: Int, val error: String? = null) : Error() {
    companion object {
        const val OTHER_CAUSE = -1

        /**
         * Error code indicating the connection to the Parse servers failed.
         */
        const val CONNECTION_FAILED = 100

        /**
         * Error code indicating the specified object doesn't exist.
         */
        const val OBJECT_NOT_FOUND = 101

        /**
         * Error code indicating you tried to query with a datatype that doesn't support it, like exact
         * matching an array or object.
         */
        const val INVALID_QUERY = 102

        /**
         * Error code indicating a missing or invalid classname. Classnames are case-sensitive. They must
         * start with a letter, and a-zA-Z0-9_ are the only valid characters.
         */
        const val INVALID_CLASS_NAME = 103

        /**
         * Error code indicating an unspecified object id.
         */
        const val MISSING_OBJECT_ID = 104

        /**
         * Error code indicating an invalid key name. Keys are case-sensitive. They must start with a
         * letter, and a-zA-Z0-9_ are the only valid characters.
         */
        const val INVALID_KEY_NAME = 105

        /**
         * Error code indicating a malformed pointer. You should not see this unless you have been mucking
         * about changing internal Parse code.
         */
        const val INVALID_POINTER = 106

        /**
         * Error code indicating that badly formed JSON was received upstream. This either indicates you
         * have done something unusual with modifying how things encode to JSON, or the network is failing
         * badly.
         */
        const val INVALID_JSON = 107

        /**
         * Error code indicating that the feature you tried to access is only available internally for
         * testing purposes.
         */
        const val COMMAND_UNAVAILABLE = 108

        /**
         * You must call Parse.initialize before using the Parse library.
         */
        const val NOT_INITIALIZED = 109

        /**
         * Error code indicating that a field was set to an inconsistent type.
         */
        const val INCORRECT_TYPE = 111

        /**
         * Error code indicating an invalid channel name. A channel name is either an empty string (the
         * broadcast channel) or contains only a-zA-Z0-9_ characters and starts with a letter.
         */
        const val INVALID_CHANNEL_NAME = 112

        /**
         * Error code indicating that push is misconfigured.
         */
        const val PUSH_MISCONFIGURED = 115

        /**
         * Error code indicating that the object is too large.
         */
        const val OBJECT_TOO_LARGE = 116

        /**
         * Error code indicating that the operation isn't allowed for clients.
         */
        const val OPERATION_FORBIDDEN = 119

        /**
         * Error code indicating the result was not found in the cache.
         */
        const val CACHE_MISS = 120

        /**
         * Error code indicating that an invalid key was used in a nested JSONObject.
         */
        const val INVALID_NESTED_KEY = 121

        /**
         * Error code indicating that an invalid filename was used for ParseFile. A valid file name
         * contains only a-zA-Z0-9_. characters and is between 1 and 128 characters.
         */
        const val INVALID_FILE_NAME = 122

        /**
         * Error code indicating an invalid ACL was provided.
         */
        const val INVALID_ACL = 123

        /**
         * Error code indicating that the request timed out on the server. Typically this indicates that
         * the request is too expensive to run.
         */
        const val TIMEOUT = 124

        /**
         * Error code indicating that the email address was invalid.
         */
        const val INVALID_EMAIL_ADDRESS = 125

        /**
         * Error code indicating that required field is missing.
         */
        const val MISSING_REQUIRED_FIELD_ERROR = 135

        /**
         * Error code indicating that a unique field was given a value that is already taken.
         */
        const val DUPLICATE_VALUE = 137

        /**
         * Error code indicating that a role's name is invalid.
         */
        const val INVALID_ROLE_NAME = 139

        /**
         * Error code indicating that an application quota was exceeded. Upgrade to resolve.
         */
        const val EXCEEDED_QUOTA = 140

        /**
         * Error code indicating that a Cloud Code script failed.
         */
        const val SCRIPT_ERROR = 141

        /**
         * Error code indicating that cloud code validation failed.
         */
        const val VALIDATION_ERROR = 142

        /**
         * Error code indicating that deleting a file failed.
         */
        const val FILE_DELETE_ERROR = 153

        /**
         * Error code indicating that the application has exceeded its request limit.
         */
        const val REQUEST_LIMIT_EXCEEDED = 155

        /**
         * Error code indicating that the provided event name is invalid.
         */
        const val INVALID_EVENT_NAME = 160

        /**
         * Error code indicating that the username is missing or empty.
         */
        const val USERNAME_MISSING = 200

        /**
         * Error code indicating that the password is missing or empty.
         */
        const val PASSWORD_MISSING = 201

        /**
         * Error code indicating that the username has already been taken.
         */
        const val USERNAME_TAKEN = 202

        /**
         * Error code indicating that the email has already been taken.
         */
        const val EMAIL_TAKEN = 203

        /**
         * Error code indicating that the email is missing, but must be specified.
         */
        const val EMAIL_MISSING = 204

        /**
         * Error code indicating that a user with the specified email was not found.
         */
        const val EMAIL_NOT_FOUND = 205

        /**
         * Error code indicating that a user object without a valid session could not be altered.
         */
        const val SESSION_MISSING = 206

        /**
         * Error code indicating that a user can only be created through signup.
         */
        const val MUST_CREATE_USER_THROUGH_SIGNUP = 207

        /**
         * Error code indicating that an an account being linked is already linked to another user.
         */
        const val ACCOUNT_ALREADY_LINKED = 208

        /**
         * Error code indicating that the current session token is invalid.
         */
        const val INVALID_SESSION_TOKEN = 209

        /**
         * Error code indicating that a user cannot be linked to an account because that account's id
         * could not be found.
         */
        const val LINKED_ID_MISSING = 250

        /**
         * Error code indicating that a user with a linked (e.g. Facebook) account has an invalid session.
         */
        const val INVALID_LINKED_SESSION = 251

        /**
         * Error code indicating that a service being linked (e.g. Facebook or Twitter) is unsupported.
         */
        const val UNSUPPORTED_SERVICE = 252

    }
}