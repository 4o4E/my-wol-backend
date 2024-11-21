package top.e404.mywol.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import top.e404.mywol.model.WsWolC2s
import top.e404.mywol.model.WsWolS2c

fun Application.configureRouting() {
    routing {
        if (token != null) {
            authenticate("token") {
                configureRouting()
            }
        } else {
            configureRouting()
        }
    }
}

@Serializable
data class WolReq(
    val clientId: String,
    val machineId: String
)

internal fun Route.configureRouting() {
    post("/wol") {
        val req = call.receive<WolReq>()
        val handler = WebsocketsHandler.handlers[req.clientId] ?: fail("客户端未连接")
        val resp = handler.sendAndWaitForQuote<WsWolC2s>(WsWolS2c(req.machineId)) ?: fail("连接超时")
        if (!resp.success) notFound(resp.message)
        ok()
    }
}