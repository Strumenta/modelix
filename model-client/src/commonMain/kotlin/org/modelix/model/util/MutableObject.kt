package org.modelix.model.util

/**
 * A mutable `Object` wrapper.
 *
 * @param <T> the type to set and get
 */
class MutableObject<T> {
    /** The mutable value.  */
    private var value: T? = null

    /**
     * Constructs a new MutableObject with the default value of `null`.
     */
    constructor() : super() {}

    /**
     * Constructs a new MutableObject with the specified value.
     *
     * @param value  the initial value to store
     */
    constructor(value: T) : super() {
        this.value = value
    }

    /**
     * Gets the value.
     *
     * @return the value, may be null
     */
    fun getValue(): T? {
        return value
    }

    /**
     * Sets the value.
     *
     * @param value  the value to set
     */
    fun setValue(value: T) {
        this.value = value
    }
    // -----------------------------------------------------------------------
    /**
     *
     *
     * Compares this object against the specified object. The result is `true` if and only if the argument
     * is not `null` and is a `MutableObject` object that contains the same `T`
     * value as this object.
     *
     *
     * @param obj  the object to compare with, `null` returns `false`
     * @return `true` if the objects are the same;
     * `true` if the objects have equivalent `value` fields;
     * `false` otherwise.
     */
    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (this === obj) {
            return true
        }
        if (obj is MutableObject<*>) {
            return value == obj.value
        }
        return false
    }

    /**
     * Returns the value's hash code or `0` if the value is `null`.
     *
     * @return the value's hash code or `0` if the value is `null`.
     */
    override fun hashCode(): Int {
        return if (value == null) 0 else value.hashCode()
    }
    // -----------------------------------------------------------------------
    /**
     * Returns the String value of this mutable.
     *
     * @return the mutable value as a string
     */
    override fun toString(): String {
        return if (value == null) "null" else value.toString()
    }
}

class MutableInt(var value: Int) {
    fun decrement() {
        value--
    }

    fun toInt(): Int = value
}
