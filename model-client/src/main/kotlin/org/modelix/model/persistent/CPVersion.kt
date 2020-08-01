package org.modelix.model.persistent

import org.apache.commons.lang3.StringUtils
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.modelix.model.operations.IOperation
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.stream.Stream

class CPVersion constructor(id: Long, time: String?, author: String?, treeHash: String?, previousVersion: String?, operations: Array<IOperation?>?, operationsHash: String?, numberOfOperations: Int) {
    val id: Long
    val time: String?
    val author: String?

    /**
     * SHA to CPTree
     */
    val treeHash: String?
    val previousVersion: String?
    val operations: Array<IOperation?>?
    val operationsHash: String?
    val numberOfOperations: Int
    fun serialize(): String {
        val opsPart: String = if (operationsHash != null) operationsHash else Stream.of(*operations)
                .map(Function<IOperation, String>({ op: IOperation -> OperationSerializer.Companion.INSTANCE.serialize(op) }))
                .reduce(BinaryOperator({ a: String, b: String -> a + "," + b }))
                .orElse("")
        var serialized: String = (SerializationUtil.longToHex(id) +
                "/" + SerializationUtil.escape(time) +
                "/" + SerializationUtil.escape(author) +
                "/" + SerializationUtil.nullAsEmptyString(treeHash) +
                "/" + SerializationUtil.nullAsEmptyString(previousVersion) +
                "/" + opsPart)
        if (numberOfOperations >= 0) {
            serialized += "/" + numberOfOperations
        }
        return serialized
    }

    val hash: String?
        get() {
            return HashUtil.sha256(serialize())
        }

    companion object {
        private val LOG: Logger = LogManager.getLogger(CPVersion::class.java)
        fun deserialize(input: String): CPVersion {
            val parts: Array<String> = input.split("/").dropLastWhile({ it.isEmpty() }).toTypedArray()
            var opsHash: String? = null
            var ops: Array<IOperation?>? = null
            if (HashUtil.isSha256(parts.get(5))) {
                opsHash = parts.get(5)
            } else {
                ops = Stream.of(*parts.get(5).split(",").toTypedArray())
                        .filter(Predicate({ cs: String? -> StringUtils.isNotEmpty(cs) }))
                        .map(Function<String, IOperation>({ serialized: String -> OperationSerializer.Companion.INSTANCE.deserialize(serialized) }))
                        .toArray(IntFunction<Array<IOperation>>({ _Dummy_.__Array__() }))
            }
            val numOps: Int = (if (parts.size >= 7) parts.get(6).toInt() else -1)
            return CPVersion(
                    SerializationUtil.longFromHex(parts.get(0)),
                    SerializationUtil.unescape(parts.get(1)),
                    SerializationUtil.unescape(parts.get(2)),
                    SerializationUtil.emptyStringAsNull(parts.get(3)),
                    SerializationUtil.emptyStringAsNull(parts.get(4)),
                    ops,
                    opsHash,
                    numOps)
        }
    }

    init {
        if ((treeHash == null || treeHash.length == 0)) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("No tree hash provided", Exception())
            }
        }
        if ((operations == null) == (operationsHash == null)) {
            throw RuntimeException("Only one of 'operations' and 'operationsHash' can be provided")
        }
        this.id = id
        this.author = author
        this.previousVersion = previousVersion
        this.time = time
        this.treeHash = treeHash
        this.operations = operations
        this.operationsHash = operationsHash
        this.numberOfOperations = (if (operations != null) operations.size else numberOfOperations)
    }
}