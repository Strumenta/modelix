package org.modelix.model.util

expect class AtomicLong(value: Long) {
    fun incrementAndGet() : Long
}

expect fun random() : Double