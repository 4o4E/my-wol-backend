package top.e404.mywol.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import top.e404.mywol.model.*

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

@Serializable
data class SshReq(
    val clientId: String,
    val machineId: String,
    val command: String,
)

internal fun Route.configureRouting() {
    post("/wol") {
        val req = call.receive<WolReq>()
        val handler = WebsocketsHandler.handlers[req.clientId] ?: fail("客户端未连接")
        val resp = handler.sendAndWaitForQuote<WsWolC2s>(WsWolS2c(req.machineId)) ?: fail("连接超时")
        if (!resp.success) notFound(resp.message)
        ok()
    }
    post("/ssh") {
        val req = call.receive<SshReq>()
        val handler = WebsocketsHandler.handlers[req.clientId] ?: fail("客户端未连接")
        val resp = handler.sendAndWaitForQuote<WsSshC2s>(WsSshS2c(req.machineId, req.command)) ?: fail("连接超时")
        if (!resp.success) notFound(resp.message)
        call.respond(resp.result!!)
    }
    post("/ssh/history") {
        val req = call.receive<WolReq>()
        val handler = WebsocketsHandler.handlers[req.clientId] ?: fail("客户端未连接")
        val resp = handler.sendAndWaitForQuote<WsSshHistoryC2s>(WsSshHistoryS2c(req.machineId)) ?: fail("连接超时")
        if (!resp.success) notFound(resp.message)
        call.respond(resp.history)
    }
    post("/ssh/shutdown") {
        val req = call.receive<WolReq>()
        val handler = WebsocketsHandler.handlers[req.clientId] ?: fail("客户端未连接")
        val resp = handler.sendAndWaitForQuote<WsSshShutdownC2s>(WsSshShutdownS2c(req.machineId)) ?: fail("连接超时")
        if (!resp.success) notFound(resp.message)
        call.respond(resp.result!!)
    }
}