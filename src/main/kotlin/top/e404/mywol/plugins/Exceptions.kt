package top.e404.mywol.plugins

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


class HttpStatusException(
    val statusCode: HttpStatusCode,
    override val message: String = statusCode.description,
) : Exception()

@DslMarker
annotation class HttpResponseMarker

suspend fun RoutingContext.ok() = call.respond(HttpStatusCode.OK)

@HttpResponseMarker
fun fail(status: HttpStatusCode, message: String? = null): Nothing {
    throw HttpStatusException(status, message ?: status.description)
}

@HttpResponseMarker
fun notFound(message: String = ""): Nothing = fail(HttpStatusCode.NotFound, message)

@HttpResponseMarker
fun exists(message: String = ""): Nothing = fail(HttpStatusCode.Conflict, message)