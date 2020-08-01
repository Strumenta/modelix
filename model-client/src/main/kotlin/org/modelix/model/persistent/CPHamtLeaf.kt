package org.modelix.model.persistent

class CPHamtLeaf constructor(val key: Long,
                             /**
                              * SHA to CPElement
                              */
                             val value: String) : CPHamtNode() {

    public override fun serialize(): String {
        return "L/" + SerializationUtil.longToHex(key) + "/" + value
    }

}