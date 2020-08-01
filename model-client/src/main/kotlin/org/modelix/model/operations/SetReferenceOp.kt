package org.modelix.model.operations

import org.modelix.model.api.INodeReference
import org.modelix.model.api.IWriteTransaction
import java.text.MessageFormat

class SetReferenceOp constructor(val sourceId: Long, val role: String?, val target: INodeReference?) : AbstractOperation() {
    public override fun apply(transaction: IWriteTransaction?): IAppliedOperation {
        val oldValue: INodeReference? = transaction!!.getReferenceTarget(sourceId, role)
        transaction.setReferenceTarget(sourceId, role, target)
        return Applied((oldValue)!!)
    }

    public override fun transform(previous: IOperation): IOperation? {
        if (previous is SetPropertyOp) {
            return this
        } else if (previous is SetReferenceOp) {
            return this
        } else if (previous is AddNewChildOp) {
            return this
        } else if (previous is DeleteNodeOp) {
            if (sourceId == previous.parentId) {
                return NoOp()
            } else {
                return this
            }
        } else if (previous is MoveNodeOp) {
            return this
        } else {
            throw RuntimeException("Unknown type: " + previous.javaClass.getName())
        }
    }

    public override fun toString(): String {
        return MessageFormat.format("SetReferenceOp {0}.{1} = {2}", java.lang.Long.toHexString(sourceId), role, target)
    }

    inner class Applied constructor(private val oldValue: INodeReference) : AbstractOperation.Applied(), IAppliedOperation {
        override val originalOp: IOperation
            get() {
                return this@SetReferenceOp
            }

        public override fun invert(): IOperation {
            return SetReferenceOp(sourceId, role, oldValue)
        }

        public override fun toString(): String {
            return super.toString() + ", oldValue: " + oldValue
        }

    }

}