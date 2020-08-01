package org.modelix.model.client

import org.modelix.model.api.IIdGenerator
import java.util.concurrent.atomic.AtomicLong

class IdGenerator constructor(clientId: Int) : IIdGenerator {
    protected val idSequence: AtomicLong
    private val clientId: Long
    public override fun generate(): Long {
        val id: Long = idSequence.incrementAndGet()
        if (id ushr 32 != clientId) {
            throw RuntimeException("End of ID range")
        }
        return id
    }

    init {
        this.clientId = clientId.toLong()
        idSequence = AtomicLong(this.clientId shl 32)
    }
}