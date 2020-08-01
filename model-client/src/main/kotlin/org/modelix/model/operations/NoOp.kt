package org.modelix.model.operations

import org.modelix.model.api.IWriteTransaction

class NoOp constructor() : AbstractOperation(), IAppliedOperation {
    public override fun apply(transaction: IWriteTransaction?): IAppliedOperation {
        return this
    }

    public override fun transform(previous: IOperation): IOperation? {
        return null
    }

    override val originalOp: IOperation
        get() {
            return this
        }

    public override fun invert(): IOperation {
        return this
    }
}