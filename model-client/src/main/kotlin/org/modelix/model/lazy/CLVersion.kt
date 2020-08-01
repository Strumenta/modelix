package org.modelix.model.lazy

import org.modelix.model.operations.IOperation
import org.modelix.model.persistent.CPOperationsList
import org.modelix.model.persistent.CPVersion
import java.util.*

class CLVersion {
    private var store: IDeserializingKeyValueStore?
    var data: CPVersion? = null
        private set

    constructor(id: Long, time: String?, author: String?, treeHash: String?, previousVersion: String?, operations: Array<IOperation?>, store: IDeserializingKeyValueStore?) {
        this.store = store
        if (operations.size <= 10) {
            data = CPVersion(id, time, author, treeHash, previousVersion, operations, null, operations.size)
        } else {
            val opsList = CPOperationsList(operations)
            IDeserializingKeyValueStore_extensions.put(store, opsList, opsList.serialize())
            data = CPVersion(id, time, author, treeHash, previousVersion, null, opsList.hash, operations.size)
        }
        IDeserializingKeyValueStore_extensions.put(store, data, data!!.serialize())
    }

    constructor(hash: String?, store: IDeserializingKeyValueStore) : this(store.get<CPVersion>(hash) { input: String -> CPVersion.Companion.deserialize(input) }!!, store) {}
    constructor(data: CPVersion, store: IDeserializingKeyValueStore?) {
        if (data == null) {
            throw NullPointerException("data is null")
        }
        this.data = data
        this.store = store
    }

    val author: String?
        get() = data!!.author

    val id: Long
        get() = data!!.id

    val time: String?
        get() = data!!.time

    val hash: String?
        get() = data.getHash()

    val previousHash: String?
        get() = data!!.previousVersion

    val treeHash: String?
        get() = data!!.treeHash

    val tree: CLTree
        get() = CLTree(treeHash, store)

    val previousVersion: CLVersion?
        get() {
            if (data!!.previousVersion == null) {
                return null
            }
            val previousVersion = store!!.get<CPVersion>(data!!.previousVersion) { input: String -> CPVersion.Companion.deserialize(input) }
                    ?: return null
            return CLVersion(previousVersion, store)
        }

    val operations: Iterable<IOperation>
        get() {
            val ops = if (data!!.operationsHash == null) data!!.operations else store!!.get<CPOperationsList>(data!!.operationsHash) { input: String -> CPOperationsList.Companion.deserialize(input) }!!.operations
            return Iterable { Arrays.stream(ops).iterator() }
        }

    val numberOfOperations: Int
        get() = data!!.numberOfOperations

    fun operationsInlined(): Boolean {
        return data!!.operations != null
    }

    companion object {
        fun loadFromHash(hash: String?, store: IDeserializingKeyValueStore?): CLVersion? {
            val data = store!!.get<CPVersion>(hash) { input: String -> CPVersion.Companion.deserialize(input) }
            return data?.let { CLVersion(it, store) }
        }
    }
}