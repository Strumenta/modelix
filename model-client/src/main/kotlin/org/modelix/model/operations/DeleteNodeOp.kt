package org.modelix.model.operations

import org.modelix.model.api.IConcept
import org.modelix.model.api.IWriteTransaction
import org.modelix.model.operations.DeleteNodeOp
import java.text.MessageFormat
import java.util.*

class DeleteNodeOp constructor(val parentId: Long, val role: String?, val index: Int, val childId: Long) : AbstractOperation(), IModifiesChildrenOp {
    fun withIndex(newIndex: Int): DeleteNodeOp {
        return (if (newIndex == index) this else DeleteNodeOp(parentId, role, newIndex, childId))
    }

    public override fun apply(transaction: IWriteTransaction?): IAppliedOperation {
        val concept: IConcept? = transaction!!.getConcept(childId)
        transaction.deleteNode(childId)
        return Applied((concept)!!)
    }

    public override fun transform(previous: IOperation): IOperation? {
        if (previous is DeleteNodeOp) {
            val o: DeleteNodeOp = previous
            if (Objects.equals(parentId, o.parentId) && Objects.equals(role, o.role)) {
                if (o.index < index) {
                    return DeleteNodeOp(parentId, role, index - 1, childId)
                } else if (Objects.equals(o.index, index)) {
                    if (!(Objects.equals(o.childId, childId))) {
                        throw RuntimeException("Both operations delete " + parentId + "." + role + "[" + index + "] but with different expected IDs " + childId + " and " + o.childId)
                    }
                    return NoOp()
                } else {
                    return this
                }
            } else {
                return this
            }
        } else if (previous is AddNewChildOp) {
            val o: AddNewChildOp = previous
            if (Objects.equals(parentId, o.parentId) && Objects.equals(role, o.role)) {
                if (o.index <= index) {
                    return DeleteNodeOp(parentId, role, index + 1, childId)
                } else {
                    return this
                }
            } else {
                return this
            }
        } else if (previous is MoveNodeOp) {
            val o: MoveNodeOp = previous
            if (Objects.equals(o.childId, childId)) {
                if (!(Objects.equals(o.sourceParentId, parentId)) || !(Objects.equals(o.sourceRole, role)) || !(Objects.equals(o.sourceIndex, index))) {
                    throw RuntimeException("node " + childId + " expected to be at " + parentId + "." + role + "[" + index + "]" + " but was " + o.sourceParentId + "." + o.sourceRole + "[" + o.sourceIndex + "]")
                }
                return DeleteNodeOp(o.targetParentId, o.targetRole, o.targetIndex, childId)
            } else if (Objects.equals(parentId, o.targetParentId) && Objects.equals(role, o.targetRole)) {
                return withIndex(o.adjustIndex(parentId, role, index))
            } else if (Objects.equals(parentId, o.sourceParentId) && Objects.equals(role, o.sourceRole)) {
                if (o.sourceIndex == index) {
                    throw RuntimeException("Node at " + parentId + "." + role + "[" + index + "] is expected to be " + childId + ", but was " + o.childId)
                } else {
                    return withIndex(o.adjustIndex(parentId, role, index))
                }
            } else {
                return this
            }
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
        if (Objects.equals(otherParentId, parentId) && Objects.equals(otherRole, role) && (index < otherIndex)) {
            adjustedIndex--
        }
        return adjustedIndex
    }

    public override fun toString(): String {
        return MessageFormat.format("DeleteNodeOp {0}, {1}.{2}[{3}]", java.lang.Long.toHexString(childId), java.lang.Long.toHexString(parentId), role, index)
    }

    inner class Applied constructor(private val concept: IConcept) : AbstractOperation.Applied(), IAppliedOperation {
        override val originalOp: IOperation
            get() {
                return this@DeleteNodeOp
            }

        public override fun invert(): IOperation {
            return AddNewChildOp(parentId, role, index, childId, concept)
        }

        public override fun toString(): String {
            return super.toString() + ", concept: " + concept
        }

    }

}