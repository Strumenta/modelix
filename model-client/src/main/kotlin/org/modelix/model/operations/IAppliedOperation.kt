package org.modelix.model.operations

open interface IAppliedOperation {
    val originalOp: IOperation
    fun invert(): IOperation
}