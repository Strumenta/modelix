/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */

package org.modelix.model.client

import org.modelix.model.VersionMerger
import org.modelix.model.api.*
import org.modelix.model.lazy.CLTree
import org.modelix.model.lazy.CLVersion
import org.modelix.model.lazy.TreeId
import org.modelix.model.operations.IAppliedOperation
import org.modelix.model.operations.IOperation
import org.modelix.model.operations.OTBranch
import org.modelix.model.util.*
import kotlin.jvm.Volatile

class ReplicatedTree(private val client: IModelClient, private val treeId: TreeId, private val branchName: String, private val user: Supplier<String>) {
    private val localBranch: IBranch
    private val localOTBranch: OTBranch
    private val mergeLock = Any()
    private val merger: VersionMerger

    @Volatile
    var version: CLVersion?
        private set

    @Volatile
    private var remoteVersion: CLVersion?
    private val versionChangeDetector: VersionChangeDetector
    private val isEditing = AtomicBoolean(false)
    private var disposed = false
    private var divergenceTime = 0
    private val convergenceWatchdog: ScheduledFuture<*>
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
            synchronized(mergeLock) {
                deleteDetachedNodes()
                createAndMergeLocalVersion()
            }
        } finally {
            isEditing.set(false)
        }
    }

    protected fun deleteDetachedNodes() {
        val hasDetachedNodes = localOTBranch.computeRead(
            org.modelix.model.util.Supplier<Boolean> {
                localOTBranch.transaction!!
                    .getChildren(ITree.ROOT_ID, ITree.DETACHED_NODES_ROLE)!!.iterator().hasNext()
            }
        )
        // avoid unnecessary write
        if (hasDetachedNodes) {
            localOTBranch.runWrite(
                Runnable {

                    // clear detached nodes
                    val t: IWriteTransaction = localOTBranch.writeTransaction!!
                    t.getChildren(ITree.ROOT_ID, ITree.DETACHED_NODES_ROLE)!!.forEach { nodeId: Long -> t.deleteNode(nodeId) }
                }
            )
        }
    }

    protected fun createAndMergeLocalVersion() {
        checkDisposed()
        var opsAndTree: org.modelix.model.util.Tuple2<List<IAppliedOperation>, ITree>
        var localBase: CLVersion?
        val remoteBase = MutableObject<CLVersion?>()
        val newLocalVersion = MutableObject<CLVersion>()
        synchronized(mergeLock) {
            opsAndTree = localOTBranch.operationsAndTree
            localBase = version
            remoteBase.setValue(remoteVersion)
            val ops: Array<IOperation?> = opsAndTree._1.map { it?.originalOp }.toTypedArray()
            // val ops: Array<IOperation?> = opsAndTree._1().stream().map(IAppliedOperation::originalOp).toArray(IntFunction<Array<IOperation>> { _Dummy_.__Array__() })
            if (ops.size == 0) {
                return
            }
            newLocalVersion.setValue(createVersion(opsAndTree._2 as CLTree, ops, localBase!!.hash))
            version = newLocalVersion.getValue()
            divergenceTime = 0
        }
        SharedExecutors.FIXED.execute(object : Runnable {
            override fun run() {
                val doMerge: Supplier<Boolean> = object : Supplier<Boolean> {
                    override fun get(): Boolean {

                        var mergedVersion: CLVersion
                        try {
                            mergedVersion = merger.mergeChange(remoteBase.getValue()!!, newLocalVersion.getValue()!!)
                            if (LOG.isDebugEnabled) {
                                LOG.debug(
                                    "Merged local ${newLocalVersion.getValue()!!.hash} with remote ${remoteBase.getValue()!!.hash} -> ${mergedVersion.hash}"
                                )
                            }
                        } catch (ex: Exception) {
                            if (LOG.isEnabledFor(Level.ERROR)) {
                                LOG.error("", ex)
                            }
                            mergedVersion = newLocalVersion.getValue()!!
                        }
                        synchronized(mergeLock) {
                            writeLocalVersion(version)
                            return if (remoteVersion == remoteBase.getValue()!!) {
                                writeRemoteVersion(mergedVersion)
                                true
                            } else {
                                remoteBase.setValue(remoteVersion)
                                false
                            }
                        }
                    }
                }

                // Avoid locking during the merge as it may require communication with the model server
                for (mergeAttempt in 0..2) {
                    if (doMerge.get()) {
                        return
                    }
                }
                synchronized(mergeLock) {
                    remoteBase.setValue(remoteVersion)
                    doMerge.get()
                }
            }
        })
    }

    protected fun writeRemoteVersion(version: CLVersion) {
        synchronized(mergeLock) {
            if (remoteVersion!!.hash != version.hash) {
                remoteVersion = version
                client.asyncStore!!.put(treeId.getBranchKey(branchName), version.hash)
            }
        }
    }

    protected fun writeLocalVersion(version: CLVersion?) {
        synchronized(mergeLock) {
            if (version!!.hash != version.hash) {
                this.version = version
                divergenceTime = 0
                localBranch.runWrite(
                    Runnable {
                        val newTree = version.tree
                        val currentTree = localBranch.transaction!!.tree as CLTree?
                        if (getHash(newTree) != getHash(currentTree)) {
                            localBranch.writeTransaction!!.tree = newTree
                        }
                    }
                )
            }
        }
    }

    fun createVersion(tree: CLTree, operations: Array<IOperation?>?, previousVersion: String?): CLVersion {
        checkDisposed()
        val time = nowString()
        return CLVersion(client.idGenerator!!.generate(), time, user.get(), tree.hash, previousVersion, operations!!, client.storeCache!!)
    }

    fun dispose() {
        checkDisposed()
        disposed = true
        versionChangeDetector.dispose()
        convergenceWatchdog.cancel(false)
    }

    fun checkDisposed() {
        if (disposed) {
            throw RuntimeException("Already disposed")
        }
    }

    companion object {
        private val LOG = LogManager.getLogger(ReplicatedTree::class)
        private fun getHash(v: CLVersion?): String? {
            return v?.hash
        }

        private fun getHash(v: CLTree?): String? {
            return v?.hash
        }
    }

    init {
        val versionHash = client[treeId.getBranchKey(branchName)]
        var initialVersion = CLVersion.loadFromHash(versionHash, client.storeCache!!)
        val initialTree = MutableObject<CLTree>()
        if (initialVersion == null) {
            initialTree.setValue(CLTree(treeId, client.storeCache!!))
            initialVersion = createVersion(initialTree.getValue()!!, arrayOfNulls(0), null)
            client.put(treeId.getBranchKey(branchName), initialVersion.hash)
        } else {
            initialTree.setValue(CLTree(initialVersion.treeHash, client.storeCache!!))
        }

        // prefetch to avoid HTTP request in command listener
        SharedExecutors.FIXED.execute(Runnable { initialTree.getValue()!!.getChildren(ITree.ROOT_ID, ITree.DETACHED_NODES_ROLE) })
        version = initialVersion
        remoteVersion = initialVersion
        localBranch = PBranch(initialTree.getValue()!!)
        localOTBranch = OTBranch(localBranch, client.idGenerator!!)
        merger = VersionMerger(client.storeCache!!, client.idGenerator!!)
        versionChangeDetector = object : VersionChangeDetector(client, treeId.getBranchKey(branchName)) {
            override fun processVersionChange(oldVersionHash: String?, newVersionHash: String?) {
                if (disposed) {
                    return
                }
                if (newVersionHash == null || newVersionHash.length == 0) {
                    return
                }
                if (newVersionHash == getHash(remoteVersion)) {
                    return
                }
                val newRemoteVersion = CLVersion.loadFromHash(newVersionHash, client.storeCache!!) ?: return
                val localBase = MutableObject<CLVersion?>()
                synchronized(mergeLock) {
                    localBase.setValue(version)
                    remoteVersion = newRemoteVersion
                }
                val doMerge = object : Supplier<Boolean> {
                    override fun get(): Boolean {
                        var mergedVersion: CLVersion
                        try {
                            mergedVersion = merger.mergeChange(localBase.getValue()!!, newRemoteVersion)
                            if (LOG.isDebugEnabled) {
                                LOG.debug(
                                    "Merged remote ${newRemoteVersion.hash} with local ${localBase.getValue()!!.hash} -> ${mergedVersion.hash}"
                                )
                            }
                        } catch (ex: Exception) {
                            if (LOG.isEnabledFor(Level.ERROR)) {
                                LOG.error("", ex)
                            }
                            mergedVersion = newRemoteVersion
                        }
                        val mergedTree = mergedVersion.tree
                        synchronized(mergeLock) {
                            remoteVersion = mergedVersion
                            if (version == localBase.getValue()) {
                                writeLocalVersion(mergedVersion)
                                writeRemoteVersion(mergedVersion)
                                return true
                            } else {
                                localBase.setValue(version)
                                return false
                            }
                        }
                    }
                }

                // Avoid locking during the merge as it may require communication with the model server
                for (mergeAttempt in 0..2) {
                    if (doMerge.get()) {
                        return
                    }
                }
                synchronized(mergeLock) {
                    localBase.setValue(version)
                    doMerge.get()
                }
            }
        }
        localOTBranch.addListener(object : IBranchListener {
            override fun treeChanged(oldTree: ITree?, newTree: ITree?) {
                if (disposed) {
                    return
                }
                if (isEditing.get()) {
                    return
                }
                SharedExecutors.FIXED.execute(
                    Runnable {
                        if (!isEditing.get()) {
                            createAndMergeLocalVersion()
                        }
                    }
                )
            }
        })
        convergenceWatchdog = SharedExecutors.fixDelay(
            1000,
            object : Runnable {
                override fun run() {
                    val localHash = if (version == null) null else version!!.hash
                    val remoteHash = if (remoteVersion == null) null else remoteVersion!!.hash
                    if (localHash == remoteHash) {
                        divergenceTime = 0
                    } else {
                        divergenceTime++
                    }
                    if (divergenceTime > 5) {
                        synchronized(mergeLock) { divergenceTime = 0 }
                    }
                }
            }
        )
    }
}
