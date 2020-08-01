package org.modelix.model.client

import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import java.util.concurrent.*

object SharedExecutors {
    private val LOG: Logger = LogManager.getLogger(SharedExecutors::class.java)
    val FIXED: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1)
    val SCHEDULED: ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1)
    fun shutdownAll() {
        SCHEDULED.shutdown()
        FIXED.shutdown()
    }

    fun fixDelay(milliSeconds: Int, r: Runnable): ScheduledFuture<*> {
        return SCHEDULED.scheduleWithFixedDelay(Runnable({
            try {
                r.run()
            } catch (ex: Exception) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("", ex)
                }
            }
        }), milliSeconds.toLong(), milliSeconds.toLong(), TimeUnit.MILLISECONDS)
    }
}