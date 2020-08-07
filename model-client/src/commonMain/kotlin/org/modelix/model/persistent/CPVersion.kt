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

package org.modelix.model.persistent

import org.modelix.model.operations.IOperation
import org.modelix.model.util.LogManager
import org.modelix.model.util.Logger

expect class CPVersion(id: Long, time: String?, author: String?, treeHash: String?, previousVersion: String?, operations: Array<IOperation?>?, operationsHash: String?, numberOfOperations: Int) {
    val id: Long
    val time: String?
    val author: String?
    /**
     * SHA to CPTree
     */
    val treeHash: String?
    val previousVersion: String?
    val operations: Array<IOperation?>?
    val operationsHash: String?
    val numberOfOperations: Int

    val hash: String

    fun serialize(): String

    companion object {
        fun deserialize(input: String): CPVersion
    }
}
