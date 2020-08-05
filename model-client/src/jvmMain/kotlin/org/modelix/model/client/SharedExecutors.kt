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

import org.modelix.model.util.Level
import org.modelix.model.util.LogManager
import org.modelix.model.util.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

actual object SharedExecutors {
    private val LOG = LogManager.getLogger(SharedExecutors::class)
    val FIXED = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1)
    val SCHEDULED = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1)
    fun shutdownAll() {
        SCHEDULED.shutdown()
        FIXED.shutdown()
    }

    fun fixDelay(milliSeconds: Int, r: Runnable): ScheduledFuture<*> {
        return SCHEDULED.scheduleWithFixedDelay(
            {
                try {
                    r.run()
                } catch (ex: Exception) {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("", ex)
                    }
                }
            },
            milliSeconds.toLong(), milliSeconds.toLong(), TimeUnit.MILLISECONDS
        )
    }
}
