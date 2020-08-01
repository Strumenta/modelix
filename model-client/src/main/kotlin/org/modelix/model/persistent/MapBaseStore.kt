package org.modelix.model.persistent

import gnu.trove.map.hash.THashMap
import org.modelix.model.IKeyListener
import org.modelix.model.IKeyValueStore
import java.util.*

class MapBaseStore constructor() : IKeyValueStore {
    private val map: MutableMap<String?, String?> = THashMap()
    public override fun get(key: String?): String? {
        return map.get(key)
    }

    public override fun put(key: String?, value: String?) {
        map.put(key, value)
    }

    public override fun getAll(keys: Iterable<String?>?): Map<String?, String?>? {
        val result: MutableMap<String?, String?> = LinkedHashMap(16, 0.75.toFloat(), false)
        for (key: String? in keys!!) {
            result.put(key, map.get(key))
        }
        return result
    }

    public override fun putAll(entries: Map<String?, String?>?) {
        map.putAll((entries)!!)
    }

    public override fun prefetch(key: String?) {}
    val entries: Iterable<Map.Entry<String?, String?>>
        get() {
            return map.entries
        }

    public override fun listen(key: String?, listener: IKeyListener?) {
        throw UnsupportedOperationException()
    }

    public override fun removeListener(key: String?, listener: IKeyListener?) {
        throw UnsupportedOperationException()
    }
}