package org.modelix.model.client

import io.vavr.Tuple2
import org.apache.commons.lang3.mutable.MutableObject
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.modelix.model.VersionMerger
import org.modelix.model.api.*
import org.modelix.model.client.ReplicatedTree
import org.modelix.model.lazy.CLTree
import org.modelix.model.lazy.CLVersion
import org.modelix.model.lazy.TreeId
import org.modelix.model.operations.IAppliedOperation
import org.modelix.model.operations.IOperation
import org.modelix.model.operations.OTBranch
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.LongConsumer
import java.util.function.Supplier

class ReplicatedTree constructor(private val client: IModelClient, private val treeId: TreeId, private val branchName: String, private val user: Supplier<String?>) {
    private val localBranch: IBranch
    private val localOTBranch: OTBranch
    private val mergeLock: Any = Any()
    private val merger: VersionMerger

    @Volatile
    var version: CLVersion?
        private set

    @Volatile
    private var remoteVersion: CLVersion?
    private val versionChangeDetector: VersionChangeDetector
    private val isEditing: AtomicBoolean = AtomicBoolean(false)
    private var disposed: Boolean = false
    private var divergenceTime: Int = 0
    private val convergenceWatchdog: ScheduledFuture<*>?
    val branch: IBranch
        get() {
            checkDisposed()
            return localOTBranch
        }

    /**
     * Call this at the beginning of an edit operation in the editor
     */
    fun startEdit() {
        isEditing.set(true)
    }

    /**
     * Call this at the end of an edit operation in the editor
     */
    fun endEdit() {
        if (disposed) return
        try {
            synchronized(mergeLock, {
                deleteDetachedNodes()
                createAndMergeLocalVersion()
            })
        } finally {
            isEditing.set(false)
        }
    }

    protected fun deleteDetachedNodes() {
        val hasDetachedNodes: Boolean = (localOTBranch.computeRead(Supplier({
            localOTBranch.transaction
                    .getChildren(ITree.Companion.ROOT_ID, ITree.Companion.DETACHED_NODES_ROLE)!!.iterator().hasNext()
        })))!!
        // avoid unnecessary write
        if (hasDetachedNodes) {
            localOTBranch.runWrite(Runnable({

                // clear detached nodes
                val t: IWriteTransaction? = localOTBranch.writeTransaction
                t!!.getChildren(ITree.Companion.ROOT_ID, ITree.Companion.DETACHED_NODES_ROLE)!!.forEach(LongConsumer({ nodeId: Long -> t.deleteNode(nodeId) }))
            }))
        }
    }

    protected fun createAndMergeLocalVersion() {
        checkDisposed()
        var opsAndTree: Tuple2<List<IAppliedOperation?>, ITree?>?
        var localBase: CLVersion?
        val remoteBase: MutableObject<CLVersion?> = MutableObject()
        val newLocalVersion: MutableObject<CLVersion> = MutableObject()
        synchronized(mergeLock, {
            opsAndTree = localOTBranch.operationsAndTree
            localBase = version
            remoteBase.setValue(remoteVersion)
            val ops: Array<IOperation?> = opsAndTree!!._1()!!.stream().map(Function({ obj: IAppliedOperation? -> obj!!.originalOp })).toArray(IntFunction<Array<IOperation>>({ _Dummy_.__Array__() }))
            if (ops.size == 0) {
                return
            }
            newLocalVersion.setValue(createVersion(opsAndTree!!._2() as CLTree?, ops, localBase.hash))
            version = newLocalVersion.getValue()
            divergenceTime = 0
        })
        SharedExecutors.FIXED.execute(object : Runnable {
            public override fun run() {
                val doMerge: Supplier<Boolean> = label@ Supplier({
                    var mergedVersion: CLVersion
                    try {
                        mergedVersion = merger.mergeChange((remoteBase.getValue())!!, newLocalVersion.getValue())
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(String.format("Merged local %s with remote %s -> %s",
                                    newLocalVersion.getValue().hash,
                                    remoteBase.getValue()!!.hash,
                                    mergedVersion.hash))
                        }
                    } catch (ex: Exception) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("", ex)
                        }
                        mergedVersion = newLocalVersion.getValue()
                    }
                    synchronized(mergeLock, {
                        writeLocalVersion(version)
                        if (remoteVersion === remoteBase.getValue()) {
                            writeRemoteVersion(mergedVersion)
                            return@label true
                        } else {
                            remoteBase.setValue(remoteVersion)
                            return@label false
                        }
                    })
                })

                // Avoid locking during the merge as it may require communication with the model server 
                for (mergeAttempt in 0..2) {
                    if (doMerge.get()) {
                        return
                    }
                }
                synchronized(mergeLock, {
                    remoteBase.setValue(remoteVersion)
                    doMerge.get()
                })
            }
        })
    }

    protected fun writeRemoteVersion(version: CLVersion) {
        synchronized(mergeLock, {
            if (!(Objects.equals(remoteVersion.hash, version.hash))) {
                remoteVersion = version
                client.asyncStore.put(treeId.getBranchKey(branchName), version.hash)
            }
        })
    }

    protected fun writeLocalVersion(version: CLVersion?) {
        synchronized(mergeLock, {
            if (!(Objects.equals(version.hash, version.hash))) {
                this.version = version
                divergenceTime = 0
                localBranch.runWrite(Runnable({
                    val newTree: CLTree? = version.tree
                    if (!(Objects.equals(getHash(newTree), getHash(localBranch.transaction.tree)))) {
                        localBranch.writeTransaction.tree = newTree!!
                    }
                }))
            }
        })
    }

    fun createVersion(tree: CLTree?, operations: Array<IOperation?>, previousVersion: String?): CLVersion {
        checkDisposed()
        val time: String = LocalDateTime.now().toString()
        return CLVersion(client.idGenerator.generate(), time, user.get(), tree.hash, previousVersion, operations, client.getStoreCache())
    }

    fun dispose() {
        checkDisposed()
        disposed = true
        versionChangeDetector.dispose()
        convergenceWatchdog!!.cancel(false)
    }

    fun checkDisposed() {
        if (disposed) {
            throw RuntimeException("Already disposed")
        }
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger(ReplicatedTree::class.java)
        private fun getHash(v: CLVersion?): String? {
            return if (v == null) null else v.hash
        }

        private fun getHash(v: CLTree?): String? {
            return if (v == null) null else v.hash
        }
    }

    init {
        val versionHash: String? = client.get(treeId.getBranchKey(branchName))
        var initialVersion: CLVersion? = CLVersion.Companion.loadFromHash(versionHash, client.storeCache)
        val initialTree: MutableObject<CLTree> = MutableObject()
        if (initialVersion == null) {
            initialTree.setValue(CLTree(treeId, client.storeCache))
            initialVersion = createVersion(initialTree.getValue(), arrayOfNulls(0), null)
            client.put(treeId.getBranchKey(branchName), initialVersion.hash)
        } else {
            initialTree.setValue(CLTree(initialVersion.treeHash, client.storeCache))
        }

        // prefetch to avoid HTTP request in command listener 
        SharedExecutors.FIXED.execute(Runnable({ initialTree.getValue().getChildren(ITree.Companion.ROOT_ID, ITree.Companion.DETACHED_NODES_ROLE) }))
        version = initialVersion
        remoteVersion = initialVersion
        localBranch = PBranch(initialTree.getValue())
        localOTBranch = OTBranch(localBranch, client.idGenerator)
        merger = VersionMerger(client.storeCache, client.idGenerator)
        versionChangeDetector = object : VersionChangeDetector(client, treeId.getBranchKey(branchName)) {
            override fun processVersionChange(oldVersionHash: String?, newVersionHash: String?) {
                if (disposed) {
                    return
                }
                if ((newVersionHash == null || newVersionHash.length == 0)) {
                    return
                }
                if (Objects.equals(newVersionHash, getHash(remoteVersion))) {
                    return
                }
                val newRemoteVersion: CLVersion? = CLVersion.Companion.loadFromHash(newVersionHash, client.storeCache)
                if (newRemoteVersion == null) {
                    return
                }
                val localBase: MutableObject<CLVersion?> = MutableObject()
                synchronized(mergeLock, {
                    localBase.setValue(version)
                    remoteVersion = newRemoteVersion
                })
                val doMerge: Supplier<Boolean> = label@ Supplier({
                    var mergedVersion: CLVersion
                    try {
                        mergedVersion = merger.mergeChange((localBase.getValue())!!, newRemoteVersion)
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(String.format("Merged remote %s with local %s -> %s",
                                    newRemoteVersion.hash,
                                    localBase.getValue().hash,
                                    mergedVersion.hash))
                        }
                    } catch (ex: Exception) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("", ex)
                        }
                        mergedVersion = newRemoteVersion
                    }
                    val mergedTree: CLTree? = mergedVersion.getTree()
                    synchronized(mergeLock, {
                        remoteVersion = mergedVersion
                        if (version === localBase.getValue()) {
                            writeLocalVersion(mergedVersion)
                            writeRemoteVersion(mergedVersion)
                            return@label true
                        } else {
                            localBase.setValue(version)
                            return@label false
                        }
                    })
                })

                // Avoid locking during the merge as it may require communication with the model server 
                for (mergeAttempt in 0..2) {
                    if (doMerge.get()) {
                        return
                    }
                }
                synchronized(mergeLock, {
                    localBase.setValue(version)
                    doMerge.get()
                })
            }
        }
        localOTBranch.addListener(object : IBranchListener {
            public override fun treeChanged(oldTree: ITree?, newTree: ITree?) {
                if (disposed) {
                    return
                }
                if (isEditing.get()) {
                    return
                }
                SharedExecutors.FIXED.execute(Runnable({
                    if (isEditing.get()) {
                        return@execute
                    }
                    createAndMergeLocalVersion()
                }))
            }
        })
        convergenceWatchdog = SharedExecutors.fixDelay(1000, object : Runnable {
            public override fun run() {
                val localHash: String? = if (version == null) null else version!!.hash
                val remoteHash: String? = if (remoteVersion == null) null else remoteVersion!!.hash
                if (Objects.equals(localHash, remoteHash)) {
                    divergenceTime = 0
                } else {
                    divergenceTime++
                }
                if (divergenceTime > 5) {
                    synchronized(mergeLock, { divergenceTime = 0 })
                }
            }
        })
    }
}