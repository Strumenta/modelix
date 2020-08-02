/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */

package org.modelix.model.persistent

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SerializationUtil {
    @JvmStatic
    fun escape(value: String?): String {
        return if (value == null) {
            "%00"
        } else URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun unescape(value: String?): String? {
        if (value == null) {
            return null
        }
        return if ("%00" == value) {
            null
        } else URLDecoder.decode(value, StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun longToHex(value: Long): String {
        return java.lang.Long.toHexString(value)
    }

    @JvmStatic
    fun longFromHex(hex: String?): Long {
        return java.lang.Long.parseUnsignedLong(hex, 16)
    }

    @JvmStatic
    fun intToHex(value: Int): String {
        return Integer.toHexString(value)
    }

    @JvmStatic
    fun intFromHex(hex: String?): Int {
        return Integer.parseUnsignedInt(hex, 16)
    }

    @JvmStatic
    fun nullAsEmptyString(str: String?): String {
        if (str == null) {
            return ""
        }
        if (str.length == 0) {
            throw RuntimeException("Empty string not allowed")
        }
        return str
    }

    @JvmStatic
    fun emptyStringAsNull(str: String?): String? {
        return if (str == null || str.length == 0) null else str
    }
}
