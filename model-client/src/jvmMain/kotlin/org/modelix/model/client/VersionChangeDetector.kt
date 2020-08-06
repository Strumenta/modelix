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

import org.modelix.model.IKeyListener
import org.modelix.model.IKeyValueStore
import org.modelix.model.client.SharedExecutors.fixDelay
import org.modelix.model.util.Level
import org.modelix.model.util.LogManager
import org.modelix.model.util.Runnable
import java.util.concurrent.ScheduledFuture

actual abstract class VersionChangeDetector actual constructor(private val store: IKeyValueStore, private val key: String) {
    private val keyListener: IKeyListener
    var lastVersionHash: String? = null
        private set
    private val pollingTask: org.modelix.model.util.ScheduledFuture<*>
    private fun versionChanged(newVersion: String?) {
        if (newVersion == lastVersionHash) {
            return
        }
        try {
            processVersionChange(lastVersionHash, newVersion)
        } catch (ex: Exception) {
            if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("", ex)
            }
        }
        lastVersionHash = newVersion
    }

    protected actual abstract fun processVersionChange(oldVersion: String?, newVersion: String?)
    actual fun dispose() {
        pollingTask.cancel(false)
        store.removeListener(key, keyListener)
    }

    companion object {
        private val LOG = LogManager.getLogger(VersionChangeDetector::class)
    }

    init {
        keyListener = object : IKeyListener {
            override fun changed(key: String?, versionHash: String?) {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Listener received new version $versionHash")
                }
                versionChanged(versionHash)
            }
        }
        SharedExecutors.FIXED.execute(Runnable { store.listen(key, keyListener) })
        pollingTask = fixDelay(
            3000,
            object : Runnable {
                override fun run() {
                    val version = store[key]
                    if (version == lastVersionHash) {
                        return
                    }
                    if (LOG.isDebugEnabled) {
                        LOG.debug("New version detected by polling: $version")
                    }
                    versionChanged(version)
                }
            }
        )
    }
}
