package org.modelix.model.operations

import org.modelix.model.operations.AbstractOperation

abstract class AbstractOperation constructor() : IOperation {
    inner abstract class Applied constructor() {
        public override fun toString(): String {
            return "applied:" + this@AbstractOperation.toString()
        }
    }
}