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

package org.modelix.model.util

// import java.util.stream.Collectors
// import java.util.stream.LongStream
// import java.util.stream.Stream
// import java.util.stream.StreamSupport
// import kotlin.streams.toList
//
// fun <T> Stream<T>.toSequence(): Sequence<T> {
//    return this.collect(Collectors.toList()).asSequence()
// }
//
// fun LongStream.toSequence(): Sequence<Long> {
//    return this.toList().asSequence()
// }
//
// fun <T> Sequence<T>.toStream(): Stream<T> {
//    return this.toList().stream()
// }
//
// fun Sequence<Long>.toLongStream(): LongStream {
//    var b = LongStream.builder()
//    this.forEach { b = b.add(it) }
//    return b.build()
// }
//
// fun <T> Sequence<T>.skip(n: Long): Sequence<T> {
//    return this.drop(n.toInt())
// }

expect object StreamUtils {

    fun indexOf(stream: Sequence<Long>, value: Long): Int
}
