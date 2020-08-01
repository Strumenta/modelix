package org.modelix.model.operations

import org.modelix.model.api.IWriteTransaction
import org.modelix.model.operations.SetPropertyOp
import java.text.MessageFormat

class SetPropertyOp constructor(val nodeId: Long, val role: String?, val value: String?) : AbstractOperation() {
    public override fun apply(transaction: IWriteTransaction?): IAppliedOperation {
        val oldValue: String? = transaction!!.getProperty(nodeId, role)
        transaction.setProperty(nodeId, role, value)
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
            if (nodeId == previous.childId) {
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
        return MessageFormat.format("SetPropertOp {0}.{1} = {2}", java.lang.Long.toHexString(nodeId), role, value)
    }

    inner class Applied constructor(private val oldValue: String) : AbstractOperation.Applied(), IAppliedOperation {
        override val originalOp: IOperation
            get() {
                return this@SetPropertyOp
            }

        public override fun invert(): IOperation {
            return SetPropertyOp(nodeId, role, oldValue)
        }

        public override fun toString(): String {
            return super.toString() + ", oldValue: " + oldValue
        }

    }

}