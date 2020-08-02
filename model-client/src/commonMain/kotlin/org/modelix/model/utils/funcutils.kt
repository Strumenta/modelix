package org.modelix.model.utils

interface Runnable {
    fun run()
}
interface Supplier<T> {
    fun get() : T
}

fun Runnable(handler: () -> Unit): Runnable = object : Runnable {
    override fun run() {
        handler()
    }
}

fun <T> Supplier(handler: () -> T): Supplier<T> = object : Supplier<T> {
    override fun get(): T = handler()
}