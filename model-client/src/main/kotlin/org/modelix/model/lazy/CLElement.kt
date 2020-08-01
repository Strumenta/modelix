package org.modelix.model.lazy

import org.modelix.model.api.ITree
import org.modelix.model.persistent.CPElement
import org.modelix.model.persistent.CPNode

abstract class CLElement(protected val tree: CLTree, protected val data: CPElement) {
    open fun getData(): CPElement? {
        return data
    }

    val id: Long
        get() = data.id

    fun getTree(): ITree {
        return tree
    }

    val parent: CLNode?
        get() = tree.resolveElement(data.parentId) as CLNode

    val roleInParent: String?
        get() = data.roleInParent

    val ref: CLElementRef
        get() = CLElementRef(id)

    companion object {
        fun create(tree: CLTree, data: CPElement?): CLNode? {
            return if (data == null) {
                null
            } else CLNode(tree, data as CPNode)
        }
    }

}