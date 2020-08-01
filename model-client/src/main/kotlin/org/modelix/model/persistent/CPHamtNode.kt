package org.modelix.model.persistent

import java.util.*
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Predicate

abstract class CPHamtNode constructor() {
    abstract fun serialize(): String

    companion object {
        val DESERIALIZER: Function<String, CPHamtNode?> = Function({ s: String -> deserialize(s) })
        fun deserialize(input: String): CPHamtNode {
            val parts: Array<String> = input.split("/").dropLastWhile({ it.isEmpty() }).toTypedArray()
            if (("L" == parts.get(0))) {
                return CPHamtLeaf(SerializationUtil.longFromHex(parts.get(1)), parts.get(2))
            } else if (("I" == parts.get(0))) {
                return CPHamtInternal(
                        SerializationUtil.intFromHex(parts.get(1)),
                        Arrays.stream(parts.get(2).split(",").toTypedArray())
                                .filter(Predicate({ it: String? -> (it != null && it.length > 0) }))
                                .toArray(IntFunction({ _Dummy_.__Array__() })))
            } else {
                throw RuntimeException("Unknown type: " + parts.get(0) + ", input: " + input)
            }
        }
    }
}