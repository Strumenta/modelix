package org.modelix.model.persistent

import org.apache.commons.lang3.StringUtils
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.modelix.model.util.pmap.COWArrays
import java.util.*
import java.util.function.*
import java.util.function.Function
import java.util.stream.Collectors

class CPNode protected constructor(id1: Long, val concept: String?, parentId1: Long, roleInParent1: String?, private val childrenIds: LongArray?, val propertyRoles: Array<String?>?, val propertyValues: Array<String?>?, val referenceRoles: Array<String?>?, val referenceTargets: Array<CPElementRef?>?) : CPElement(id1, parentId1, roleInParent1) {
    public override fun serialize(): String {
        val sb: StringBuilder = StringBuilder()
        sb.append(SerializationUtil.longToHex(id))
        sb.append("/")
        sb.append(SerializationUtil.escape(concept))
        sb.append("/")
        sb.append(SerializationUtil.longToHex(parentId))
        sb.append("/")
        sb.append(SerializationUtil.escape(roleInParent))
        sb.append("/")
        sb.append(Arrays.stream(childrenIds).mapToObj(LongFunction({ obj: Long -> SerializationUtil.longToHex() })).reduce(BinaryOperator({ a: String?, b: String? -> a + ", " + b })))
        sb.append("/")
        var first: Boolean = true
        run({
            val role_it: Iterator<String?> = Arrays.stream(propertyRoles).iterator()
            val value_it: Iterator<String?> = Arrays.stream(propertyValues).iterator()
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
        })
        sb.append("/")
        first = true
        run({
            val role_it: Iterator<String?> = Arrays.stream(referenceRoles).iterator()
            val value_it: Iterator<CPElementRef> = Arrays.stream(referenceTargets).iterator()
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
        })
        return sb.toString()
    }

    fun getChildrenIds(): Iterable<Long> {
        return Iterable({ Arrays.stream(childrenIds).iterator() })
    }

    val childrenIdArray: LongArray?
        get() {
            return COWArrays.copy(childrenIds)
        }

    val childrenSize: Int
        get() {
            return childrenIds!!.size
        }

    fun getChildId(index: Int): Long {
        return childrenIds!!.get(index)
    }

    fun getPropertyValue(role: String?): String? {
        val index: Int = Arrays.binarySearch(propertyRoles, role)
        return (if (index < 0) null else propertyValues!!.get(index))
    }

    fun getReferenceTarget(role: String?): CPElementRef? {
        val index: Int = Arrays.binarySearch(referenceRoles, role)
        return (if (index < 0) null else referenceTargets!!.get(index))
    }

    fun withPropertyValue(role: String?, value: String?): CPNode {
        var index: Int = Arrays.binarySearch(propertyRoles, role)
        if (value == null) {
            if (index < 0) {
                return this
            } else {
                return create(id, concept, parentId, roleInParent, childrenIds, COWArrays.removeAt(propertyRoles, index), COWArrays.removeAt(propertyValues, index), referenceRoles, referenceTargets)
            }
        } else {
            if (index < 0) {
                index = -(index + 1)
                return create(id, concept, parentId, roleInParent, childrenIds, COWArrays.insert(propertyRoles, index, role), COWArrays.insert(propertyValues, index, value), referenceRoles, referenceTargets)
            } else {
                return create(id, concept, parentId, roleInParent, childrenIds, propertyRoles, COWArrays.set(propertyValues, index, value), referenceRoles, referenceTargets)
            }
        }
    }

    fun withReferenceTarget(role: String?, target: CPElementRef?): CPNode {
        var index: Int = Arrays.binarySearch(referenceRoles, role)
        if (target == null) {
            if (index < 0) {
                return this
            } else {
                return create(id, concept, parentId, roleInParent, childrenIds, propertyRoles, propertyValues, COWArrays.removeAt(referenceRoles, index), COWArrays.removeAt(referenceTargets, index))
            }
        } else {
            if (index < 0) {
                index = -(index + 1)
                return create(id, concept, parentId, roleInParent, childrenIds, propertyRoles, propertyValues, COWArrays.insert(referenceRoles, index, role), COWArrays.insert(referenceTargets, index, target))
            } else {
                return create(id, concept, parentId, roleInParent, childrenIds, propertyRoles, propertyValues, referenceRoles, COWArrays.set(referenceTargets, index, target))
            }
        }
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger(CPNode::class.java)
        private val EMPTY_LONG_ARRAY: LongArray = LongArray(0)
        val DESERIALIZER: Function<String, CPNode> = Function({ s: String -> deserialize(s) })
        fun create(id: Long, concept: String?, parentId: Long, roleInParent: String?, childrenIds: LongArray?, propertyRoles: Array<String?>?, propertyValues: Array<String?>?, referenceRoles: Array<String?>?, referenceTargets: Array<CPElementRef?>?): CPNode {
            checkForDuplicates(childrenIds)
            if (propertyRoles!!.size != propertyValues!!.size) {
                throw IllegalArgumentException(propertyRoles.size.toString() + " != " + propertyValues.size)
            }
            if (referenceRoles!!.size != referenceTargets!!.size) {
                throw IllegalArgumentException(referenceRoles.size.toString() + " != " + referenceTargets.size)
            }
            return CPNode(id, concept, parentId, roleInParent, childrenIds, propertyRoles, propertyValues, referenceRoles, referenceTargets)
        }

        private fun checkForDuplicates(values: LongArray?) {
            val copy: LongArray = LongArray(values!!.size)
            System.arraycopy(values, 0, copy, 0, values.size)
            Arrays.sort(copy)
            for (i in 1 until copy.size) {
                if (copy.get(i - 1) == copy.get(i)) {
                    throw RuntimeException("Duplicate value: " + copy.get(i))
                }
            }
        }

        fun deserialize(input: String): CPNode {
            try {
                val parts: Array<String> = input.split("/").dropLastWhile({ it.isEmpty() }).toTypedArray()
                val properties: List<Array<String>> = Arrays.stream(parts.get(5).split(",").toTypedArray())
                        .filter(Predicate({ cs: String? -> StringUtils.isNotEmpty(cs) }))
                        .map(Function({ it: String -> it.split("=").dropLastWhile({ it.isEmpty() }).toTypedArray() }))
                        .collect(Collectors.toList())
                val references: List<Array<String>> = Arrays.stream(parts.get(6).split(",").toTypedArray())
                        .filter(Predicate({ cs: String? -> StringUtils.isNotEmpty(cs) }))
                        .map(Function({ it: String -> it.split("=").dropLastWhile({ it.isEmpty() }).toTypedArray() }))
                        .collect(Collectors.toList())
                return CPNode(
                        SerializationUtil.longFromHex(parts.get(0)),
                        SerializationUtil.unescape(parts.get(1)),
                        SerializationUtil.longFromHex(parts.get(2)),
                        SerializationUtil.unescape(parts.get(3)),
                        Arrays.stream(parts.get(4).split(",").toTypedArray())
                                .filter(Predicate({ cs: String? -> StringUtils.isNotEmpty(cs) }))
                                .mapToLong(ToLongFunction({ obj: String? -> SerializationUtil.longFromHex() }))
                                .toArray(),
                        properties.stream().map(Function({ it: Array<String> -> SerializationUtil.unescape(it.get(0)) })).toArray(IntFunction({ _Dummy_.__Array__() })),
                        properties.stream().map(Function({ it: Array<String> -> SerializationUtil.unescape(it.get(1)) })).toArray(IntFunction({ _Dummy_.__Array__() })),
                        references.stream().map(Function({ it: Array<String> -> SerializationUtil.unescape(it.get(0)) })).toArray(IntFunction({ _Dummy_.__Array__() })),
                        references.stream()
                                .map(Function<Array<String>, CPElementRef>({ it: Array<String> -> CPElementRef.Companion.fromString(SerializationUtil.unescape(it.get(1))) }))
                                .toArray(IntFunction<Array<CPElementRef>>({ _Dummy_.__Array__() }))
                )
            } catch (ex: Exception) {
                throw RuntimeException("Failed to deserialize " + input, ex)
            }
        }
    }

}