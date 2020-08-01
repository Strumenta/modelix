package org.modelix.model.operations

import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.modelix.model.api.*
import org.modelix.model.operations.OTWriteTransaction
import org.modelix.model.util.StreamUtils
import java.util.stream.LongStream

class OTWriteTransaction constructor(private val transaction: IWriteTransaction?, private val otBranch: OTBranch, protected var idGenerator: IIdGenerator?) : IWriteTransaction {
    protected fun apply(op: IOperation) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("apply: " + op)
        }
        val appliedOp: IAppliedOperation? = op.apply(transaction)
        otBranch.operationApplied(appliedOp)
    }

    public override fun moveChild(newParentId: Long, newRole: String?, newIndex: Int, childId: Long) {
        var newIndex: Int = newIndex
        val oldparent: Long = getParent(childId)
        val oldRole: String? = getRole(childId)
        val oldIndex: Int = StreamUtils.indexOf(getChildren(oldparent, oldRole), childId)
        if (newIndex == -1) {
            newIndex = getChildren(newParentId, newRole)!!.count().toInt()
        }
        apply(MoveNodeOp(childId, oldparent, oldRole, oldIndex, newParentId, newRole, newIndex))
    }

    public override fun setProperty(nodeId: Long, role: String?, value: String?) {
        apply(SetPropertyOp(nodeId, role, value))
    }

    public override fun setReferenceTarget(sourceId: Long, role: String?, target: INodeReference?) {
        apply(SetReferenceOp(sourceId, role, target))
    }

    public override fun addNewChild(parentId: Long, role: String?, index: Int, childId: Long, concept: IConcept?) {
        var index: Int = index
        if (index == -1) {
            index = getChildren(parentId, role)!!.count().toInt()
        }
        apply(AddNewChildOp(parentId, role, index, childId, concept))
    }

    public override fun deleteNode(nodeId: Long) {
        val parent: Long = getParent(nodeId)
        val role: String? = getRole(nodeId)
        val index: Int = StreamUtils.indexOf(getChildren(parent, role), nodeId)
        apply(DeleteNodeOp(parent, role, index, nodeId))
    }

    public override fun addNewChild(parentId: Long, role: String?, index: Int, concept: IConcept?): Long {
        val childId: Long = idGenerator!!.generate()
        addNewChild(parentId, role, index, childId, concept)
        return childId
    }

    public override fun containsNode(nodeId: Long): Boolean {
        return transaction!!.containsNode(nodeId)
    }

    public override fun getAllChildren(parentId: Long): LongStream? {
        return transaction!!.getAllChildren(parentId)
    }

    override val branch: IBranch
        get() {
            return otBranch
        }

    public override fun getChildren(parentId: Long, role: String?): LongStream? {
        return transaction!!.getChildren(parentId, role)
    }

    public override fun getConcept(nodeId: Long): IConcept? {
        return transaction!!.getConcept(nodeId)
    }

    public override fun getParent(nodeId: Long): Long {
        return transaction!!.getParent(nodeId)
    }

    public override fun getProperty(nodeId: Long, role: String?): String? {
        return transaction!!.getProperty(nodeId, role)
    }

    public override fun getReferenceTarget(sourceId: Long, role: String?): INodeReference? {
        return transaction!!.getReferenceTarget(sourceId, role)
    }

    public override fun getRole(nodeId: Long): String? {
        return transaction!!.getRole(nodeId)
    }

    public override fun getTree(): ITree? {
        return transaction.getTree()
    }

    public override fun setTree(tree: ITree?) {
        throw UnsupportedOperationException()
    }

    protected fun wrap(node: INode?): INode {
        return (if (node is PNodeAdapter) PNodeAdapter(node.getNodeId(), otBranch) else (node)!!)
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger(OTWriteTransaction::class.java)
    }

}