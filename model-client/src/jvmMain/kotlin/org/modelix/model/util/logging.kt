package org.modelix.model.util

import kotlin.reflect.KClass

fun Level.toLog4jLevel(): org.apache.log4j.Level {
    return when (this) {
        Level.ERROR -> org.apache.log4j.Level.ERROR
        Level.DEBUG -> org.apache.log4j.Level.DEBUG
        Level.TRACE -> org.apache.log4j.Level.TRACE
        Level.WARN -> org.apache.log4j.Level.WARN
    }
}

actual object LogManager {
    actual fun getLogger(clazz: KClass<*>): Logger {
        return object : Logger {
            private val log4jInstance = org.apache.log4j.Logger.getLogger(clazz.java)

            override fun isEnabledFor(level: Level): Boolean {
                return log4jInstance.isEnabledFor(level.toLog4jLevel())
            }

            override fun error(message: String, exception: Throwable?) {
                return log4jInstance.error(message, exception)
            }

            override fun debug(message: String) {
                return log4jInstance.debug(message)
            }

            override fun trace(message: String) {
                return log4jInstance.trace(message)
            }

            override fun warn(message: String, exception: Throwable?) {
                return log4jInstance.warn(message, exception)
            }
        }
    }
}
