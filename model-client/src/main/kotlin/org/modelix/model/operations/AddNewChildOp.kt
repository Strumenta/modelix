package org.modelix.model.operations

import org.modelix.model.api.IConcept
import org.modelix.model.api.IWriteTransaction
import org.modelix.model.operations.AddNewChildOp
import java.text.MessageFormat
import java.util.*

class AddNewChildOp constructor(val parentId: Long, val role: String?, val index: Int, val childId: Long, val concept: IConcept?) : AbstractOperation(), IModifiesChildrenOp {
    fun withIndex(newIndex: Int): AddNewChildOp {
        return (if (newIndex == index) this else AddNewChildOp(parentId, role, newIndex, childId, concept))
    }

    public override fun apply(transaction: IWriteTransaction?): IAppliedOperation {
        transaction!!.addNewChild(parentId, role, index, childId, concept)
        return Applied()
    }

    public override fun transform(previous: IOperation): IOperation? {
        if (previous is AddNewChildOp) {
            val o: AddNewChildOp = previous
            if (Objects.equals(o.parentId, parentId) && Objects.equals(o.role, role)) {
                if (o.index <= index) {
                    return AddNewChildOp(parentId, role, index + 1, childId, concept)
                } else {
                    return this
                }
            } else {
                return this
            }
        } else if (previous is DeleteNodeOp) {
            return withIndex(previous.adjustIndex(parentId, role, index))
        } else if (previous is MoveNodeOp) {
            return withIndex(previous.adjustIndex(parentId, role, index))
        } else if (previous is SetPropertyOp) {
            return this
        } else if (previous is SetReferenceOp) {
            return this
        } else if (previous is NoOp) {
            return this
        } else {
            throw RuntimeException("Unknown type: " + previous.javaClass.getName())
        }
    }

    public override fun adjustIndex(otherParentId: Long, otherRole: String?, otherIndex: Int): Int {
        var adjustedIndex: Int = otherIndex
        if (Objects.equals(otherParentId, parentId) && Objects.equals(otherRole, role) && (index <= otherIndex)) {
            adjustedIndex++
        }
        return adjustedIndex
    }

    public override fun toString(): String {
        return MessageFormat.format("AddNewChildOp {0}, {1}.{2}[{3}], {4}", java.lang.Long.toHexString(childId), java.lang.Long.toHexString(parentId), role, index, concept)
    }

    inner class Applied constructor() : AbstractOperation.Applied(), IAppliedOperation {
        override val originalOp: IOperation
            get() {
                return this@AddNewChildOp
            }

        public override fun invert(): IOperation {
            return DeleteNodeOp(parentId, role, index, childId)
        }
    }

}