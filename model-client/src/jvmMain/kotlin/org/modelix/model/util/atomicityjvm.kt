package org.modelix.model.util

import java.util.concurrent.atomic.AtomicLong

actual class AtomicLong actual constructor(value: Long){
    private val wrapped = AtomicLong(value)

    actual fun incrementAndGet() : Long = wrapped.incrementAndGet()
}

actual fun random() : Double = Math.random()