package org.modelix.model.client

import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.modelix.model.IKeyListener
import org.modelix.model.IKeyValueStore
import java.util.*
import java.util.concurrent.ScheduledFuture

abstract class VersionChangeDetector constructor(private val store: IKeyValueStore, private val key: String?) {
    private val keyListener: IKeyListener
    var lastVersionHash: String? = null
        private set
    private val pollingTask: ScheduledFuture<*>?
    private fun versionChanged(newVersion: String?) {
        if (Objects.equals(newVersion, lastVersionHash)) {
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

    protected abstract fun processVersionChange(oldVersion: String?, newVersion: String?)
    fun dispose() {
        pollingTask!!.cancel(false)
        store.removeListener(key, keyListener)
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger(VersionChangeDetector::class.java)
    }

    init {
        keyListener = object : IKeyListener {
            public override fun changed(key: String?, versionHash: String?) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Listener received new version " + versionHash)
                }
                versionChanged(versionHash)
            }
        }
        SharedExecutors.FIXED.execute(object : Runnable {
            public override fun run() {
                store.listen(key, keyListener)
            }
        })
        pollingTask = SharedExecutors.fixDelay(3000, object : Runnable {
            public override fun run() {
                val version: String? = store.get(key)
                if (Objects.equals(version, lastVersionHash)) {
                    return
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("New version detected by polling: " + version)
                }
                versionChanged(version)
            }
        })
    }
}