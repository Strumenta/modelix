package org.modelix.model.util

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

actual class AtomicLong actual constructor(value: Long){
    private val wrapped = AtomicLong(value)

    actual fun incrementAndGet() : Long = wrapped.incrementAndGet()
}

actual class AtomicBoolean actual constructor(value: Boolean) {
    private val wrapped = AtomicBoolean(value)

    actual constructor() : this(false)

    actual fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean =
            wrapped.compareAndSet(expectedValue, newValue)

    actual fun set(newValue: Boolean) {
        wrapped.set(newValue)
    }

}

actual fun random() : Double = Math.random()

actual fun sleep(ms: Long) {
    try {
        Thread.sleep(ms)
    } catch (ex2: InterruptedException) {
    }
}