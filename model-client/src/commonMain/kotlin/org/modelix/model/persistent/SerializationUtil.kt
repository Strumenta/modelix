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

expect object SerializationUtil {
    fun intToHex(value: Int): String
    fun longToHex(value: Long): String
    fun longFromHex(hex: String?): Long
    fun intFromHex(hex: String?): Int
    fun escape(value: String?): String
    fun unescape(value: String?): String?
    fun nullAsEmptyString(str: String?): String
    fun emptyStringAsNull(str: String?): String?
}
