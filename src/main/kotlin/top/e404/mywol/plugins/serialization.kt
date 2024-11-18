package top.e404.mywol.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(ktorJson)
    }
}

val ktorJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}