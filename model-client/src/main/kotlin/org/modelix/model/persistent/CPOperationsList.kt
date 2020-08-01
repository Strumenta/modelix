package org.modelix.model.persistent

import org.apache.commons.lang3.StringUtils
import org.modelix.model.operations.IOperation
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.stream.Stream

class CPOperationsList constructor(val operations: Array<IOperation?>) {
    fun serialize(): String {
        return Stream.of(*operations)
                .map(Function<IOperation, String>({ op: IOperation -> OperationSerializer.Companion.INSTANCE.serialize(op) }))
                .reduce(BinaryOperator({ a: String, b: String -> a + "," + b }))
                .orElse("")
    }

    val hash: String?
        get() {
            return HashUtil.sha256(serialize())
        }

    companion object {
        fun deserialize(input: String): CPOperationsList {
            return CPOperationsList(
                    Stream.of(*input.split(",").toTypedArray())
                            .filter(Predicate({ cs: String? -> StringUtils.isNotEmpty(cs) }))
                            .map(Function<String, IOperation>({ serialized: String -> OperationSerializer.Companion.INSTANCE.deserialize(serialized) }))
                            .toArray(IntFunction<Array<IOperation>>({ _Dummy_.__Array__() }))
            )
        }
    }

}