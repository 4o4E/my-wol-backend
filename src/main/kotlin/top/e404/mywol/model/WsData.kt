package top.e404.mywol.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

interface PacketData {
    /**
     * packet唯一id
     */
    val id: String

    /**
     * 回复的packet的id
     */
    val quote: String?
}

@Serializable
sealed interface WsC2sData : PacketData

@Serializable
sealed interface WsS2cData : PacketData

/**
 * 上传本机machine列表
 */
@SerialName("sync-c2s")
@Serializable
data class WsSyncC2s(
    val machines: List<WolMachine>,
    override val quote: String? = null,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sData

/**
 * 更新远程设备列表
 */
@SerialName("sync-s2c")
@Serializable
data class WsSyncS2c(
    val clients: List<WolClient>,
    override val quote: String? = null,
    override val id: String = UUID.randomUUID().toString(),
) : WsS2cData

/**
 * 发送开机数据包 请求
 */
@SerialName("wol-s2c")
@Serializable
data class WsWolS2c(
    val machineId: String,
    override val quote: String? = null,
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
    override val quote: String? = null,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sData

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
    val state: MachineState
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