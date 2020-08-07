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

package org.modelix.model.lazy

import org.modelix.model.persistent.CPElement
import org.modelix.model.persistent.CPHamtInternal
import org.modelix.model.persistent.CPHamtLeaf
import org.modelix.model.persistent.CPHamtNode
import org.modelix.model.util.BiPredicate

actual abstract class CLHamtNode<E : CPHamtNode?> actual constructor(store_: IDeserializingKeyValueStore) {
    protected actual var store: IDeserializingKeyValueStore = store_
    protected fun createEmptyNode(): CLHamtNode<*> {
        return CLHamtNodeCompanion.create(CPHamtInternal(0, arrayOfNulls(0)), store)!!
    }

    actual abstract fun getData(): CPHamtNode?

    operator fun get(key: Long): String? {
        val bulkQuery: IBulkQuery = NonBulkQuery(store)
        return get(key, 0, bulkQuery)!!.execute()
    }

    fun getAll(keys: Iterable<Long>?, bulkQuery: IBulkQuery): IBulkQuery.Value<List<String?>?>? {
        val f: org.modelix.model.util.Function<Long, IBulkQuery.Value<String?>?>? = org.modelix.model.util.Function { key: Long -> get(key, 0, bulkQuery) }
        return bulkQuery.map(keys, f)
    }

    fun put(key: Long, value: String?): CLHamtNode<*>? {
        return put(key, value, 0)
    }

    fun put(element: CLElement): CLHamtNode<*>? {
        return put(element.id, element.getData()!!.hash)
    }

    fun put(data: CPElement): CLHamtNode<*>? {
        return put(data.id, data.hash)
    }

    fun remove(key: Long): CLHamtNode<*>? {
        return remove(key, 0)
    }

    fun remove(element: CLElement): CLHamtNode<*>? {
        return remove(element.id)
    }

    actual abstract operator fun get(key: Long, shift: Int, bulkQuery: IBulkQuery?): IBulkQuery.Value<String?>?
    actual abstract fun put(key: Long, value: String?, shift: Int): CLHamtNode<*>?
    actual abstract fun remove(key: Long, shift: Int): CLHamtNode<*>?
    actual abstract fun visitEntries(visitor: BiPredicate<Long?, String?>?): Boolean
    actual abstract fun visitChanges(oldNode: CLHamtNode<*>?, visitor: IChangeVisitor?)
    actual interface IChangeVisitor {
        actual fun entryAdded(key: Long, value: String?)
        actual fun entryRemoved(key: Long, value: String?)
        actual fun entryChanged(key: Long, oldValue: String?, newValue: String?)
    }

}

