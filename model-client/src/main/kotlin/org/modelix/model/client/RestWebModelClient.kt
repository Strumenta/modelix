package org.modelix.model.client

import org.apache.commons.io.FileUtils
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.json.JSONArray
import org.json.JSONObject
import org.modelix.model.IKeyListener
import org.modelix.model.IKeyValueStore
import org.modelix.model.KeyValueStoreCache
import org.modelix.model.lazy.IDeserializingKeyValueStore
import org.modelix.model.lazy.ObjectStoreCache
import org.modelix.model.persistent.HashUtil
import org.modelix.model.util.StreamUtils
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.*
import java.util.stream.Stream
import javax.ws.rs.client.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.sse.InboundSseEvent
import javax.ws.rs.sse.SseEventSource

class RestWebModelClient @JvmOverloads constructor(val baseUrl: String? = null) : IModelClient {
    companion object {
        private val LOG: Logger = LogManager.getLogger(RestWebModelClient::class.java)
        val MODEL_URI_VAR_NAME: String = "MODEL_URI"
        private var defaultToken: String? = null
        val modelUrlFromEnv: String?
            get() {
                var url: String? = System.getProperty(MODEL_URI_VAR_NAME)
                if ((url == null || url.length == 0)) {
                    url = System.getenv(MODEL_URI_VAR_NAME)
                }
                return url
            }

        val defaultUrl: String
            get() {
                val urlFromEnv: String? = modelUrlFromEnv
                if ((urlFromEnv != null && urlFromEnv.length > 0)) {
                    return urlFromEnv
                } else {
                    return "http://modelix.q60.de:80/model/"
                }
            }

        private fun isEmptyString(str: String?): Boolean {
            return str == null || str.length == 0
        }

        init {
            try {
                val sharedSecretFile: File = File("/secrets/modelsecret/modelsecret.txt")
                if (org.modelix.model.client.sharedSecretFile.exists()) {
                    defaultToken = FileUtils.readFileToString(org.modelix.model.client.sharedSecretFile, StandardCharsets.UTF_8)
                }
            } catch (ex: Exception) {
                if (LOG.isEnabledFor(Level.ERROR)) {
                    LOG.error("Failed to load default token", ex)
                }
            }
        }
    }

    override var clientId: Int = 0
        get() {
            if (field == 0) {
                val response: Response = client.target(baseUrl + "counter/clientId").request().post(Entity.text(""))
                val idStr: String = response.readEntity(String::class.java)
                field = idStr.toInt()
            }
            return field
        }
        private set
    private val client: Client
    private val listeners: MutableList<SseListener> = ArrayList()
    override val asyncStore: IKeyValueStore = GarbageFilteringStore(AsyncStore(this))
    private val cache: ObjectStoreCache = ObjectStoreCache(KeyValueStoreCache(asyncStore))

    @get:Synchronized
    override var idGenerator: IdGenerator? = null
        get() {
            if (field == null) {
                field = IdGenerator(clientId)
            }
            return field
        }
        private set
    private val watchDogTask: ScheduledFuture<*>?
    private var authToken: String? = defaultToken

    fun dispose() {
        watchDogTask!!.cancel(false)
        synchronized(listeners, {
            listeners.forEach(Consumer({ obj: SseListener -> obj.dispose() }))
            listeners.clear()
        })
    }

    public override fun get(key: String?): String? {
        val isHash: Boolean = HashUtil.isSha256(key)
        if (isHash) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("GET " + key)
            }
        }
        val start: Long = System.currentTimeMillis()
        val response: Response = client.target(baseUrl + "get/" + URLEncoder.encode(key, StandardCharsets.UTF_8)).request().buildGet().invoke()
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            val value: String = response.readEntity(String::class.java)
            val end: Long = System.currentTimeMillis()
            if (isHash) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("GET " + key + " took " + (end - start) + " ms: " + value)
                }
            }
            return value
        } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return null
        } else {
            throw RuntimeException("Request for key '" + key + "' failed: " + response.getStatusInfo())
        }
    }

    public override fun getAll(keys: Iterable<String?>?): Map<String?, String?>? {
        if (!keys!!.iterator().hasNext()) {
            return HashMap()
        }
        val json: JSONArray = JSONArray()
        for (key: String? in keys) {
            json.put(key)
        }
        val body: String = json.toString()
        val start: Long = System.currentTimeMillis()
        val response: Response = client.target(baseUrl + "getAll").request(MediaType.APPLICATION_JSON).put(Entity.text(body))
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            val jsonStr: String = response.readEntity(String::class.java)
            val responseJson: JSONArray = JSONArray(jsonStr)
            val result: MutableMap<String?, String?> = LinkedHashMap(16, 0.75.toFloat(), false)
            for (entry_: Any in responseJson) {
                val entry: JSONObject = entry_ as JSONObject
                result.put(entry.getString("key"), entry.optString("value"))
            }
            val end: Long = System.currentTimeMillis()
            return result
        } else {
            throw RuntimeException(String.format("Request for %d keys failed (%s, ...): %s",
                    keys.spliterator().getExactSizeIfKnown(),
                    StreamUtils.toStream(keys).findFirst().orElse(null),
                    response.getStatusInfo()))
        }
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    val email: String
        get() {
            val response: Response = client.target(baseUrl + "getEmail").request().buildGet().invoke()
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response.readEntity(String::class.java)
            } else {
                throw RuntimeException("Request for e-mail address failed: " + response.getStatusInfo())
            }
        }

    public override fun listen(key: String?, keyListener: IKeyListener?) {
        val sseListener: SseListener = SseListener(key, keyListener)
        synchronized(listeners, { listeners.add(sseListener) })
    }

    public override fun removeListener(key: String?, listener: IKeyListener?) {
        synchronized(listeners, { listeners.removeIf(Predicate({ it: SseListener -> Objects.equals(it.key, key) && it.keyListener === listener })) })
    }

    public override fun put(key: String?, value: String?) {
        if (!(key!!.matches("[a-zA-Z0-9-_]{43}"))) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("PUT " + key + " = " + value)
            }
        }
        val response: Response = client.target(baseUrl + "put/" + URLEncoder.encode(key, StandardCharsets.UTF_8)).request(MediaType.TEXT_PLAIN).put(Entity.text(value))
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw RuntimeException("Failed to store entry (" + response.getStatusInfo() + ") " + key + " = " + value)
        }
    }

    public override fun putAll(entries: Map<String?, String?>?) {
        val sendBatch: Consumer<JSONArray> = object : Consumer<JSONArray> {
            public override fun accept(json: JSONArray) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("PUT batch of " + json.length() + " entries")
                }
                val response: Response = client.target(baseUrl + "putAll").request(MediaType.APPLICATION_JSON).put(Entity.text(json.toString()))
                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    throw RuntimeException(String.format("Failed to store %d entries (%s) %s",
                            entries!!.size,
                            response.getStatusInfo(),
                            entries.entries.stream().map(Function({ e: Map.Entry<String?, String?> -> e.key.toString() + " = " + e.value + ", ..." })).findFirst().orElse("")))
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("PUT " + entries!!.size + " entries")
        }
        var json: JSONArray = JSONArray()
        var approxSize: Int = 0
        for (entry: Map.Entry<String?, String?> in entries!!.entries) {
            val jsonEntry: JSONObject = JSONObject()
            jsonEntry.put("key", entry.key)
            jsonEntry.put("value", entry.value)
            approxSize += entry.key!!.length
            approxSize += entry.value!!.length
            json.put(jsonEntry)
            if (!(entry.key!!.matches(Regex("[a-zA-Z0-9-_]{43}")))) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("PUT " + entry.key + " = " + entry.value)
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

    public override fun prefetch(key: String) {}

    override val storeCache: IDeserializingKeyValueStore
        get() {
            return cache
        }

    inner class SseListener constructor(val key: String?, val keyListener: IKeyListener?) {
        private val notificationLock: Any = Any()
        private var lastValue: String? = null
        private val sse: Array<Sse?> = arrayOfNulls(2)
        private var disposed: Boolean = false
        fun dispose() {
            if (disposed) {
                return
            }
            disposed = true
            for (i in sse.indices) {
                if (sse.get(i) != null) {
                    sse.get(i)!!.sse.close()
                    sse.get(i) = null
                }
            }
        }

        @Synchronized
        fun ensureConnected() {
            if (disposed) {
                return
            }
            for (i in sse.indices) {
                if (sse.get(i) == null) {
                    continue
                }
                if (sse.get(i)!!.birth > System.currentTimeMillis()) {
                    sse.get(i)!!.birth = System.currentTimeMillis()
                }
                if (!(sse.get(i)!!.sse.isOpen())) {
                    sse.get(i) = null
                }
            }
            for (i in sse.indices) {
                // To support rebalancing after scaling the cluster a connection shouldn't be open for too long. 
                if (sse.get(i) != null && sse.get(i)!!.age > 20000) {
                    sse.get(i)!!.sse.close()
                    sse.get(i) = null
                }
            }
            val youngest: Long = Stream.of(*sse).filter(Predicate<Sse>({ obj: Sse? -> Objects.nonNull(obj) })).mapToLong(ToLongFunction<Sse>({ it: Sse -> it.birth })).reduce(0L, LongBinaryOperator({ a: Long, b: Long -> Math.max(a, b) }))
            if (System.currentTimeMillis() - youngest < 5000) {
                return
            }
            for (i in sse.indices) {
                if (sse.get(i) != null) {
                    continue
                }
                val url: String = baseUrl + "subscribe/" + URLEncoder.encode(key, StandardCharsets.UTF_8)
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Connecting to " + url)
                }
                val target: WebTarget = client.target(url)
                sse.get(i) = Sse(SseEventSource.target(target).reconnectingEvery(1, TimeUnit.SECONDS).build())
                sse.get(i)!!.sse.register(object : Consumer<InboundSseEvent> {
                    public override fun accept(event: InboundSseEvent) {
                        val value: String = event.readData()
                        synchronized(notificationLock, {
                            if (!(Objects.equals(value, lastValue))) {
                                lastValue = value
                                keyListener!!.changed(key, value)
                            }
                        })
                    }
                }, object : Consumer<Throwable?> {
                    public override fun accept(ex: Throwable?) {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("", ex)
                        }
                    }
                })
                if (disposed) {
                    return
                }
                sse.get(i)!!.sse.open()
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Connected to " + url)
                }
                break
            }
        }

        inner class Sse constructor(val sse: SseEventSource) {
            var birth: Long = System.currentTimeMillis()
            val age: Long
                get() {
                    return System.currentTimeMillis() - birth
                }

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
            public override fun filter(ctx: ClientRequestContext) {
                ctx.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
            }
        }).build()
        watchDogTask = SharedExecutors.fixDelay(1000, object : Runnable {
            public override fun run() {
                // initialization necessary for the compiler...
                var ls: List<SseListener> = emptyList()
                synchronized(listeners, { ls = ArrayList(listeners) })
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
        })
    }
}