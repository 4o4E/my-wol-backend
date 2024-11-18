package top.e404.mywol.data

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import top.e404.mywol.plugins.ktorJson
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object Storage {
    private val log = LoggerFactory.getLogger(Storage::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler({ _, e ->
        log.warn("Storage error", e)
    }))
    private const val FILE_PATH = "data.json"
    private val file by lazy { File(FILE_PATH) }
    @Volatile
    private var job: Job? = null
    private val data: MutableMap<String, Client> = ConcurrentHashMap()

    sealed interface DataProvider {
        val data get() = Storage.data
    }

    private data object DataProviderImpl : DataProvider

    suspend fun <T> use(operator: DataProvider.() -> T): T {
        try {
            return operator(DataProviderImpl)
        } finally {
            scheduleSave()
        }
    }

    fun getClient(id: String) = data[id]?.snapshot

    val clients get() = data.values.map { it.snapshot }

    private suspend fun scheduleSave() {
        if (job != null) return
        job = scope.launch(Dispatchers.IO) {
            // 1分钟后保存变更
            delay(60 * 1000)
            file.writeText(ktorJson.encodeToString(data))
            job = null
        }
    }

    fun load() {
        if (!file.exists()) return
        data.putAll(ktorJson.decodeFromString(file.readText()))
    }

    fun shutdown() {
        log.info("shutdown, save storage data")
        try {
            file.writeText(ktorJson.encodeToString(data))
        } catch (e: Throwable) {
            log.warn("Save data error, data: {}", data, e)
        }
        job?.cancel()
    }
}

@Serializable
data class Client(
    val id: String,
    var name: String,
    val machines: MutableMap<String, Machine>,
)

internal val Client.snapshot get() = ClientSnapshot(id, name, machines.map { it.key to it.value.snapshot }.toMap())

@Serializable
data class ClientSnapshot(
    val id: String,
    val name: String,
    val machines: Map<String, MachineSnapshot>,
)

@Serializable
data class Machine(
    var id: String,
    var name: String,
    var deviceIp: String,
    var broadcastIp: String,
    var mac: String,
    var time: Long,
)

internal val Machine.snapshot get() = MachineSnapshot(id, name, deviceIp, broadcastIp, mac, time)

@Serializable
data class MachineSnapshot(
    val id: String,
    val name: String,
    val deviceIp: String,
    val broadcastIp: String,
    val mac: String,
    val time: Long,
)