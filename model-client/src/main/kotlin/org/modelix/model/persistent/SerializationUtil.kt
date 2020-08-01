package org.modelix.model.persistent

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SerializationUtil {
    fun escape(value: String?): String {
        if (value == null) {
            return "%00"
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    fun unescape(value: String?): String? {
        if (value == null) {
            return null
        }
        if (("%00" == value)) {
            return null
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }

    fun longToHex(value: Long): String {
        return java.lang.Long.toHexString(value)
    }

    fun longFromHex(hex: String?): Long {
        return java.lang.Long.parseUnsignedLong(hex, 16)
    }

    fun intToHex(value: Int): String {
        return Integer.toHexString(value)
    }

    fun intFromHex(hex: String?): Int {
        return Integer.parseUnsignedInt(hex, 16)
    }

    fun nullAsEmptyString(str: String?): String {
        if (str == null) {
            return ""
        }
        if (str.length == 0) {
            throw RuntimeException("Empty string not allowed")
        }
        return str
    }

    fun emptyStringAsNull(str: String?): String? {
        return (if ((str == null || str.length == 0)) null else str)
    }
}