package org.modelix.model.operations

open interface IModifiesChildrenOp {
    fun adjustIndex(parentId: Long, role: String?, index: Int): Int
}