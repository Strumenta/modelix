package org.modelix.model.operations

import org.modelix.model.api.IWriteTransaction
import org.modelix.model.operations.MoveNodeOp
import java.text.MessageFormat
import java.util.*

class MoveNodeOp constructor(val childId: Long, val sourceParentId: Long, val sourceRole: String?, val sourceIndex: Int, val targetParentId: Long, val targetRole: String?, val targetIndex: Int) : AbstractOperation(), IModifiesChildrenOp {
    fun withIndex(newSourceIndex: Int, newTargetIndex: Int): MoveNodeOp {
        return (if (newSourceIndex == sourceIndex && newTargetIndex == targetIndex) this else MoveNodeOp(childId, sourceParentId, sourceRole, newSourceIndex, targetParentId, targetRole, newTargetIndex))
    }

    public override fun apply(transaction: IWriteTransaction?): IAppliedOperation {
        transaction!!.moveChild(targetParentId, targetRole, targetIndex, childId)
        return Applied()
    }

    public override fun transform(previous: IOperation): IOperation? {
        if (previous is AddNewChildOp) {
            val o: AddNewChildOp = previous
            return withIndex(o.adjustIndex(sourceParentId, sourceRole, sourceIndex), o.adjustIndex(targetParentId, targetRole, targetIndex))
        } else if (previous is DeleteNodeOp) {
            val o: DeleteNodeOp = previous
            if (Objects.equals(o.parentId, sourceParentId) && Objects.equals(o.role, sourceRole) && Objects.equals(o.index, sourceIndex)) {
                if (!(Objects.equals(o.childId, childId))) {
                    throw RuntimeException(sourceParentId.toString() + "." + sourceRole + "[" + sourceIndex + "] expected to be " + childId + ", but was " + o.childId)
                }
                return NoOp()
            } else {
                return withIndex(o.adjustIndex(sourceParentId, sourceRole, sourceIndex), o.adjustIndex(targetParentId, targetRole, targetIndex))
            }
        } else if (previous is MoveNodeOp) {
            val o: MoveNodeOp = previous
            return withIndex(o.adjustIndex(sourceParentId, sourceRole, sourceIndex), o.adjustIndex(targetParentId, targetRole, targetIndex))
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
        if (Objects.equals(otherParentId, sourceParentId) && Objects.equals(otherRole, sourceRole) && (sourceIndex < otherIndex)) {
            adjustedIndex--
        }
        if (Objects.equals(otherParentId, targetParentId) && Objects.equals(otherRole, targetRole) && (targetIndex <= otherIndex)) {
            adjustedIndex++
        }
        return adjustedIndex
    }

    public override fun toString(): String {
        return MessageFormat.format("MoveNodeOp {0}, {1}.{2}[{3}]->{4}.{5}[{6}]", java.lang.Long.toHexString(childId), java.lang.Long.toHexString(sourceParentId), sourceRole, sourceIndex, java.lang.Long.toHexString(targetParentId), targetRole, targetIndex)
    }

    inner class Applied constructor() : AbstractOperation.Applied(), IAppliedOperation {
        override val originalOp: IOperation
            get() {
                return this@MoveNodeOp
            }

        public override fun invert(): IOperation {
            return MoveNodeOp(childId, targetParentId, targetRole, targetIndex, sourceParentId, sourceRole, sourceIndex)
        }
    }

}