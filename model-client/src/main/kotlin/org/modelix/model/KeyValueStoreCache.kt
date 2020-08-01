package org.modelix.model

import org.apache.commons.collections4.map.LRUMap
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.modelix.model.persistent.HashUtil
import org.modelix.model.util.StreamUtils
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Predicate
import java.util.stream.Collectors

class KeyValueStoreCache constructor(private val store: IKeyValueStore) : IKeyValueStore {
    private val cache: MutableMap<String?, String?> = Collections.synchronizedMap(LRUMap(300000))
    private val pendingPrefetches: MutableSet<String> = HashSet()
    private val activeRequests: MutableList<GetRequest> = ArrayList()
    public override fun prefetch(rootKey: String) {
        val processedKeys: MutableSet<String?> = HashSet()
        processedKeys.add(rootKey)
        var newKeys: MutableList<String> = Arrays.asList(rootKey)
        while (!newKeys.isEmpty() && processedKeys.size + newKeys.size <= 100000) {
            synchronized(pendingPrefetches, { newKeys.removeAll(pendingPrefetches) })
            val currentKeys: List<String?> = newKeys
            newKeys = ArrayList()
            var loadedEntries: Map<String?, String?>?
            synchronized(pendingPrefetches, { pendingPrefetches.addAll(currentKeys) })
            try {
                loadedEntries = getAll(currentKeys)
                for (entry: Map.Entry<String?, String?> in loadedEntries!!.entries) {
                    processedKeys.add(entry.key)
                    for (childKey: String? in HashUtil.extractSha256(entry.value)) {
                        if (processedKeys.contains(childKey)) {
                            continue
                        }
                        newKeys.add(childKey)
                    }
                }
            } finally {
                pendingPrefetches.removeAll(currentKeys)
            }
        }
    }

    public override fun get(key: String?): String? {
        return getAll(setOf(key))!!.get(key)
    }

    public override fun getAll(keys_: Iterable<String?>?): Map<String?, String?>? {
        val remainingKeys: MutableList<String?> = StreamUtils.toStream(keys_).collect(Collectors.toList())
        val result: MutableMap<String?, String?> = LinkedHashMap(16, 0.75.toFloat(), false)
        synchronized(cache, {
            val itr: MutableIterator<String?> = remainingKeys.iterator()
            while (itr.hasNext()) {
                val key: String? = itr.next()
                val value: String? = cache.get(key)
                // always put even if null to have the same order in the linked hash map as in the input 
                result.put(key, value)
                if (value != null) {
                    itr.remove()
                }
            }
        })
        if (!remainingKeys.isEmpty()) {
            val requiredRequest: MutableList<GetRequest> = ArrayList()
            var newRequest: GetRequest? = null
            synchronized(activeRequests, {
                for (r: GetRequest in activeRequests) {
                    if (remainingKeys.stream().anyMatch(Predicate({ o: String? -> r.keys.contains(o) }))) {
                        if (LOG.isDebugEnabled()) {
                            val intersection: Set<String?>? = StreamUtils.intersection(remainingKeys.stream(), r.keys)
                            LOG.debug("Reusing an active request: " + intersection!!.stream().findFirst().orElse(null) + " (" + intersection.size + ")")
                        }
                        requiredRequest.add(r)
                        remainingKeys.removeAll(r.keys)
                    }
                }
                if (!remainingKeys.isEmpty()) {
                    newRequest = GetRequest(HashSet(remainingKeys))
                    requiredRequest.add(newRequest!!)
                    activeRequests.add(newRequest!!)
                }
            })
            if (newRequest != null) {
                try {
                    newRequest!!.execute()
                } finally {
                    synchronized(activeRequests, { activeRequests.remove(newRequest!!) })
                }
            }
            for (req: GetRequest in requiredRequest) {
                val reqResult: Map<String?, String?> = req.waitForResult()
                for (entry: Map.Entry<String?, String?> in reqResult.entries) {
                    if (result.containsKey(entry.key)) {
                        result.put(entry.key, entry.value)
                    }
                }
            }
        }
        return result
    }

    public override fun listen(key: String?, listener: IKeyListener?) {
        store.listen(key, listener)
    }

    public override fun put(key: String?, value: String?) {
        cache.put(key, value)
        store.put(key, value)
    }

    public override fun putAll(entries: Map<String?, String?>?) {
        entries!!.forEach(BiConsumer({ key: String?, value: String? -> cache.put(key, value) }))
        store.putAll(entries)
    }

    public override fun removeListener(key: String?, listener: IKeyListener?) {
        store.removeListener(key, listener)
    }

    private inner class GetRequest constructor(val keys: Set<String?>) {
        private var result: Map<String?, String?>? = null
        private var exception: Exception? = null
        fun execute() {
            try {
                val entriesFromStore: Map<String?, String?>? = store.getAll(keys)
                for (entry: Map.Entry<String?, String?> in entriesFromStore!!.entries) {
                    cache.put(entry.key, entry.value)
                }
                putResult(entriesFromStore)
            } catch (ex: Exception) {
                putException(ex)
            }
        }

        @Synchronized
        fun putException(ex: Exception?) {
            exception = ex
            this.notifyAll()
        }

        @Synchronized
        fun putResult(result: Map<String?, String?>?) {
            this.result = result
            this.notifyAll()
        }

        @Synchronized
        fun waitForResult(): Map<String?, String?> {
            while (result == null && exception == null) {
                try {
                    this.wait()
                } catch (ex: InterruptedException) {
                    throw RuntimeException()
                }
            }
            if (result != null) {
                return result
            } else {
                throw RuntimeException(exception)
            }
        }

    }

    companion object {
        private val LOG: Logger = LogManager.getLogger(KeyValueStoreCache::class.java)
    }

}