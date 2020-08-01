package org.modelix.model

import gnu.trove.set.TLongSet
import gnu.trove.set.hash.TLongHashSet
import org.apache.commons.lang3.mutable.MutableObject
import org.modelix.model.api.*
import org.modelix.model.lazy.CLTree
import org.modelix.model.lazy.CLVersion
import org.modelix.model.lazy.IDeserializingKeyValueStore
import org.modelix.model.operations.IAppliedOperation
import org.modelix.model.operations.IOperation
import org.modelix.model.persistent.CPVersion
import org.modelix.model.util.StreamUtils
import java.util.*
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.stream.Collectors

class VersionMerger constructor(private val storeCache: IDeserializingKeyValueStore?, private val idGenerator: IIdGenerator?) {
    private val mergeLock: Any = Any()
    fun mergeChange(lastMergedVersion: CLVersion, newVersion: CLVersion): CLVersion {
        var lastMergedVersion: CLVersion = lastMergedVersion
        synchronized(mergeLock, {
            if (lastMergedVersion == null) {
                lastMergedVersion = newVersion
                return newVersion
            } else {
                if (Objects.equals(newVersion.getHash(), lastMergedVersion.getHash())) {
                    return lastMergedVersion
                }
                val merged: CLVersion = mergeHistory(lastMergedVersion.getHash(), newVersion.getHash())
                checkTreeIds(lastMergedVersion, newVersion)
                return merged
            }
        })
    }

    fun checkTreeIds(v1: CLVersion, v2: CLVersion) {
        val id1: String? = v1.getTree().getId()
        val id2: String? = v2.getTree().getId()
        if (!(Objects.equals(id1, id2))) {
            throw RuntimeException("Tree ID mismatch: " + id1 + " and " + id2)
        }
    }

    protected fun mergeHistory(leftVersionHash: String?, rightVersionHash: String?): CLVersion {
        val commonBase: String? = commonBaseVersion(leftVersionHash, rightVersionHash)
        val leftHistory: List<CLVersion> = getHistory((leftVersionHash)!!, commonBase)
        val rightHistory: List<CLVersion> = getHistory((rightVersionHash)!!, commonBase)
        val mergedVersion: MutableObject<CLVersion> = MutableObject()
        var tree: CLTree? = getVersion(commonBase).getTree()
        if (tree == null) {
            tree = CLTree((storeCache)!!)
        }
        val branch: IBranch = PBranch(tree)
        if (rightHistory.isEmpty() || leftHistory.isEmpty()) {
            val fastForwardHistory: List<CLVersion> = if (leftHistory.isEmpty()) rightHistory else leftHistory
            val numOps: Int = fastForwardHistory.stream().map(Function({ obj: CLVersion -> obj.getNumberOfOperations() })).reduce(0, BinaryOperator({ a: Int, b: Int -> Math.max(a, b) }))
            if (numOps > 100) {
                return fastForwardHistory.get(0)
            }
            // A small number of changes may be faster to compute locally. 
        }
        branch.runWrite(Runnable({
            val t: IWriteTransaction? = branch.getWriteTransaction()
            val leftAppliedOps: MutableList<IAppliedOperation?> = ArrayList()
            val rightAppliedOps: MutableList<IAppliedOperation?> = ArrayList()
            val appliedVersionIds: TLongSet = TLongHashSet()
            while (!leftHistory.isEmpty() || !rightHistory.isEmpty()) {
                val useLeft: Boolean = rightHistory.isEmpty() || !leftHistory.isEmpty() && StreamUtils.last(leftHistory).getId() < StreamUtils.last(rightHistory).getId()
                val versionToApply: CLVersion? = StreamUtils.removeLast(if (useLeft) leftHistory else rightHistory)
                if (appliedVersionIds.contains(versionToApply.getId())) {
                    continue
                }
                appliedVersionIds.add(versionToApply.getId())
                val oppositeAppliedOps: List<IOperation> = (if (useLeft) rightAppliedOps else leftAppliedOps).stream()
                        .map(Function({ obj: IAppliedOperation? -> obj.getOriginalOp() }))
                        .collect(Collectors.toList())
                val operationsToApply: List<IOperation> = StreamUtils.toStream(versionToApply.getOperations())
                        .map(Function({ it: IOperation? -> transformOperation(it, oppositeAppliedOps) }))
                        .collect(Collectors.toList())
                for (op: IOperation in operationsToApply) {
                    val appliedOp: IAppliedOperation? = op.apply(t)
                    if (useLeft) {
                        leftAppliedOps.add(appliedOp)
                    } else {
                        rightAppliedOps.add(appliedOp)
                    }
                }
                mergedVersion.setValue(CLVersion(
                        versionToApply.getId(),
                        versionToApply.getTime(),
                        versionToApply.getAuthor(),
                        (t.getTree() as CLTree?).getHash(),
                        if (mergedVersion.getValue() != null) mergedVersion.getValue().getHash() else versionToApply.getPreviousHash(),
                        operationsToApply.toTypedArray(),
                        storeCache
                ))
            }
        }))
        if (mergedVersion.getValue() == null) {
            throw RuntimeException("Failed to merge " + leftVersionHash + " and " + rightVersionHash)
        }
        return mergedVersion.getValue()
    }

    protected fun transformOperation(opToTransform: IOperation?, previousOps: Iterable<IOperation>): IOperation? {
        var result: IOperation? = opToTransform
        for (previous: IOperation in previousOps) {
            result = result!!.transform(previous)
        }
        return result
    }

    /**
     *
     *
     * @param fromVersion The newest version
     * @param toVersionExclusive The oldest version
     * @return Newest version first
     */
    protected fun getHistory(fromVersion: String, toVersionExclusive: String?): List<CLVersion> {
        val history: MutableList<CLVersion> = ArrayList()
        if (Objects.equals(fromVersion, toVersionExclusive)) {
            return history
        }
        var version: CLVersion? = getVersion(fromVersion)
        while (true) {
            if (version == null) {
                break
            }
            history.add(version)
            if (version.getPreviousHash() == null) {
                break
            }
            if (Objects.equals(version.getPreviousHash(), toVersionExclusive)) {
                break
            }
            version = version.getPreviousVersion()
        }
        return history
    }

    protected fun commonBaseVersion(leftHash: String?, rightHash: String?): String? {
        var leftHash: String? = leftHash
        var rightHash: String? = rightHash
        val leftVersions: MutableSet<String> = HashSet()
        val rightVersions: MutableSet<String> = HashSet()
        while (leftHash != null || rightHash != null) {
            if (leftHash != null) {
                leftVersions.add(leftHash)
            }
            if (rightHash != null) {
                rightVersions.add(rightHash)
            }
            if (leftHash != null) {
                if (rightVersions.contains(leftHash)) {
                    return leftHash
                }
            }
            if (rightHash != null) {
                if (leftVersions.contains(rightHash)) {
                    return rightHash
                }
            }
            if (leftHash != null) {
                leftHash = Optional.ofNullable(getVersion(leftHash)).map<String>(Function({ obj: CLVersion -> obj.getPreviousHash() })).orElse(null)
            }
            if (rightHash != null) {
                rightHash = Optional.ofNullable(getVersion(rightHash)).map<String>(Function({ obj: CLVersion -> obj.getPreviousHash() })).orElse(null)
            }
        }
        return null
    }

    private fun getVersion(hash: String?): CLVersion? {
        if (hash == null) {
            return null
        }
        return CLVersion.Companion.loadFromHash(hash, storeCache)
    }

    protected fun getTree(version: CPVersion?): ITree? {
        if (version == null) {
            return null
        }
        return CLTree(version.treeHash, storeCache)
    }

}