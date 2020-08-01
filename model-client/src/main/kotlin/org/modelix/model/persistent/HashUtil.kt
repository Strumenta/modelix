package org.modelix.model.persistent

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object HashUtil {
    private val HASH_PATTERN: Pattern = Pattern.compile("[a-zA-Z0-9\\-_]{43}")
    private val UTF8: Charset = StandardCharsets.UTF_8
    fun sha256(input: ByteArray?): String {
        try {
            val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
            digest.update(input)
            val sha256Bytes: ByteArray = digest.digest()
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256Bytes)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        }
    }

    fun sha256(input: String?): String {
        return sha256(input!!.toByteArray(UTF8))
    }

    fun isSha256(value: String?): Boolean {
        if (value == null) {
            return false
        }
        if (value.length != 43) {
            return false
        }
        return HASH_PATTERN.matcher(value).matches()
    }

    fun extractSha256(input: String?): Iterable<String> {
        return object : Iterable<String?> {
            public override fun iterator(): Iterator<String> {
                return object : MutableIterator<String?> {
                    private val matcher: Matcher = HASH_PATTERN.matcher(input)
                    private var hasNext: Boolean = false
                    private var hasNextInitialized: Boolean = false
                    fun ensureInitialized() {
                        if (!(hasNextInitialized)) {
                            hasNext = matcher.find()
                            hasNextInitialized = true
                        }
                    }

                    public override fun hasNext(): Boolean {
                        ensureInitialized()
                        return hasNext
                    }

                    public override fun next(): String {
                        ensureInitialized()
                        hasNextInitialized = false
                        return matcher.group()
                    }
                }
            }
        }
    }

    fun base64encode(input: String): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.toByteArray(UTF8))
    }

    fun base64decode(input: String): String {
        return String(Base64.getUrlDecoder().decode(input.toByteArray(UTF8)), UTF8)
    }
}