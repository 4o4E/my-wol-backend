package top.e404.mywol.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import top.e404.mywol.data.ClientSnapshot
import top.e404.mywol.data.Storage
import top.e404.mywol.model.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebsocket() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
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
        val client = Storage.getClient(id) ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "client not found"))
            return@webSocket
        }
        WebsocketsHandler(client, this).also {
            WebsocketsHandler.handlers[id] = it
            it.loop()
        }
    }
}

class WebsocketsHandler(
    private val client: ClientSnapshot,
    private val session: WebSocketSession
) {
    companion object {
        private val log = LoggerFactory.getLogger(WebsocketsHandler::class.java)
        val handlers = ConcurrentHashMap<String, WebsocketsHandler>()

        suspend fun syncAll(clients: List<ClientSnapshot> = Storage.clients) {
            val packet = getSyncS2cPacket(clients)
            for (handler in handlers.values) {
                try {
                    handler.send(packet)
                } catch (t: Throwable) {
                    log.warn("unexpected error when send sync to ${handler.client.name}", t)
                }
            }
        }

        private fun getSyncS2cPacket(clients: List<ClientSnapshot>) = WsSyncS2c(clients.map { client ->
            WolClient(
                client.id,
                client.name,
                client.machines.values.map { machine ->
                    WolMachine(
                        machine.id,
                        machine.name,
                        machine.mac,
                        machine.deviceIp,
                        machine.broadcastIp,
                        handlers[client.id]?.machineInfo?.get(machine.id) ?: MachineState.UNKNOWN
                    )
                },
                if (handlers.containsKey(client.id)) ClientState.ONLINE
                else ClientState.OFFLINE
            )
        })
    }

    private val incoming by lazy { session.incoming }
    private val outgoing by lazy { session.outgoing }
    private val machineInfo = mutableMapOf<String, MachineState>()

    // req id to resp
    private val queue = ConcurrentHashMap<String, DataContainer>()

    suspend fun loop() {
        for (frame in incoming) {
            if (frame !is Frame.Text) {
                log.warn("Unknown frame type: $frame")
                return
            }
            val text = frame.readText()
            val packet = ktorJson.decodeFromString(WsC2sData.serializer(), text)
            log.debug("ws({}) recv: {}", client.name, packet)
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
                is WsWolResp -> return@launch
                is WsWolReq -> {
                    Storage.getClient(packet.clientId)?.machines?.get(packet.machineId)?.let { machine ->
                        log.debug("ws wol: ${packet.clientId} wol ${packet.machineId}")
                        val resp = sendAndWaitForQuote<WsWolResp>(WsWolReq(packet.clientId, packet.machineId))
                        if (resp == null) {
                            send(WsWolResp(packet.id, false, "timeout"))
                            return@launch
                        }
                        send(WsWolResp(packet.id, resp.success, resp.message))
                    }
                }

                is WsSyncMachineState -> {
                    for ((id, state) in packet.machines) {
                        machineInfo[id] = state
                    }
                }
            }
        }
    }

    suspend fun send(packet: WsS2cData) = withContext(Dispatchers.IO) {
        val json = ktorJson.encodeToString(WsS2cData.serializer(), packet)
        log.debug("ws({}) send: {}", client.name, json)
        outgoing.send(Frame.Text(json))
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> sendAndWaitForQuote(
        packet: WsS2cData,
        timeout: Duration = 10.seconds
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
        var data: WsC2sData? = null
    }
}