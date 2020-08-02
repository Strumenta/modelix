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

package org.modelix.model

expect class KeyValueStoreCache(store: IKeyValueStore) : IKeyValueStore {
    override fun prefetch(rootKey: String?)

    override fun get(key: String?): String?

    override fun getAll(keys_: Iterable<String?>?): Map<String?, String?>?

    override fun listen(key: String?, listener: IKeyListener?)

    override fun put(key: String?, value: String?)

    override fun putAll(entries: Map<String?, String?>?)

    override fun removeListener(key: String?, listener: IKeyListener?)
}
