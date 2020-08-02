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

package org.modelix.model.api

import org.modelix.model.api.IBranch

class WriteTransaction(_tree: ITree?, branch: IBranch?, idGenerator: IIdGenerator) : Transaction(branch), IWriteTransaction {
    override var tree: ITree? = _tree
        set(value) {
            checkNotClosed()
            tree = value
            field = value
        }
    private var closed = false
    protected var idGenerator: IIdGenerator
    fun close() {
        closed = true
    }

    protected fun checkNotClosed() {
        check(!closed) { "Transaction is already closed" }
    }

    override fun setProperty(nodeId: Long, role: String?, value: String?) {
        checkNotClosed()
        tree = tree!!.setProperty(nodeId, role, value)
    }

    override fun setReferenceTarget(sourceId: Long, role: String?, target: INodeReference?) {
        checkNotClosed()
        tree = tree!!.setReferenceTarget(sourceId, role, target)
    }

    override fun moveChild(newParentId: Long, newRole: String?, newIndex: Int, childId: Long) {
        checkNotClosed()
        tree = tree!!.moveChild(newParentId, newRole, newIndex, childId)
    }

    override fun addNewChild(parentId: Long, role: String?, index: Int, concept: IConcept?): Long {
        checkNotClosed()
        val newId = idGenerator.generate()
        addNewChild(parentId, role, index, newId, concept)
        return newId
    }

    override fun addNewChild(parentId: Long, role: String?, index: Int, childId: Long, concept: IConcept?) {
        checkNotClosed()
        tree = tree!!.addNewChild(parentId, role, index, childId, concept)
    }

    override fun deleteNode(nodeId: Long) {
        checkNotClosed()
        tree!!.getAllChildren(nodeId)!!.forEach { nodeId: Long -> deleteNode(nodeId) }
        tree = tree!!.deleteNode(nodeId)
    }

    init {
        this.tree = tree
        this.idGenerator = idGenerator
    }
}
