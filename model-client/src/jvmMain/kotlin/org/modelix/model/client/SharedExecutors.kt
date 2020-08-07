/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */

package org.modelix.model.client

import org.modelix.model.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

actual object SharedExecutors {
    private val LOG = LogManager.getLogger(SharedExecutors::class)
    actual val FIXED = object : ExecutorService {
        val wrapped = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1)
        override fun shutdown() {
            wrapped.shutdown()
        }

        override fun execute(command: Runnable) {
            wrapped.execute { command.run() }
        }
    }

    actual val SCHEDULED: org.modelix.model.util.ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1).toModelix()
    actual fun shutdownAll() {
        SCHEDULED.shutdown()
        FIXED.shutdown()
    }

    actual fun fixDelay(milliSeconds: Int, r: Runnable): org.modelix.model.util.ScheduledFuture<*> {
        return SCHEDULED.scheduleWithFixedDelay(
            Runnable
            {
                try {
                    r.run()
                } catch (ex: Exception) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("", ex)
                    }
                }
            },
            milliSeconds.toLong(), milliSeconds.toLong()
        )
    }
}

private fun ScheduledExecutorService.toModelix(): org.modelix.model.util.ScheduledExecutorService {
    val wrapped = this
    return object : org.modelix.model.util.ScheduledExecutorService {
        override fun scheduleWithFixedDelay(command: Runnable?, initialDelay: Long, delay: Long): org.modelix.model.util.ScheduledFuture<*> {
            return wrapped.scheduleWithFixedDelay(command?.toJvm(), initialDelay, delay, TimeUnit.MILLISECONDS).toModelix()
        }

        override fun shutdown() {
            wrapped.shutdown()
        }

        override fun execute(command: Runnable) {
            wrapped.execute(command.toJvm())
        }
    }
}

private fun <V> ScheduledFuture<V>.toModelix(): org.modelix.model.util.ScheduledFuture<V> {
    val wrapped = this
    return object : org.modelix.model.util.ScheduledFuture<V> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            return wrapped.cancel(mayInterruptIfRunning)
        }
    }
}

private fun Runnable.toJvm(): java.lang.Runnable {
    val wrapped = this
    return java.lang.Runnable { wrapped.run(); }
}
