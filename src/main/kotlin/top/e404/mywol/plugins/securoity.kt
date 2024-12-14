package top.e404.mywol.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

internal val token = System.getenv()["token"]?.ifBlank { null }

fun Application.configureSecurity() {
    authentication {
        bearer(name = "token") {
            realm = "mywol"
            authenticate { credentials ->
                if (credentials.token == token) UserIdPrincipal(token)
                else null
            }
        }
    }
}