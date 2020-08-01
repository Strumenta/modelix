package org.modelix.model.persistent

import java.util.*
import java.util.function.BinaryOperator

class CPHamtInternal constructor(var bitmap: Int,
                                 /**
                                  * SHA to CPHamtNode
                                  */
                                 val children: Array<String?>?) : CPHamtNode() {

    public override fun serialize(): String {
        return String.format("I/%s/%s",
                SerializationUtil.intToHex(bitmap),
                Arrays.stream(children).reduce(BinaryOperator({ a: String?, b: String? -> a + "," + b })).orElse("")
        )
    }

}