package org.modelix.model.persistent

import org.modelix.model.api.IConcept
import org.modelix.model.api.INodeReference
import org.modelix.model.api.PNodeReference
import org.modelix.model.operations.*
import java.util.*

class OperationSerializer private constructor() {
    companion object {
        val INSTANCE: OperationSerializer = OperationSerializer()
        private val SEPARATOR: String = ";"
        fun serializeConcept(concept: IConcept?): String {
//    return SerializationUtil.escape(((SAbstractConceptAdapter) ((SConceptAdapter) concept).getAdapted()).serialize());
            throw UnsupportedOperationException()
        }

        fun deserializeConcept(serialized: String?): IConcept {
//    return new SConceptAdapter(SAbstractConceptAdapter.deserialize(SerializationUtil.unescape(serialized)));
            throw UnsupportedOperationException()
        }

        fun serializeReference(`object`: INodeReference?): String? {
            if (`object` == null) {
                return ""
            } else if (`object` is PNodeReference) {
                return SerializationUtil.longToHex(`object`.getId())
                //    } else if (object instanceof SNodeReferenceAdapter) {
//      return SerializationUtil.escape(SNodePointer.serialize(((SNodeReferenceAdapter) object).getReference()));
            } else {
                throw RuntimeException("Unknown reference type: " + `object`.javaClass.getName())
            }
        }

        fun deserializeReference(serialized: String?): INodeReference? {
            if ((serialized == null || serialized.length == 0)) {
                return null
            }
            if (serialized.matches("[a-fA-F0-9]+")) {
                return PNodeReference(SerializationUtil.longFromHex(serialized))
            }
            throw RuntimeException("Cannot deserialize concept: " + serialized)
        }

        init {
            INSTANCE.registerSerializer(AddNewChildOp::class.java, object : Serializer<AddNewChildOp> {
                public override fun serialize(op: AddNewChildOp): String {
                    return SerializationUtil.longToHex(op.parentId) + SEPARATOR + SerializationUtil.escape(op.role) + SEPARATOR + op.index + SEPARATOR + SerializationUtil.longToHex(op.childId) + SEPARATOR + serializeConcept(op.concept)
                }

                public override fun deserialize(serialized: String): AddNewChildOp {
                    val parts: Array<String> = serialized.split(SEPARATOR).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    return AddNewChildOp(SerializationUtil.longFromHex(parts.get(0)), SerializationUtil.unescape(parts.get(1)), parts.get(2).toInt(), SerializationUtil.longFromHex(parts.get(3)), deserializeConcept(parts.get(4)))
                }
            })
            INSTANCE.registerSerializer(DeleteNodeOp::class.java, object : Serializer<DeleteNodeOp> {
                public override fun serialize(op: DeleteNodeOp): String {
                    return SerializationUtil.longToHex(op.parentId) + SEPARATOR + SerializationUtil.escape(op.role) + SEPARATOR + op.index + SEPARATOR + SerializationUtil.longToHex(op.childId)
                }

                public override fun deserialize(serialized: String): DeleteNodeOp {
                    val parts: Array<String> = serialized.split(SEPARATOR).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    return DeleteNodeOp(SerializationUtil.longFromHex(parts.get(0)), SerializationUtil.unescape(parts.get(1)), parts.get(2).toInt(), SerializationUtil.longFromHex(parts.get(3)))
                }
            })
            INSTANCE.registerSerializer(MoveNodeOp::class.java, object : Serializer<MoveNodeOp> {
                public override fun serialize(op: MoveNodeOp): String {
                    return SerializationUtil.longToHex(op.childId) + SEPARATOR + SerializationUtil.longToHex(op.sourceParentId) + SEPARATOR + SerializationUtil.escape(op.sourceRole) + SEPARATOR + op.sourceIndex + SEPARATOR + SerializationUtil.longToHex(op.targetParentId) + SEPARATOR + SerializationUtil.escape(op.targetRole) + SEPARATOR + op.targetIndex
                }

                public override fun deserialize(serialized: String): MoveNodeOp {
                    val parts: Array<String> = serialized.split(SEPARATOR).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    return MoveNodeOp(SerializationUtil.longFromHex(parts.get(0)), SerializationUtil.longFromHex(parts.get(1)), SerializationUtil.unescape(parts.get(2)), parts.get(3).toInt(), SerializationUtil.longFromHex(parts.get(4)), SerializationUtil.unescape(parts.get(5)), parts.get(6).toInt())
                }
            })
            INSTANCE.registerSerializer<NoOp>(NoOp::class.java, object : Serializer<NoOp?> {
                public override fun serialize(op: NoOp?): String {
                    return ""
                }

                public override fun deserialize(serialized: String): NoOp? {
                    return NoOp()
                }
            })
            INSTANCE.registerSerializer(SetPropertyOp::class.java, object : Serializer<SetPropertyOp> {
                public override fun serialize(op: SetPropertyOp): String {
                    return SerializationUtil.longToHex(op.nodeId) + SEPARATOR + SerializationUtil.escape(op.role) + SEPARATOR + SerializationUtil.escape(op.value)
                }

                public override fun deserialize(serialized: String): SetPropertyOp {
                    val parts: Array<String> = serialized.split(SEPARATOR).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    return SetPropertyOp(SerializationUtil.longFromHex(parts.get(0)), SerializationUtil.unescape(parts.get(1)), SerializationUtil.unescape(parts.get(2)))
                }
            })
            INSTANCE.registerSerializer(SetReferenceOp::class.java, object : Serializer<SetReferenceOp> {
                public override fun serialize(op: SetReferenceOp): String {
                    return SerializationUtil.longToHex(op.sourceId) + SEPARATOR + SerializationUtil.escape(op.role) + SEPARATOR + serializeReference(op.target)
                }

                public override fun deserialize(serialized: String): SetReferenceOp {
                    val parts: Array<String> = serialized.split(SEPARATOR).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    return SetReferenceOp(SerializationUtil.longFromHex(parts.get(0)), SerializationUtil.unescape(parts.get(1)), deserializeReference(parts.get(2)))
                }
            })
        }
    }

    private val serializers: MutableMap<Class<out IOperation>, Serializer<*>> = HashMap()
    private val deserializers: MutableMap<String, Serializer<*>> = HashMap()
    fun <T : IOperation?> registerSerializer(type: Class<T>, serializer: Serializer<T>) {
        serializers.put(type, serializer)
        deserializers.put(type.getSimpleName(), serializer)
    }

    fun serialize(op: IOperation): String {
        val serializer: Serializer<*>? = serializers.get(op.javaClass)
        if (serializer == null) {
            throw RuntimeException("Unknown operation type: " + op.javaClass.getSimpleName())
        }
        return op.javaClass.getSimpleName() + SEPARATOR + serializer.serialize(op)
    }

    fun deserialize(serialized: String): IOperation {
        val parts: Array<String> = serialized.split(SEPARATOR, 2.toBoolean()).toTypedArray()
        val deserializer: Serializer<*>? = deserializers.get(parts.get(0))
        if (deserializer == null) {
            throw RuntimeException("Unknown operation type: " + parts.get(0))
        }
        return (deserializer.deserialize(parts.get(1)))!!
    }

    open interface Serializer<E : IOperation?> {
        fun serialize(op: E): String
        fun deserialize(serialized: String): E
    }
}