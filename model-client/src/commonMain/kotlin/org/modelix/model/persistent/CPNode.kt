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

import org.modelix.model.util.Function
import org.modelix.model.util.LogManager
import org.modelix.model.util.pmap.COWArrays
import kotlin.jvm.JvmStatic

class CPNode protected constructor(id1: Long, val concept: String?, parentId1: Long, roleInParent1: String?, private val childrenIds: LongArray, val propertyRoles: Array<String>, val propertyValues: Array<String>, val referenceRoles: Array<String>, val referenceTargets: Array<CPElementRef>) : CPElement(id1, parentId1, roleInParent1) {
    @ExperimentalStdlibApi
    override fun serialize(): String? {
        val sb = StringBuilder()
        sb.append(SerializationUtil.longToHex(id))
        sb.append("/")
        sb.append(SerializationUtil.escape(concept))
        sb.append("/")
        sb.append(SerializationUtil.longToHex(parentId))
        sb.append("/")
        sb.append(SerializationUtil.escape(roleInParent))
        sb.append("/")
        sb.append(childrenIds.map { obj: Long -> SerializationUtil.longToHex(obj) }.reduceRightOrNull { a: String, b: String -> "$a, $b" })
        sb.append("/")
        var first = true
        run {
            val role_it = propertyRoles.iterator()
            val value_it = propertyValues.iterator()
            var role_var: String?
            var value_var: String?
            while (role_it.hasNext() && value_it.hasNext()) {
                role_var = role_it.next()
                value_var = value_it.next()
                if (first) {
                    first = false
                } else {
                    sb.append(",")
                }
                sb.append(SerializationUtil.escape(role_var)).append("=").append(SerializationUtil.escape(value_var))
            }
        }
        sb.append("/")
        first = true
        run {
            val role_it = referenceRoles.iterator()
            val value_it = referenceTargets.iterator()
            var role_var: String?
            var value_var: CPElementRef
            while (role_it.hasNext() && value_it.hasNext()) {
                role_var = role_it.next()
                value_var = value_it.next()
                if (first) {
                    first = false
                } else {
                    sb.append(",")
                }
                sb.append(SerializationUtil.escape(role_var)).append("=").append(SerializationUtil.escape(value_var.toString()))
            }
        }
        return sb.toString()
    }

    fun getChildrenIds(): Iterable<Long> {
        return Iterable { childrenIds.iterator() }
    }

    val childrenIdArray: LongArray
        get() = COWArrays.copy(childrenIds)

    val childrenSize: Int
        get() = childrenIds.size

    fun getChildId(index: Int): Long {
        return childrenIds[index]
    }

    fun getPropertyValue(role: String?): String? {
        val index = propertyRoles.toList().binarySearch(role)
        return if (index < 0) null else propertyValues[index]
    }

    fun getReferenceTarget(role: String?): CPElementRef? {
        val index = referenceRoles.toList().binarySearch(role)
        return if (index < 0) null else referenceTargets[index]
    }

    fun withPropertyValue(role: String, value: String?): CPNode {
        var index = propertyRoles.toList().binarySearch(role)
        return if (value == null) {
            if (index < 0) {
                this
            } else {
                create(
                    id, concept, parentId, roleInParent, childrenIds, COWArrays.removeAt(propertyRoles, index) as Array<String?>, COWArrays.removeAt(propertyValues, index) as Array<String?>,
                    referenceRoles as Array<String?>, referenceTargets as Array<CPElementRef?>
                )
            }
        } else {
            if (index < 0) {
                index = -(index + 1)
                create(
                    id, concept, parentId, roleInParent, childrenIds,
                    COWArrays.insert(propertyRoles, index, role) as Array<String?>,
                    COWArrays.insert(propertyValues, index, value) as Array<String?>,
                    referenceRoles as Array<String?>, referenceTargets as Array<CPElementRef?>
                )
            } else {
                create(
                    id, concept, parentId, roleInParent, childrenIds,
                    propertyRoles as Array<String?>,
                    COWArrays.set(propertyValues, index, value) as Array<String?>,
                    referenceRoles as Array<String?>, referenceTargets as Array<CPElementRef?>
                )
            }
        }
    }

    fun withReferenceTarget(role: String, target: CPElementRef?): CPNode {
        var index = referenceRoles.toList().binarySearch(role)
        return if (target == null) {
            if (index < 0) {
                this
            } else {
                create(
                    id, concept, parentId, roleInParent, childrenIds,
                    propertyRoles as Array<String?>, propertyValues as Array<String?>,
                    COWArrays.removeAt(referenceRoles, index) as Array<String?>, COWArrays.removeAt(referenceTargets, index) as Array<CPElementRef?>
                )
            }
        } else {
            if (index < 0) {
                index = -(index + 1)
                create(
                    id, concept, parentId, roleInParent, childrenIds,
                    propertyRoles as Array<String?>, propertyValues as Array<String?>,
                    COWArrays.insert(referenceRoles, index, role) as Array<String?>,
                    COWArrays.insert(referenceTargets, index, target) as Array<CPElementRef?>
                )
            } else {
                create(
                    id, concept, parentId, roleInParent, childrenIds,
                    propertyRoles as Array<String?>, propertyValues as Array<String?>,
                    referenceRoles as Array<String?>, COWArrays.set(referenceTargets, index, target) as Array<CPElementRef?>
                )
            }
        }
    }

    companion object {
        private val LOG = LogManager.getLogger(CPNode::class)
        private val EMPTY_LONG_ARRAY = LongArray(0)
        val DESERIALIZER = Function { s: String -> deserialize(s) }
        @JvmStatic
        fun create(id: Long, concept: String?, parentId: Long, roleInParent: String?, childrenIds: LongArray, propertyRoles: Array<String?>, propertyValues: Array<String?>, referenceRoles: Array<String?>, referenceTargets: Array<CPElementRef?>): CPNode {
            checkForDuplicates(childrenIds)
            require(propertyRoles.size == propertyValues.size) { propertyRoles.size.toString() + " != " + propertyValues.size }
            require(referenceRoles.size == referenceTargets.size) { referenceRoles.size.toString() + " != " + referenceTargets.size }
            return CPNode(
                id, concept, parentId, roleInParent, childrenIds,
                propertyRoles as Array<String>, propertyValues as Array<String>,
                referenceRoles as Array<String>, referenceTargets as Array<CPElementRef>
            )
        }

        private fun checkForDuplicates(values: LongArray) {
            val copy = LongArray(values.size)
            values.copyInto(copy)
            copy.sort()
            for (i in 1 until copy.size) {
                if (copy[i - 1] == copy[i]) {
                    throw RuntimeException("Duplicate value: " + copy[i])
                }
            }
        }

        @JvmStatic
        fun deserialize(input: String): CPNode {
            return try {
                val parts = input.split("/").dropLastWhile { it.isEmpty() }.toTypedArray()
                val properties = parts[5].split(",").toTypedArray()
                    .filter { cs: String? -> !cs.isNullOrEmpty() }
                    .map { it: String -> it.split("=").dropLastWhile { it.isEmpty() }.toTypedArray() }
                    .toList()
                val references = parts[6].split(",").toTypedArray()
                    .filter { cs: String? -> !cs.isNullOrEmpty() }
                    .map { it: String -> it.split("=").dropLastWhile { it.isEmpty() }.toTypedArray() }
                    .toList()
                CPNode(
                    SerializationUtil.longFromHex(parts[0]),
                    SerializationUtil.unescape(parts[1]),
                    SerializationUtil.longFromHex(parts[2]),
                    SerializationUtil.unescape(parts[3]),
                    parts[4].split(",").toTypedArray()
                        .filter { cs: String? -> !cs.isNullOrEmpty() }
                        .map { obj: String -> SerializationUtil.longFromHex(obj) }
                        .toTypedArray().toLongArray(),
                    properties.map { it: Array<String> -> SerializationUtil.unescape(it[0])!! }.toTypedArray(),
                    properties.map { it: Array<String> -> SerializationUtil.unescape(it[1])!! }.toTypedArray(),
                    references.map { it: Array<String> -> SerializationUtil.unescape(it[0])!! }.toTypedArray(),
                    references.map { it: Array<String> -> CPElementRef.fromString(SerializationUtil.unescape(it[1])!!) }
                        .toTypedArray()
                )
            } catch (ex: Exception) {
                throw RuntimeException("Failed to deserialize $input", ex)
            }
        }
    }
}
