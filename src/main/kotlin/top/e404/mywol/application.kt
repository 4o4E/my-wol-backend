package top.e404.mywol

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import sun.misc.Signal
import top.e404.mywol.data.Storage
import top.e404.mywol.plugins.*
import kotlin.system.exitProcess

fun main() {
    Storage.load()
    Signal.handle(Signal("INT")) {
        Storage.shutdown()
        exitProcess(0)
    }
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