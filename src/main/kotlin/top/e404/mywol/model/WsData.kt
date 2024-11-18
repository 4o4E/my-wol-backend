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
 * 上传wol列表
 */
@SerialName("sync-machine-state")
@Serializable
data class WsSyncMachineState(
    val machines: Map<String, MachineState>,
    override val quote: String? = null,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sData

/**
 * 下发设备和wol列表
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
@SerialName("wol-req")
@Serializable
data class WsWolReq(
    val clientId: String,
    val machineId: String,
    override val quote: String? = null,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sData, WsS2cData

/**
 * 发送开机数据包 响应
 */
@SerialName("wol-resp")
@Serializable
data class WsWolResp(
    val origin: String,
    val success: Boolean,
    val message: String,
    override val quote: String? = null,
    override val id: String = UUID.randomUUID().toString(),
) : WsC2sData, WsS2cData

/**
 * 运行了app的设备
 */
@Serializable
data class WolClient(
    val id: String,
    val name: String,
    val machines: List<WolMachine>,
    val state: ClientState
)

/**
 * 配置了mac和ip的网络设备
 */
@Serializable
data class WolMachine(
    val id: String,
    val name: String,
    val mac: String,
    val deviceIp: String,
    val broadcastIp: String,
    val state: MachineState
)

/**
 * app客户端状态
 */
@Serializable
enum class ClientState {
    /**
     * client在线
     */
    ONLINE,
    /**
     * client离线
     */
    OFFLINE
}

/**
 * 网络设备状态
 */
@Serializable
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