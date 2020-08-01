package org.modelix.model.lazy

import org.apache.commons.lang3.mutable.MutableObject
import org.modelix.model.persistent.CPHamtLeaf
import org.modelix.model.persistent.CPHamtNode
import org.modelix.model.persistent.HashUtil
import java.util.function.BiPredicate

class CLHamtLeaf : CLHamtNode<CPHamtLeaf?> {
    private override val data: CPHamtLeaf

    constructor(data: CPHamtLeaf, store: IDeserializingKeyValueStore?) : super(store) {
        this.data = data
    }

    private constructor(key: Long, value: String, store: IDeserializingKeyValueStore) : super(store) {
        data = CPHamtLeaf(key, value)
        val serialized = data.serialize()
        store.put(HashUtil.sha256(serialized), data, serialized)
    }

    override fun getData(): CPHamtNode {
        return data
    }

    val key: Long
        get() = data.getKey()

    val value: String?
        get() = data.getValue()

    override fun put(key: Long, value: String?, shift: Int): CLHamtNode<*>? {
        return if (key == data.key) {
            if (value == data.value) {
                this
            } else {
                create(key, value, store!!)
            }
        } else {
            if (shift > CLHamtNode.Companion.MAX_SHIFT) {
                throw RuntimeException(shift.toString() + " > " + CLHamtNode.Companion.MAX_SHIFT)
            }
            var result = createEmptyNode()
            result = result!!.put(data.key, data.value, shift)
            if (result == null) {
                result = createEmptyNode()
            }
            result = result!!.put(key, value, shift)
            result
        }
    }

    override fun remove(key: Long, shift: Int): CLHamtNode<*>? {
        return if (key == data.key) {
            null
        } else {
            this
        }
    }

    override fun get(key: Long, shift: Int, bulkQuery: IBulkQuery): IBulkQuery.Value<String?>? {
        return bulkQuery.constant(if (data.key == key) data.value else null)
    }

    override fun visitEntries(visitor: BiPredicate<Long?, String?>): Boolean {
        return visitor.test(data.key, data.value)
    }

    override fun visitChanges(oldNode: CLHamtNode<*>, visitor: IChangeVisitor) {
        if (oldNode === this) {
            return
        }
        val oldValue = MutableObject<String?>()
        oldNode.visitEntries { k, v ->
            if (k == data.key) {
                oldValue.setValue(v)
            } else {
                visitor.entryRemoved(k, v)
            }
            true
        }
        if (oldValue.value == null) {
            visitor.entryAdded(data.key, data.value)
        } else if (oldValue.value !== data.value) {
            visitor.entryChanged(data.key, oldValue.value, data.value)
        }
    }

    companion object {
        fun create(key: Long, value: String?, store: IDeserializingKeyValueStore): CLHamtLeaf? {
            return value?.let { CLHamtLeaf(key, it, store) }
        }
    }
}