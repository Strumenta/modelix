package org.modelix.model.api

import org.apache.commons.lang3.mutable.MutableObject
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.modelix.model.api.PBranch
import org.modelix.model.util.ContextValue
import org.modelix.model.util.pmap.COWArrays
import java.util.function.Supplier

class PBranch @JvmOverloads constructor(@field:Volatile private var tree: ITree?, private val idGenerator: IIdGenerator? = DefaultIdGenerator.Companion.instance) : IBranch {
    private val writeLock = Any()
    val contextTransactions = ContextValue<Transaction?>()
    private var listeners: Array<IBranchListener?>? = arrayOfNulls(0)
    fun runWithTransaction(transaction: ITransaction?, runnable: Runnable?) {
        contextTransactions.runWith(transaction as Transaction?, runnable!!)
    }

    override fun runRead(runnable: Runnable) {
        val prevTransaction = contextTransactions.getValue()
        if (prevTransaction is IReadTransaction) {
            runnable.run()
        } else {
            val currentTree = if (prevTransaction == null) tree else prevTransaction.tree
            val t = ReadTransaction(currentTree!!, this)
            contextTransactions.runWith(t, runnable)
        }
    }

    override fun runWrite(runnable: Runnable?) {
        synchronized(writeLock) {
            val prevTransaction = contextTransactions.getValue()
            check(prevTransaction !is ReadTransaction) { "Cannot run write from read" }
            val prevWrite = prevTransaction as WriteTransaction?
            val oldTree = if (prevWrite == null) tree else prevWrite.getTree()
            val newWrite = WriteTransaction(oldTree!!, this, idGenerator)
            try {
                contextTransactions.runWith(newWrite, runnable!!)
                newWrite.close()
                val newTree = newWrite.getTree()
                if (prevWrite == null) {
                    tree = newTree
                    notifyTreeChange(oldTree, newTree)
                } else {
                    prevWrite.setTree(newTree)
                }
            } finally {
                newWrite.close()
            }
        }
    }

    override fun <T> computeRead(computable: Supplier<T>): T? {
        val result = MutableObject<T>()
        runRead(Runnable { result.setValue(computable.get()) })
        return result.value
    }

    override fun <T> computeWrite(computable: Supplier<T>): T? {
        val result = MutableObject<T>()
        runWrite(Runnable { result.setValue(computable.get()) })
        return result.value
    }

    override fun canRead(): Boolean {
        return contextTransactions.getValue() != null
    }

    override fun canWrite(): Boolean {
        return contextTransactions.getValue() is IWriteTransaction
    }

    override val transaction: ITransaction
        get() = contextTransactions.getValue() ?: throw IllegalStateException("Not in a transaction")

    override val readTransaction: IReadTransaction?
        get() {
            val transaction: ITransaction = transaction
            check(transaction is ReadTransaction) { "Not in a read transaction" }
            return transaction
        }

    override val writeTransaction: IWriteTransaction
        get() {
            val transaction: ITransaction = transaction
            check(transaction is WriteTransaction) { "Not in a write transaction" }
            return transaction
        }

    override fun addListener(l: IBranchListener?) {
        listeners = COWArrays.addIfAbsent(listeners, l)
    }

    override fun removeListener(l: IBranchListener?) {
        listeners = COWArrays.remove(listeners, l)
    }

    protected fun notifyTreeChange(oldTree: ITree?, newTree: ITree?) {
        if (oldTree === newTree) {
            return
        }
        for (l in listeners!!) {
            try {
                l!!.treeChanged(oldTree, newTree)
            } catch (ex: Exception) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Exception in branch listener", ex)
                }
            }
        }
    }

    companion object {
        private val LOG = LogManager.getLogger(PBranch::class.java)
    }

}