package org.modelix.model.persistent

class CPTree constructor(val id: String?, val rootId: Long,
                         /**
                          * SHA to CPHamtNode
                          */
                         var idToHash: String?) {

    fun serialize(): String {
        return id + "/" + rootId + "/" + idToHash
    }

    companion object {
        fun deserialize(input: String): CPTree {
            val parts: Array<String> = input.split("/").dropLastWhile({ it.isEmpty() }).toTypedArray()
            return CPTree(parts.get(0), parts.get(1).toLong(), parts.get(2))
        }
    }

}