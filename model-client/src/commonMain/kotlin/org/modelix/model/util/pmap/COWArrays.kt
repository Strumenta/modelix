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

package org.modelix.model.util.pmap

expect object COWArrays {
    fun <T> insert(array: Array<T>, index: Int, element: T): Array<T>
    fun <T> addIfAbsent(array: Array<T>, value: T): Array<T>
    fun <T> remove(array: Array<T>, value: T): Array<T>
    operator fun <T> set(array: Array<T>, index: Int, value: T): Array<T>
    fun <T> removeAt(array: Array<T>, index: Int): Array<T>
    fun <T> indexOf(array: Array<T>, value: T): Int
    operator fun set(array: LongArray, index: Int, value: Long): LongArray
    fun <T> add(array: Array<T>, value: T): Array<T>
    fun add(array: LongArray, value: Long): LongArray
    fun add(array: LongArray, values: LongArray): LongArray
}
