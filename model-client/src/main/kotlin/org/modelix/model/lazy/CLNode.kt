package org.modelix.model.lazy

import org.modelix.model.persistent.CPElementRef
import org.modelix.model.persistent.CPNode
import java.util.function.Function
import java.util.stream.Stream
import java.util.stream.StreamSupport

class CLNode(tree: CLTree, data: CPNode) : CLElement(tree, data) {
    constructor(tree: CLTree, id: Long, concept: String?, parentId: Long, roleInParent: String?, childrenIds: LongArray?, propertyRoles: Array<String?>?, propertyValues: Array<String?>?, referenceRoles: Array<String?>?, referenceTargets: Array<CPElementRef?>?) : this(tree, CPNode.Companion.create(id, concept, parentId, roleInParent, childrenIds, propertyRoles, propertyValues, referenceRoles, referenceTargets)) {}

    override fun getData(): CPNode? {
        return super.getData() as CPNode
    }

    fun getChildren(bulkQuery: IBulkQuery): IBulkQuery.Value<Iterable<CLNode?>?>? {
        return tree.resolveElements(getData()!!.childrenIds, bulkQuery)!!.map { elements: List<CLNode?>? -> elements }
    }

    fun getDescendants(bulkQuery: IBulkQuery, includeSelf: Boolean): IBulkQuery.Value<Iterable<CLNode>>? {
        return if (includeSelf) {
            getDescendants(bulkQuery, false)
                    .map(Function { descendants: Iterable<CLNode?> ->
                        Iterable<CLNode> {
                            Stream.concat(
                                    Stream.of(this),
                                    StreamSupport.stream(descendants.spliterator(), false)
                            ).iterator()
                        }
                    })
        } else {
            getChildren(bulkQuery)!!.mapBulk { children: Iterable<CLNode?>? ->
                val d = bulkQuery
                        .map<CLNode?, Iterable<CLNode?>>(children, Function<CLNode?, IBulkQuery.Value<Iterable<CLNode>>?> { child: CLNode? -> child!!.getDescendants(bulkQuery, true) })
                        .map { it: List<Iterable<CLNode?>>? -> Iterable<CLNode> { it!!.stream().flatMap { n: Iterable<CLNode?> -> StreamSupport.stream(n.spliterator(), false) }.iterator() } }
                d
            }
        }
    }

    val concept: String?
        get() = getData().getConcept()
}