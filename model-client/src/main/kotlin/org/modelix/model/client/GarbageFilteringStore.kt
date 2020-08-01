package org.modelix.model.client

import org.modelix.model.IKeyListener
import org.modelix.model.IKeyValueStore
import org.modelix.model.persistent.HashUtil
import org.modelix.model.util.StreamUtils
import java.util.*
import java.util.stream.Collectors

class GarbageFilteringStore constructor(private val store: IKeyValueStore) : IKeyValueStore {
    private val pendingEntries: MutableMap<String?, String?> = HashMap()
    public override fun get(key: String?): String? {
        return (if (pendingEntries.containsKey(key)) pendingEntries.get(key) else store.get(key))
    }

    public override fun put(key: String?, value: String?) {
        putAll(Collections.singletonMap(key, value))
    }

    public override fun getAll(keys_: Iterable<String?>?): Map<String?, String?>? {
        val keys: MutableList<String?> = StreamUtils.toStream(keys_).collect(Collectors.toList())
        val result: MutableMap<String?, String?> = LinkedHashMap(16, 0.75.toFloat(), false)
        synchronized(pendingEntries, {
            val itr: MutableIterator<String?> = keys.iterator()
            while (itr.hasNext()) {
                val key: String? = itr.next()
                // always put even if null to have the same order in the linked hash map as in the input 
                result.put(key, pendingEntries.get(key))
                if (pendingEntries.containsKey(key)) {
                    itr.remove()
                }
            }
        })
        if (!keys.isEmpty()) {
            result.putAll((store.getAll(keys))!!)
        }
        return result
    }

    public override fun putAll(entries: Map<String?, String?>?) {
        val entriesToWrite: MutableMap<String?, String?> = LinkedHashMap(16, 0.75.toFloat(), false)
        for (entry: Map.Entry<String?, String?> in entries!!.entries) {
            if (HashUtil.isSha256(entry.key)) {
                pendingEntries.put(entry.key, entry.value)
            } else {
                collectDependencies(entry.key, entry.value, entriesToWrite)
            }
        }
        if (!entriesToWrite.isEmpty()) {
            val entry: Optional<MutableMap.MutableEntry<String?, String?>> = entriesToWrite.entries.stream().findFirst()
            if (entry.isPresent()) {
                store.put(entry.get().key, entry.get().value)
            } else {
                store.putAll(entriesToWrite)
            }
        }
    }

    protected fun collectDependencies(key: String?, value: String?, acc: MutableMap<String?, String?>) {
        for (depKey: String? in HashUtil.extractSha256(value)) {
            if (pendingEntries.containsKey(depKey)) {
                val depValue: String? = pendingEntries.remove(depKey)
                collectDependencies(depKey, depValue, acc)
            }
        }
        acc.put(key, value)
    }

    public override fun prefetch(key: String) {
        store.prefetch(key)
    }

    public override fun listen(key: String?, listener: IKeyListener?) {
        store.listen(key, listener)
    }

    public override fun removeListener(key: String?, listener: IKeyListener?) {
        store.removeListener(key, listener)
    }

}