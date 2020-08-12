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

package org.modelix.model.operations

import org.modelix.model.api.IConcept
import org.modelix.model.api.ITree
import org.modelix.model.api.IWriteTransaction
import org.modelix.model.persistent.SerializationUtil

class AddNewChildOp(val parentId: Long, val role: String?, val index: Int, val childId: Long, val concept: IConcept?) : AbstractOperation() {
    fun withIndex(newIndex: Int): AddNewChildOp {
        return if (newIndex == index) this else AddNewChildOp(parentId, role, newIndex, childId, concept)
    }

    override fun apply(transaction: IWriteTransaction): IAppliedOperation {
        transaction.addNewChild(parentId, role, index, childId, concept)
        return Applied()
    }

    override fun transform(previous: IOperation, indexAdjustments: IndexAdjustments): IOperation {
        val adjusted = {
            val a = withAdjustedIndex(indexAdjustments)
            indexAdjustments.nodeAdd(parentId, role, index)
            a
        }
        return when (previous) {
            is AddNewChildOp -> adjusted()
            is DeleteNodeOp -> {
                if (previous.childId == this.parentId) {
                    indexAdjustments.concurrentNodeAdd(ITree.ROOT_ID, ITree.DETACHED_NODES_ROLE, 0)
                    AddNewChildOp(ITree.ROOT_ID, ITree.DETACHED_NODES_ROLE, 0, this.childId, this.concept)
                } else {
                    adjusted()
                }
            }
            is MoveNodeOp -> adjusted()
            is SetPropertyOp -> adjusted()
            is SetReferenceOp -> adjusted()
            is NoOp -> adjusted()
            else -> throw RuntimeException("Unknown type: " + previous::class.simpleName)
        }
    }

    override fun loadAdjustment(indexAdjustments: IndexAdjustments) {
        indexAdjustments.concurrentNodeAdd(parentId, role, index)
    }

    override fun undoAdjustment(indexAdjustments: IndexAdjustments) {
        indexAdjustments.undoConcurrentNodeAdd(parentId, role, index)
    }

    override fun withAdjustedIndex(indexAdjustments: IndexAdjustments): IOperation {
        return withIndex(indexAdjustments.getAdjustedIndex(parentId, role, index, true))
    }

    override fun toString(): String {
        return "AddNewChildOp ${SerializationUtil.longToHex(childId)}, ${SerializationUtil.longToHex(parentId)}.$role[$index], $concept"
    }

    override fun toCode(): String {
        return """t.addNewChild(0x${parentId.toString(16)}, "$role", $index, 0x${childId.toString(16)}, null)"""
    }

    inner class Applied : AbstractOperation.Applied(), IAppliedOperation {
        override val originalOp: IOperation
            get() = this@AddNewChildOp

        override fun invert(): IOperation {
            return DeleteNodeOp(parentId, role, index, childId)
        }
    }
}
