package org.modelix.model.operations

import io.vavr.Tuple
import io.vavr.Tuple2
import org.modelix.model.api.*
import java.util.*
import java.util.function.Supplier

class OTBranch constructor(private val branch: IBranch, private val idGenerator: IIdGenerator?) : IBranch {
    private var operations: MutableList<IAppliedOperation?> = ArrayList()
    private val operationsLock: Any = Any()
    fun operationApplied(op: IAppliedOperation?) {
        synchronized(operationsLock, { operations.add(op) })
    }

    val newOperations: List<IAppliedOperation?>
        get() {
            synchronized(operationsLock, {
                val result: List<IAppliedOperation?> = operations
                operations = ArrayList()
                return result
            })
        }

    val operationsAndTree: Tuple2<List<IAppliedOperation?>, ITree?>
        get() {
            synchronized(operationsLock, { return Tuple.of(newOperations, computeRead(Supplier({ transaction.getTree() }))) })
        }

    public override fun addListener(l: IBranchListener?) {
        branch.addListener(l)
    }

    public override fun removeListener(l: IBranchListener?) {
        branch.removeListener(l)
    }

    public override fun canRead(): Boolean {
        return branch.canRead()
    }

    public override fun canWrite(): Boolean {
        return branch.canWrite()
    }

    public override fun <T> computeRead(computable: Supplier<T>): T? {
        checkNotEDT()
        return branch.computeRead(computable)
    }

    public override fun <T> computeWrite(computable: Supplier<T>): T? {
        checkNotEDT()
        return branch.computeWrite(computable)
    }

    override val readTransaction: IReadTransaction?
        get() {
            return branch.getReadTransaction()
        }

    override val transaction: ITransaction
        get() {
            return wrap(branch.getTransaction())
        }

    override val writeTransaction: IWriteTransaction
        get() {
            return wrap(branch.getWriteTransaction())
        }

    public override fun runRead(runnable: Runnable) {
        checkNotEDT()
        branch.runRead(runnable)
    }

    public override fun runWrite(runnable: Runnable?) {
        checkNotEDT()
        branch.runWrite(runnable)
    }

    fun wrap(t: ITransaction?): ITransaction {
        return (if (t is IWriteTransaction) wrap(t as IWriteTransaction?) else (t)!!)
    }

    fun wrap(t: IWriteTransaction?): IWriteTransaction {
        return OTWriteTransaction(t, this, idGenerator)
    }

    protected fun checkNotEDT() {}

}