package org.modelix.model.util

import kotlin.reflect.KClass

interface Logger {
    fun isEnabledFor(level: Level) : Boolean
    val isDebugEnabled : Boolean
            get() = isEnabledFor(Level.DEBUG)
    val isTraceEnabled : Boolean
        get() = isEnabledFor(Level.TRACE)
    fun error(message: String, exception: Throwable? = null)
    fun debug(message: String)
    fun trace(message: String)
    fun warn(message: String, exception: Throwable? = null)
}

enum class Level {
    DEBUG,
    TRACE,
    WARN,
    ERROR
}

expect object LogManager {
    fun getLogger(clazz: KClass<*>) : Logger
}