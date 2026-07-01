package com.sitewatch.app.monitor

import java.security.MessageDigest

object HashUtil {
    /** SHA-256 hex digest of the given content. */
    fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
