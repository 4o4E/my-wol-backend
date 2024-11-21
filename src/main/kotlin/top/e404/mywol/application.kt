package top.e404.mywol

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import top.e404.mywol.plugins.*

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv()["port"]?.toIntOrNull() ?: 8080,
        host = System.getenv()["host"]?.ifBlank { null } ?: "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureLog()
    configureStatuePages()
    configureWebsocket()
    configureRouting()
}