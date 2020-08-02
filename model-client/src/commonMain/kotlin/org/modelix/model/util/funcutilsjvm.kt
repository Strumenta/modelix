package org.modelix.model.util

interface Runnable {
    fun run()
}
interface Supplier<T> {
    fun get(): T
}

fun Runnable(handler: () -> Unit): Runnable = object : Runnable {
    override fun run() {
        handler()
    }
}

fun <T> Supplier(handler: () -> T): Supplier<T> = object : Supplier<T> {
    override fun get(): T = handler()
}

interface Consumer<T> {
    fun accept(t: T)
}

fun <T> Consumer(handler: (T) -> Unit): Consumer<T> = object : Consumer<T> {
    override fun accept(t: T) {
        handler(t)
    }
}

interface Function<T, R> {
    fun apply(t: T): R
}

fun <T, R> Function(handler: (T) -> R): Function<T, R> = object : Function<T, R> {
    override fun apply(t: T): R {
        return handler(t)
    }
}

interface BiFunction<T, U, R> {
    fun apply(t: T, u: U): R
}

fun <T, U, R> BiFunction(handler: (T, U) -> R): BiFunction<T, U, R> = object : BiFunction<T, U, R> {
    override fun apply(t: T, u: U): R = handler(t, u)
}

interface Tuple {
    companion object {
        fun <T1,T2> of(t1: T1, t2: T2) : Tuple2<T1,T2> {
            return object : Tuple2<T1, T2> {
                override val _1: T1
                    get() = t1
                override val _2: T2
                    get() = t2

            }
        }
    }
}
interface Tuple2<T1,T2> : Tuple {
    val _1: T1
    val _2: T2
}

expect fun Long.toHexString() : String