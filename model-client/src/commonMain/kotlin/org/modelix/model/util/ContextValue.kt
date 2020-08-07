package org.modelix.model.util

import kotlin.native.concurrent.ThreadLocal

class ContextValue<E> {
    private var defaultValue: E? = null
    @ThreadLocal
    private var value = mutableListOf<E>()

    constructor() {}
    constructor(defaultValue: E) {
        this.defaultValue = defaultValue
    }

    private val stack: MutableList<E>
        private get() {
            var stack = value
            if (stack == null) {
                stack = ArrayList()
                value = stack
            }
            return stack
        }

    fun runWith(newValue: E, r: Runnable) {
        try {
            stack.add(newValue)
            r.run()
        } finally {
            val stack: MutableList<E> = stack
            stack.removeAt(stack.size - 1)
        }
    }

    fun <T> computeWith(newValue: E, r: Supplier<T>): T {
        return try {
            stack.add(newValue)
            r.get()
        } finally {
            val stack: MutableList<E> = stack
            stack.removeAt(stack.size - 1)
        }
    }

    fun getValue(): E? {
        val stack: List<E> = stack
        return if (stack.isEmpty()) defaultValue else stack[stack.size - 1]
    }

    val allValues: Iterable<E>
        get() = stack
}
