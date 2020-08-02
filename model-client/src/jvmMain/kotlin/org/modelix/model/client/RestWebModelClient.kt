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

import org.apache.commons.io.FileUtils
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.json.JSONArray
import org.json.JSONObject
import org.modelix.model.IKeyListener
import org.modelix.model.IKeyValueStore
import org.modelix.model.KeyValueStoreCache
import org.modelix.model.client.SharedExecutors.fixDelay
import org.modelix.model.lazy.IDeserializingKeyValueStore
import org.modelix.model.lazy.ObjectStoreCache
import org.modelix.model.persistent.HashUtil
import org.modelix.model.util.StreamUtils.toStream
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Objects
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.ToLongFunction
import java.util.stream.Stream
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.client.Entity
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.sse.SseEventSource
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class RestWebModelClient @JvmOverloads constructor(var baseUrl: String? = null) : IModelClient {
    companion object {
        private val LOG = LogManager.getLogger(RestWebModelClient::class.java)
        const val MODEL_URI_VAR_NAME = "MODEL_URI"
        private var defaultToken: String? = null
        val modelUrlFromEnv: String?
            get() {
                var url = System.getProperty(MODEL_URI_VAR_NAME)
                if (url == null || url.length == 0) {
                    url = System.getenv(MODEL_URI_VAR_NAME)
                }
                return url
            }

        val defaultUrl: String
            get() {
                val urlFromEnv = modelUrlFromEnv
                return if ((urlFromEnv != null && urlFromEnv.length > 0)) {
                    urlFromEnv
                } else {
                    "http://modelix.q60.de:80/model/"
                }
            }

        private fun isEmptyString(str: String?): Boolean {
            return str == null || str.length == 0
        }

        init {
            try {
                val sharedSecretFile = File("/secrets/modelsecret/modelsecret.txt")
                if (sharedSecretFile.exists()) {
                    defaultToken = FileUtils.readFileToString(sharedSecretFile, StandardCharsets.UTF_8)
                }
            } catch (ex: Exception) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Failed to load default token", ex)
                }
            }
        }
    }

    override var clientId = 0
        get() {
            if (field == 0) {
                val response = client.target(baseUrl + "counter/clientId").request().post(Entity.text(""))
                val idStr = response.readEntity(String::class.java)
                field = idStr.toInt()
            }
            return field
        }
        private set
    private val client: Client
    private val listeners: MutableList<SseListener> = ArrayList()
    override val asyncStore: IKeyValueStore = GarbageFilteringStore(AsyncStore(this))
    private val cache = ObjectStoreCache(KeyValueStoreCache(asyncStore))

    @get:Synchronized
    override var idGenerator: IdGenerator? = null
        get() {
            if (field == null) {
                field = IdGenerator(clientId)
            }
            return field
        }
        private set
    private val watchDogTask: ScheduledFuture<*>
    private var authToken = defaultToken

    fun dispose() {
        watchDogTask.cancel(false)
        synchronized(listeners) {
            listeners.forEach(Consumer { obj: SseListener -> obj.dispose() })
            listeners.clear()
        }
    }

    override fun get(key: String?): String? {
        val isHash = HashUtil.isSha256(key)
        if (isHash) {
            if (LOG.isDebugEnabled) {
                LOG.debug("GET $key")
            }
        }
        val start = System.currentTimeMillis()
        val response = client.target(baseUrl + "get/" + URLEncoder.encode(key, StandardCharsets.UTF_8)).request().buildGet().invoke()
        return if (response.status == Response.Status.OK.statusCode) {
            val value = response.readEntity(String::class.java)
            val end = System.currentTimeMillis()
            if (isHash) {
                if (LOG.isDebugEnabled) {
                    LOG.debug("GET " + key + " took " + (end - start) + " ms: " + value)
                }
            }
            value
        } else if (response.status == Response.Status.NOT_FOUND.statusCode) {
            null
        } else {
            throw RuntimeException("Request for key '" + key + "' failed: " + response.statusInfo)
        }
    }

    override fun getAll(keys: Iterable<String?>?): Map<String?, String?>? {
        if (!keys!!.iterator().hasNext()) {
            return HashMap()
        }
        val json = JSONArray()
        for (key in keys) {
            json.put(key)
        }
        val body = json.toString()
        val start = System.currentTimeMillis()
        val response = client.target(baseUrl + "getAll").request(MediaType.APPLICATION_JSON).put(Entity.text(body))
        return if (response.status == Response.Status.OK.statusCode) {
            val jsonStr = response.readEntity(String::class.java)
            val responseJson = JSONArray(jsonStr)
            val result: MutableMap<String?, String?> = LinkedHashMap(16, 0.75.toFloat(), false)
            for (entry_: Any in responseJson) {
                val entry = entry_ as JSONObject
                result[entry.getString("key")] = entry.optString("value")
            }
            val end = System.currentTimeMillis()
            result
        } else {
            throw RuntimeException(
                String.format(
                    "Request for %d keys failed (%s, ...): %s",
                    keys.spliterator().exactSizeIfKnown,
                    toStream(keys).findFirst().orElse(null),
                    response.statusInfo
                )
            )
        }
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    val email: String
        get() {
            val response = client.target(baseUrl + "getEmail").request().buildGet().invoke()
            return if (response.status == Response.Status.OK.statusCode) {
                response.readEntity(String::class.java)
            } else {
                throw RuntimeException("Request for e-mail address failed: " + response.statusInfo)
            }
        }

    override fun listen(key: String?, keyListener: IKeyListener?) {
        val sseListener = SseListener(key, keyListener)
        synchronized(listeners) { listeners.add(sseListener) }
    }

    override fun removeListener(key: String?, listener: IKeyListener?) {
        synchronized(listeners) { listeners.removeIf({ it: SseListener -> Objects.equals(it.key, key) && it.keyListener === listener }) }
    }

    override fun put(key: String?, value: String?) {
        if (!key!!.matches(Regex("[a-zA-Z0-9-_]{43}"))) {
            if (LOG.isDebugEnabled) {
                LOG.debug("PUT $key = $value")
            }
        }
        val response = client.target(baseUrl + "put/" + URLEncoder.encode(key, StandardCharsets.UTF_8)).request(MediaType.TEXT_PLAIN).put(Entity.text(value))
        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
            throw RuntimeException("Failed to store entry (" + response.statusInfo + ") " + key + " = " + value)
        }
    }

    override fun putAll(entries: Map<String?, String?>?) {
        val sendBatch = Consumer<JSONArray> { json ->
            if (LOG.isDebugEnabled) {
                LOG.debug("PUT batch of " + json.length() + " entries")
            }
            val response = client.target(baseUrl + "putAll").request(MediaType.APPLICATION_JSON).put(Entity.text(json.toString()))
            if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
                throw RuntimeException(
                    String.format(
                        "Failed to store %d entries (%s) %s",
                        entries!!.size,
                        response.statusInfo,
                        entries.entries.stream().map { e: Map.Entry<String?, String?> -> e.key.toString() + " = " + e.value + ", ..." }.findFirst().orElse("")
                    )
                )
            }
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("PUT " + entries!!.size + " entries")
        }
        var json = JSONArray()
        var approxSize = 0
        for ((key, value) in entries!!) {
            val jsonEntry = JSONObject()
            jsonEntry.put("key", key)
            jsonEntry.put("value", value)
            approxSize += key!!.length
            approxSize += value!!.length
            json.put(jsonEntry)
            if (!key.matches(Regex("[a-zA-Z0-9-_]{43}"))) {
                if (LOG.isDebugEnabled) {
                    LOG.debug("PUT $key = $value")
                }
            }
            if (json.length() >= 5000 || approxSize > 10000000) {
                sendBatch.accept(json)
                json = JSONArray()
                approxSize = 0
            }
        }
        if (json.length() > 0) {
            sendBatch.accept(json)
        }
    }

    override fun prefetch(key: String?) {}

    override val storeCache: IDeserializingKeyValueStore
        get() = cache

    inner class SseListener(val key: String?, val keyListener: IKeyListener?) {
        private val notificationLock = Any()
        private var lastValue: String? = null
        private val sse = arrayOfNulls<Sse>(2)
        private var disposed = false
        fun dispose() {
            if (disposed) {
                return
            }
            disposed = true
            for (i in sse.indices) {
                if (sse[i] != null) {
                    sse[i]!!.sse.close()
                    sse[i] = null
                }
            }
        }

        @Synchronized
        fun ensureConnected() {
            if (disposed) {
                return
            }
            for (i in sse.indices) {
                if (sse[i] == null) {
                    continue
                }
                if (sse[i]!!.birth > System.currentTimeMillis()) {
                    sse[i]!!.birth = System.currentTimeMillis()
                }
                if (!(sse[i]!!.sse.isOpen)) {
                    sse[i] = null
                }
            }
            for (i in sse.indices) {
                // To support rebalancing after scaling the cluster a connection shouldn't be open for too long. 
                if (sse[i] != null && sse[i]!!.age > 20000) {
                    sse[i]!!.sse.close()
                    sse[i] = null
                }
            }
            val youngest = Stream.of(*sse).filter(Predicate<Sse?> { obj: Sse? -> Objects.nonNull(obj) }).mapToLong(ToLongFunction<Sse?> { it: Sse? -> it!!.birth }).reduce(0L) { a: Long, b: Long -> Math.max(a, b) }
            if (System.currentTimeMillis() - youngest < 5000) {
                return
            }
            for (i in sse.indices) {
                if (sse[i] != null) {
                    continue
                }
                val url = baseUrl + "subscribe/" + URLEncoder.encode(key, StandardCharsets.UTF_8)
                if (LOG.isTraceEnabled) {
                    LOG.trace("Connecting to $url")
                }
                val target = client.target(url)
                sse[i] = Sse(SseEventSource.target(target).reconnectingEvery(1, TimeUnit.SECONDS).build())
                sse[i]!!.sse.register(
                    Consumer { event ->
                        val value = event.readData()
                        synchronized(notificationLock) {
                            if (!((value == lastValue))) {
                                lastValue = value
                                keyListener!!.changed(key, value)
                            }
                        }
                    },
                    object : Consumer<Throwable?> {
                        override fun accept(ex: Throwable?) {
                            if (LOG.isEnabledFor(Level.ERROR)) {
                                LOG.error("", ex)
                            }
                        }
                    }
                )
                if (disposed) {
                    return
                }
                sse[i]!!.sse.open()
                if (LOG.isTraceEnabled) {
                    LOG.trace("Connected to $url")
                }
                break
            }
        }

        inner class Sse(val sse: SseEventSource) {
            var birth = System.currentTimeMillis()
            val age: Long
                get() = System.currentTimeMillis() - birth
        }
    }

    init {
        if (isEmptyString(baseUrl)) {
            baseUrl = defaultUrl
        }
        if (!(baseUrl!!.endsWith("/"))) {
            baseUrl += "/"
        }
        client = ClientBuilder.newBuilder().register(object : ClientRequestFilter {
            @Throws(IOException::class)
            override fun filter(ctx: ClientRequestContext) {
                ctx.headers.add(HttpHeaders.AUTHORIZATION, "Bearer $authToken")
            }
        }).build()
        watchDogTask = fixDelay(
            1000,
            object : Runnable {
                override fun run() {
                    var ls: List<SseListener>
                    synchronized(listeners) { ls = ArrayList(listeners) }
                    for (l: SseListener in ls) {
                        try {
                            l.ensureConnected()
                        } catch (ex: Exception) {
                            if (LOG.isEnabledFor(Level.ERROR)) {
                                LOG.error("", ex)
                            }
                        }
                    }
                }
            }
        )
    }
}
