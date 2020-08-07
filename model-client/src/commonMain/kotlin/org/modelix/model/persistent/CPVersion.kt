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

import org.modelix.model.operations.IOperation
import org.modelix.model.util.Level
import org.modelix.model.util.LogManager

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
        val opsPartTemp = operations!!.toList()
            .map { op: IOperation? -> OperationSerializer.INSTANCE.serialize(op!!) }
        val opsPart = operationsHash
            ?: if (opsPartTemp.isNotEmpty())
                opsPartTemp.reduce { a: String, b: String -> "$a,$b" } else ""
        var serialized = SerializationUtil.longToHex(id) +
            "/" + SerializationUtil.escape(time) +
            "/" + SerializationUtil.escape(author) +
            "/" + SerializationUtil.nullAsEmptyString(treeHash) +
            "/" + SerializationUtil.nullAsEmptyString(previousVersion) +
            "/" + opsPart
        if (numberOfOperations >= 0) {
            serialized += "/$numberOfOperations"
        }
        return serialized
    }

    val hash: String
        get() = HashUtil.sha256(serialize())

    companion object {
        private val LOG = LogManager.getLogger(CPVersion::class)
        fun deserialize(input: String): CPVersion {
            val parts = input.split("/").dropLastWhile { it.isEmpty() }.toTypedArray()
            var opsHash: String? = null
            var ops: Array<IOperation?>? = null
            if (HashUtil.isSha256(parts[5])) {
                opsHash = parts[5]
            } else {
                ops = parts[5].split(",").toTypedArray()
                    .filter { cs: String? -> !cs.isNullOrEmpty() }
                    .map { serialized: String? -> OperationSerializer.INSTANCE.deserialize(serialized!!) }
                    .toTypedArray()
            }
            val numOps = if (parts.size >= 7) parts[6].toInt() else -1
            return CPVersion(
                SerializationUtil.longFromHex(parts[0]),
                SerializationUtil.unescape(parts[1]),
                SerializationUtil.unescape(parts[2]),
                SerializationUtil.emptyStringAsNull(parts[3]),
                SerializationUtil.emptyStringAsNull(parts[4]),
                ops,
                opsHash,
                numOps
            )
        }
    }

    init {
        if (treeHash == null || treeHash.length == 0) {
            if (LOG.isEnabledFor(Level.WARN)) {
                LOG.warn("No tree hash provided", Exception())
            }
        }
        if (operations == null == (operationsHash == null)) {
            throw RuntimeException("Only one of 'operations' and 'operationsHash' can be provided")
        }
        this.id = id
        this.author = author
        this.previousVersion = previousVersion
        this.time = time
        this.treeHash = treeHash
        this.operations = operations
        this.operationsHash = operationsHash
        this.numberOfOperations = operations?.size ?: numberOfOperations
    }
}
