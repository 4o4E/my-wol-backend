package top.e404.mywol.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

interface PacketData {
    /**
     * packet唯一id
     */
    val id: String
}

@Serializable
sealed interface WsC2sData : PacketData

@Serializable
sealed interface WsS2cData : PacketData

@Serializable
sealed interface WsC2sDataWithQuote : WsC2sData {
    /**
     * 回复的packet的id
     */
    val quote: String
}

/**
 * 上传本机machine列表
 */
@SerialName("sync-c2s")
@Serializable
data class WsSyncC2s(
    val machines: List<WolMachine>,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sData

/**
 * 更新远程设备列表
 */
@SerialName("sync-s2c")
@Serializable
data class WsSyncS2c(
    val clients: List<WolClient>,
    override val id: String = UUID.randomUUID().toString(),
) : WsS2cData

/**
 * 发送开机数据包 请求
 */
@SerialName("wol-s2c")
@Serializable
data class WsWolS2c(
    val machineId: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsS2cData

/**
 * 发送开机数据包 响应
 */
@SerialName("wol-c2s")
@Serializable
data class WsWolC2s(
    val success: Boolean,
    val message: String,
    override val quote: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sDataWithQuote

/**
 * 发送ssh命令 请求
 */
@SerialName("ssh-s2c")
@Serializable
data class WsSshS2c(
    val machineId: String,
    val command: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsS2cData

/**
 * 发送ssh命令 响应
 */
@SerialName("ssh-c2s")
@Serializable
data class WsSshC2s(
    val success: Boolean,
    val message: String,
    val result: SshResult?,
    override val quote: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sDataWithQuote

/**
 * 同步ssh命令历史 请求
 */
@SerialName("ssh-history-s2c")
@Serializable
data class WsSshHistoryS2c(
    val machineId: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsS2cData

/**
 * 同步ssh命令历史 响应
 */
@SerialName("ssh-history-c2s")
@Serializable
data class WsSshHistoryC2s(
    val success: Boolean,
    val message: String,
    val history: List<SshHistory>,
    override val quote: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sDataWithQuote

/**
 * ssh关机 请求
 */
@SerialName("ssh-shutdown-s2c")
@Serializable
data class WsSshShutdownS2c(
    val machineId: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsS2cData

/**
 * ssh关机 响应
 */
@SerialName("ssh-shutdown-c2s")
@Serializable
data class WsSshShutdownC2s(
    val success: Boolean,
    val message: String,
    val result: SshResult?,
    override val quote: String,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sDataWithQuote

@Serializable
data class SshHistory(
    val command: String,
    val result: SshResult
)

@Serializable
data class SshResult(
    val exitStatus: Int,
    val out: String,
    val err: String,
)

/**
 * 运行了app的设备
 */
@Serializable
data class WolClient(
    val id: String,
    val name: String,
    val machines: List<WolMachine>
)

/**
 * 配置了mac和ip的网络设备
 */
@Serializable
data class WolMachine(
    val id: String,
    val name: String,
    val time: Long,
    val state: MachineState,
    val isSshConfigured: Boolean,
    val canShutdown: Boolean
)

/**
 * 网络设备状态
 */
@Serializable
@Suppress("UNUSED")
enum class MachineState {
    /**
     * 开机
     */
    ON,

    /**
     * 没开机
     */
    OFF,

    /**
     * 由于client掉线所以不知道开没没开机
     */
    UNKNOWN
}