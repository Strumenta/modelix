package org.modelix.model.util

expect class AtomicLong(value: Long) {
    fun incrementAndGet(): Long
}

expect class AtomicBoolean(value: Boolean) {
    constructor()
    fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean
    fun set(newValue: Boolean)
    fun get(): Boolean
}

expect fun random(): Double

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

interface ScheduledFuture<V> {
    fun cancel(mayInterruptIfRunning: Boolean): Boolean
}

interface ScheduledExecutorService : ExecutorService {

    /**
     * Submits a periodic action that becomes enabled first after the
     * given initial delay, and subsequently with the given delay
     * between the termination of one execution and the commencement of
     * the next.
     *
     *
     * The sequence of task executions continues indefinitely until
     * one of the following exceptional completions occur:
     *
     *  * The task is [explicitly cancelled][Future.cancel]
     * via the returned future.
     *  * The executor terminates, also resulting in task cancellation.
     *  * An execution of the task throws an exception.  In this case
     * calling [get][Future.get] on the returned future will throw
     * [ExecutionException], holding the exception as its cause.
     *
     * Subsequent executions are suppressed.  Subsequent calls to
     * [isDone()][Future.isDone] on the returned future will
     * return `true`.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay in milliseconds between the termination of one
     * execution and the commencement of the next
     * the series of repeated tasks.  The future's [         ][Future.get] method will never return normally,
     * and will throw an exception upon task cancellation or
     * abnormal termination of a task execution.
     * @throws RejectedExecutionException if the task cannot be
     * scheduled for execution
     * @throws NullPointerException if command or unit is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    fun scheduleWithFixedDelay(
        command: Runnable?,
        initialDelay: Long,
        delay: Long
    ): ScheduledFuture<*>
}
