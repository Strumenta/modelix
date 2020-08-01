package org.modelix.model.util.pmap

import org.modelix.model.util.StreamUtils
import java.util.stream.Collectors
import java.util.stream.Stream

abstract class SmallPMap<K, V> : CustomPMap<K, V> {
    abstract override fun get(key: K): V
    abstract override fun put(key: K, value: V): SmallPMap<K, V>?
    abstract override fun remove(key: K): SmallPMap<K, V>?
    abstract override fun keys(): Iterable<K>?
    abstract override fun values(): Iterable<V>?
    class Single<K, V>(private val key: K, private val value: V) : SmallPMap<K, V?>() {
        override fun get(key: K): V? {
            return if (this.key == key) value else null
        }

        override fun keys(): Iterable<K>? {
            return setOf(key)
        }

        override fun put(key: K, value: V?): SmallPMap<K, V?>? {
            if (value == null) {
                return remove(key)
            }
            return if (key == this.key) {
                if (value == this.value) {
                    this
                } else {
                    Single<K, V?>(key, value)
                }
            } else create(arrayOf(this.key, key), arrayOf(this.value, value))
        }

        override fun remove(key: K): SmallPMap<K, V?>? {
            return if (key == this.key) EMPTY else this
        }

        override fun values(): Iterable<V?>? {
            return setOf(value)
        }

        override fun containsKey(key: K): Boolean {
            return key == this.key
        }

    }

    class Multiple<K, V>(private val keys: Array<Any?>?, private val values: Array<Any?>?) : SmallPMap<K, V?>() {
        override fun get(key: K): V? {
            for (i in keys!!.indices) {
                if (keys[i] == key) {
                    return values!![i] as V?
                }
            }
            return null
        }

        override fun put(key: K, value: V?): SmallPMap<K, V?>? {
            if (value == null) {
                return remove(key)
            }
            val index = COWArrays.indexOf(keys, key)
            return if (index != -1) {
                if (value == values!![index]) {
                    this
                } else {
                    create(keys, COWArrays.set(values, index, value))
                }
            } else {
                create(COWArrays.add(keys, key), COWArrays.add(values, value))
            }
        }

        override fun remove(key: K): SmallPMap<K, V?>? {
            val index = COWArrays.indexOf(keys, key)
            return if (index != -1) {
                create(COWArrays.removeAt(keys, index), COWArrays.removeAt(values, index))
            } else {
                this
            }
        }

        override fun keys(): Iterable<K>? {
            return Stream.of(*keys).map { it: Any? -> it as K? }.collect(Collectors.toList())
        }

        override fun values(): Iterable<V?>? {
            return Stream.of(*values).map { it: Any? -> it as V? }.collect(Collectors.toList())
        }

        override fun containsKey(key: K): Boolean {
            for (k in keys!!) {
                if (k == key) {
                    return true
                }
            }
            return false
        }

    }

    companion object {
        private val EMPTY: SmallPMap<*, *> = Multiple<Any?, Any?>(arrayOfNulls(0), arrayOfNulls(0))
        fun <K, V> empty(): SmallPMap<K, V> {
            return EMPTY
        }

        private fun <K, V> create(keys: Array<Any?>?, values: Array<Any?>?): SmallPMap<K?, V?> {
            if (keys!!.size == 0) {
                return empty()
            }
            return if (keys.size == 1) {
                Single(keys[0] as K?, values!![0] as V?)
            } else Multiple<K?, V?>(keys, values)
        }

        fun <K, V> createS(keys: Iterable<K>?, values: Iterable<V>?): SmallPMap<K, V> {
            return create(
                    StreamUtils.toStream(keys).toArray { _Dummy_.__Array__() },
                    StreamUtils.toStream(values).toArray { _Dummy_.__Array__() })
        }
    }
}