package org.modelix.model

import org.modelix.model.api.IIdGenerator
import org.modelix.model.lazy.CLVersion
import org.modelix.model.lazy.IDeserializingKeyValueStore

expect class VersionMerger(storeCache: IDeserializingKeyValueStore, idGenerator: IIdGenerator) {
    fun mergeChange(lastMergedVersion: CLVersion, newVersion: CLVersion): CLVersion
}
