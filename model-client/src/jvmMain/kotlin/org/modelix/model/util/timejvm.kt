package org.modelix.model.util

import java.time.LocalDateTime

actual fun nowString(): String = LocalDateTime.now().toString()
