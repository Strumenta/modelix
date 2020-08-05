package org.modelix.model.util

expect class ContextValue<E>() {
    fun runWith(newValue: E, r: Runnable)
    fun getValue(): E?
}
