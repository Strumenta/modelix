package org.modelix.model.persistent

open interface IPersistable {
    fun calcHash(): String?
    fun serialize()
}