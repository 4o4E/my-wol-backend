package top.e404.mywol.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import top.e404.mywol.data.Client
import top.e404.mywol.data.ClientSnapshot
import top.e404.mywol.data.Machine
import top.e404.mywol.data.Storage

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

internal fun Route.configureRouting() {
    route("/clients") {
        // list
        get {
            call.respond(Storage.clients)
        }
        // 注册client
        post {
            val client = call.receive<Client>()
            val exists = Storage.getClient(client.id)
            if (exists != null) exists("client exists: ${client.id}")
            Storage.use {
                data[client.id] = client
            }
            WebsocketsHandler.syncAll()
            ok()
        }
        // 删除client
        route("/{clientId}") {
            fun RoutingContext.getClient(): ClientSnapshot {
                val clientId = call.pathParameters["clientId"]!!
                return Storage.getClient(clientId) ?: notFound("unknown clientId: $clientId")
            }

            delete {
                val client = getClient()
                Storage.use {
                    data.remove(client.id)
                }
                WebsocketsHandler.syncAll()
                ok()
            }

            // devices
            route("/machines") {
                get {
                    val client = getClient()
                    call.respond(client.machines)
                }
                post {
                    val client = getClient()
                    val machine = call.receive<Machine>()
                    val exists = client.machines[machine.id]
                    if (exists != null) exists("machine exists: ${machine.id}")
                    Storage.use {
                        data[client.id]!!.machines[machine.id] = machine
                    }
                    WebsocketsHandler.syncAll()
                    ok()
                }
                put {
                    val client = getClient()
                    val machine = call.receive<Machine>()
                    client.machines[machine.id] ?: notFound("machine not found: ${machine.id}")
                    Storage.use {
                        data[client.id]!!.machines[machine.id] = machine
                    }
                    WebsocketsHandler.syncAll()
                    ok()
                }
                delete("/{machineId}") {
                    val client = getClient()
                    val machineId = call.pathParameters["machineId"]!!
                    val removed = Storage.use {
                        data[client.id]!!.machines.remove(machineId)
                    }
                    if (removed == null) {
                        notFound("machine not found: $machineId")
                    }
                    WebsocketsHandler.syncAll()
                    ok()
                }
            }
        }
    }
}