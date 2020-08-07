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

import org.modelix.model.IKeyListener
import org.modelix.model.IKeyValueStore
import org.modelix.model.lazy.IDeserializingKeyValueStore
import kotlin.jvm.Synchronized

expect class RestWebModelClient constructor(baseUrl: String? = null) : IModelClient {
    companion object {
        val modelUrlFromEnv: String?
        val defaultUrl: String
    }

    override var clientId : Int
    override val asyncStore: IKeyValueStore

    @get:Synchronized
    override var idGenerator: IdGenerator?

    fun dispose()

    override fun get(key: String?): String?

    override fun getAll(keys: Iterable<String?>?): Map<String?, String?>?

    fun setAuthToken(token: String?)

    val email: String

    override fun listen(key: String?, keyListener: IKeyListener?)

    override fun removeListener(key: String?, listener: IKeyListener?)

    override fun put(key: String?, value: String?)

    override fun putAll(entries: Map<String?, String?>?)
    override fun prefetch(key: String?)

    override val storeCache: IDeserializingKeyValueStore

}
