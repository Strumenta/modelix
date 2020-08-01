package org.modelix.model.persistent

abstract class CPElementRef  /*package*/
internal constructor() {
    abstract val isGLobal: Boolean
    abstract val isLocal: Boolean
    abstract val elementId: Long
    abstract val treeId: String?

    private class LocalRef(private val id: Long) : CPElementRef() {
        public override fun toString(): String {
            return "" + id
        }

        public override fun isGLobal(): Boolean {
            return false
        }

        public override fun isLocal(): Boolean {
            return true
        }

        public override fun getElementId(): Long {
            return id
        }

        public override fun getTreeId(): String? {
            throw RuntimeException("Local reference")
        }

        public override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o == null || this.javaClass != o.javaClass) {
                return false
            }
            val that: LocalRef = o as LocalRef
            if (id != that.id) {
                return false
            }
            return true
        }

        public override fun hashCode(): Int {
            var result: Int = 0
            result = 31 * result + (id xor (id shr 32)).toInt()
            return result
        }

    }

    private class GlobalRef(treeId1: String, elementId1: Long) : CPElementRef() {
        override val treeId: String?
        override val elementId: Long
        public override fun toString(): String {
            return "G" + treeId + "#" + elementId
        }

        public override fun isGLobal(): Boolean {
            return true
        }

        public override fun isLocal(): Boolean {
            return false
        }

        public override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o == null || this.javaClass != o.javaClass) {
                return false
            }
            val that: GlobalRef = o as GlobalRef
            if (elementId != that.elementId) {
                return false
            }
            if ((if (treeId != null) !(((treeId as Any) == that.treeId)) else that.treeId != null)) {
                return false
            }
            return true
        }

        public override fun hashCode(): Int {
            var result: Int = 0
            result = 31 * result + (elementId xor (elementId shr 32)).toInt()
            result = 31 * result + ((if (treeId != null) treeId.toString().hashCode() else 0))
            return result
        }

        init {
            treeId = treeId1
            elementId = elementId1
        }
    }

    class MpsRef(ref: String) : CPElementRef() {
        val serializedRef: String?

        public override fun toString(): String {
            return "M" + serializedRef
        }

        public override fun isGLobal(): Boolean {
            return false
        }

        public override fun isLocal(): Boolean {
            return false
        }

        public override fun getElementId(): Long {
            throw RuntimeException("MPS reference")
        }

        public override fun getTreeId(): String? {
            throw RuntimeException("MPS reference")
        }

        public override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o == null || this.javaClass != o.javaClass) {
                return false
            }
            val that: MpsRef = o as MpsRef
            if ((if (serializedRef != null) !(((serializedRef as Any) == that.serializedRef)) else that.serializedRef != null)) {
                return false
            }
            return true
        }

        public override fun hashCode(): Int {
            var result: Int = 0
            result = 31 * result + ((if (serializedRef != null) serializedRef.toString().hashCode() else 0))
            return result
        }

        init {
            serializedRef = ref
        }
    }

    companion object {
        fun local(elementId: Long): CPElementRef {
            return LocalRef(elementId)
        }

        fun global(treeId: String, elementId: Long): CPElementRef {
            return GlobalRef(treeId, elementId)
        }

        fun mps(pointer: String): CPElementRef {
            return MpsRef(pointer)
        }

        fun fromString(str: String?): CPElementRef {
            if (str!!.get(0) == 'G') {
                val i: Int = str.lastIndexOf("#")
                return global(str.substring(1, i), java.lang.Long.valueOf(str.substring(i + 1)))
            } else if (str.get(0) == 'M') {
                return mps(str.substring(1))
            } else {
                return local(java.lang.Long.valueOf(str))
            }
        }
    }
}