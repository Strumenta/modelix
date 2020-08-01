package org.modelix.model.client

import org.modelix.model.api.IBranch
import org.modelix.model.api.IBranchListener

class SimpleIndirectBranch constructor(override val branch: IBranch) : IIndirectBranch {
    public override fun addListener(l: IBranchListener) {
        branch.addListener(l)
    }

//    public override fun getBranch(): IBranch? {
//        return branch
//    }

    public override fun removeListener(l: IBranchListener?) {
        branch.removeListener(l)
    }

}