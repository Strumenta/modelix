package org.modelix.model.util

expect class AtomicLong(value: Long) {
    fun incrementAndGet() : Long
}

expect class AtomicBoolean(value: Boolean) {
    constructor()
    fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean
    fun set(newValue: Boolean)
}

expect fun random() : Double

expect fun sleep(ms: Long)

interface Executor {
    fun execute(command: Runnable)
}

interface ExecutorService : Executor {

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     *
     * This method does not wait for previously submitted tasks to
     * complete execution.  Use [awaitTermination][.awaitTermination]
     * to do that.
     */
    fun shutdown()
}