package top.e404.mywol.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import top.e404.mywol.model.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun Application.configureWebsocket() {
    install(WebSockets) {
        pingPeriod = 15.seconds.toJavaDuration()
        timeout = 15.seconds.toJavaDuration()
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        if (token != null) {
            authenticate("token") {
                websocket()
            }
        } else {
            websocket()
        }
    }
}

private fun Route.websocket() {
    webSocket("/ws") {
        val id = call.request.header("id") ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "id required"))
            return@webSocket
        }
        val name = call.request.header("name") ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "name required"))
            return@webSocket
        }
        launch(Dispatchers.IO) {
            val reason = closeReason.await()
            application.log.info("ws({}) closed: {}", name, reason?.message ?: "unknown")
            WebsocketsHandler.handlers.remove(id)
            WebsocketsHandler.syncAll()
        }
        WebsocketsHandler(id, name, this).also {
            WebsocketsHandler.handlers[id] = it
            it.loop()
        }
    }
}

class WebsocketsHandler(
    private val id: String,
    private val name: String,
    private val session: WebSocketSession
) {
    companion object {
        private val log = LoggerFactory.getLogger(WebsocketsHandler::class.java)
        val handlers = ConcurrentHashMap<String, WebsocketsHandler>()

        suspend fun syncAll() {
            val all = handlers.values.map {
                WolClient(it.id, it.name, it.machines)
            }
            log.info("current clients:${all.joinToString { client -> "\n${client.name}: ${client.machines.map { it.name }}" }}")
            for (handler in handlers.values) {
                try {
                    // 过滤客户端自己的
                    val packet = WsSyncS2c(all.filter { it.id != handler.id })
                    handler.send(packet)
                } catch (t: Throwable) {
                    log.warn("unexpected error when send sync to ${handler.name}", t)
                }
            }
        }
    }

    private val incoming by lazy { session.incoming }
    private val outgoing by lazy { session.outgoing }
    private var machines = emptyList<WolMachine>()

    // req id to resp
    private val queue = ConcurrentHashMap<String, DataContainer>()

    suspend fun loop() {
        for (frame in incoming) {
            if (frame !is Frame.Text) {
                log.warn("Unknown frame type: $frame")
                return
            }
            val text = frame.readText()
            log.debug("ws({}) recv: {}", name, text)
            val packet = ktorJson.decodeFromString(WsC2sData.serializer(), text)
            receive(packet)
        }
    }

    private suspend fun receive(packet: WsC2sData) = coroutineScope {
        launch(Dispatchers.IO) {
            if (packet.quote != null) {
                log.debug("ws quote: ${packet.id} quotes ${packet.quote}")
                val container = queue[packet.quote]
                if (container != null) {
                    container.data = packet
                }
            }
            when (packet) {
                is WsSyncC2s -> {
                    machines = packet.machines
                    syncAll()
                }
                else -> return@launch
            }
        }
    }

    suspend fun send(packet: WsS2cData) = withContext(Dispatchers.IO) {
        val json = ktorJson.encodeToString(WsS2cData.serializer(), packet)
        log.debug("ws({}) send: {}", name, json)
        outgoing.send(Frame.Text(json))
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> sendAndWaitForQuote(
        packet: WsS2cData,
        timeout: Duration = 3.seconds
    ) = withContext(Dispatchers.IO) {
        val container = DataContainer()
        queue[packet.id] = container
        send(packet)
        val start = System.currentTimeMillis()
        while (true) {
            delay(100)
            if (container.data != null) {
                queue.remove(packet.id)
                return@withContext container.data as WsC2sData
            }
            if (System.currentTimeMillis() - start > timeout.inWholeMilliseconds) {
                log.warn("timeout waiting for callback: $packet")
                queue.remove(packet.id)
                return@withContext null
            }
        }
    } as T?

    private class DataContainer {
        @Volatile
        var data: WsC2sData? = null
    }
}