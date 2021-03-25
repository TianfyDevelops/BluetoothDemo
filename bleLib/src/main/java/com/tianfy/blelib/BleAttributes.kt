package com.tianfy.blelib

import java.util.*

/**
 * 蓝牙操作参数类
 * uuid，广播action
 */
object BleAttributes {
    /* uuid */
    @JvmField
    var uuidServer = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
    @JvmField
    var uuidCharRead = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")
    @JvmField
    var uuidCharWrite = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3")
    @JvmField
    var uuidDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /* 蓝牙gatt服务连接/断开广播 */
    const val ACTION_GATT_CONNECTED = "action_gatt_connected"
    const val ACTION_GATT_DISCONNECTED = "action_gatt_disconnected"

    /* 蓝牙初始化完成 */
    const val ACTION_BLE_INIT_FINISH = "action_ble_init_finish"
}