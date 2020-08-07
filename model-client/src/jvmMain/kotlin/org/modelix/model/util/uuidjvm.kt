package org.modelix.model.util

actual fun randomUUID(): UUID {
    return java.util.UUID.randomUUID().toModelix()
}

private fun java.util.UUID.toModelix() : UUID {
    return UUID(this.toString())
}
