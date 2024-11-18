package top.e404.mywol.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.logging.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

fun Application.configureStatuePages() {
    install(StatusPages) {
        exception<Throwable> { call, throwable ->
            when (throwable) {
                is BadRequestException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        generateMessage(throwable).orEmpty()
                    )
                }
                is HttpStatusException -> {
                    if (throwable.statusCode.value in 200..299) {
                        call.respond(throwable.statusCode, throwable.message)
                        return@exception
                    }
                    if (throwable.statusCode.value in 500..599) {
                        call.application.log.error(throwable)
                    }
                    call.respond(throwable.statusCode, throwable.message)
                }

                else -> {
                    call.application.log.error(throwable)
                    call.respond(HttpStatusCode.InternalServerError, "Internal server error")
                }
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun generateMessage(throwable: Throwable): String? {
    throwable.findCauseByType<MissingFieldException>()?.let { e ->
        return buildString {
            append("Fields ")
            append(e.missingFields.joinToString { "'$it'" })
            if (e.missingFields.size == 1) {
                append(" is required")
            } else {
                append(" are required")
            }
        }
    }

    return null
}

val Throwable.causes: Sequence<Throwable>
    get() = sequence {
        var rootCause: Throwable? = this@causes
        while (rootCause?.cause != null) {
            yield(rootCause.cause!!)
            rootCause = rootCause.cause
        }
    }

inline fun <reified T> Throwable.findCauseByType(): T? {
    return findCause { it is T } as T?
}


fun Throwable.findCause(predicate: (Throwable) -> Boolean): Throwable? {
    return causes.firstOrNull(predicate)
}