package org.modelix.model.persistent

abstract class CPElement constructor(val id: Long, val parentId: Long, val roleInParent: String?) {

    abstract fun serialize(): String
    val hash: String?
        get() {
            return HashUtil.sha256(serialize())
        }

    companion object {
        fun deserialize(input: String): CPElement {
            return CPNode.Companion.deserialize(input)
        }
    }

}