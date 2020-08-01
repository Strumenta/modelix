package org.modelix.model.api

class ReadTransaction(override val tree: ITree, branch: IBranch) : Transaction(branch), IReadTransaction {
//    override fun getTree(): ITree? {
//        return tree
//    }

}