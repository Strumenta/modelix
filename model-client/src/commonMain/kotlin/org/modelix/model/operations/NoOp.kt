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

package org.modelix.model.operations

import org.modelix.model.api.IWriteTransaction

class NoOp : AbstractOperation(), IAppliedOperation {
    override fun apply(transaction: IWriteTransaction?): IAppliedOperation? {
        return this
    }

    override fun transform(previous: IOperation?): IOperation? {
        return null
    }

    override val originalOp: IOperation
        get() = this

    override fun invert(): IOperation? {
        return this
    }
}
