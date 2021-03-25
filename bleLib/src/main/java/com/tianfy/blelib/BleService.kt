package com.tianfy.blelib

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import java.util.*
import kotlin.experimental.and

/**
 * 蓝牙连接通信服务类
 */
class BleService : Service() {
    private val TAG = BleService::class.java.name
    private var connectionState = STATE_DISCONNECTED //连接状态
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var characteristicRead: BluetoothGattCharacteristic? = null
    private var characteristicWrite: BluetoothGattCharacteristic? = null
    private var bluetoothAddress: String? = null

    val handler = Handler(this.mainLooper)

    private val binder: IBinder = BleServiceBind()

    inner class BleServiceBind : Binder() {
        val service: BleService
            get() = this@BleService
    }

    override fun onBind(intent: Intent): IBinder? {
        initialize()
        return binder
    }

    /**
     * 初始化bluetoothManager bluetoothAdapter
     */
    fun initialize() {
        Log.d(TAG, "initialize()")
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager!!.adapter
        }
    }

    /**
     * 根据蓝牙设备地址连接设备
     *
     * @param address    设备地址
     * @return true 成功调用连接，false 调用连接失败，连接结果通过回调（gattCallback）的方式返回
     */
    fun connect(address: String?): Boolean {
        if (bluetoothAdapter == null || address == null) {
            return false
        }
        if (address == bluetoothAddress && bluetoothGatt != null) {
            /* 如果当前的设备和上次连接设备相同，则尝试重新连接 */
            return if (bluetoothGatt!!.connect()) {
                connectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }
        /* 通过mac地址得到设备 */
        bluetoothDevice = bluetoothAdapter!!.getRemoteDevice(address)
        if (bluetoothDevice == null) {
            return false
        }
        /* 连接蓝牙设备 */
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice!!.connectGatt(
                applicationContext, false,
                gattCallback, BluetoothDevice.TRANSPORT_LE
            )
        } else {
            bluetoothDevice!!.connectGatt(applicationContext, true, gattCallback)
        }
        bluetoothAddress = address
        connectionState = STATE_CONNECTING
        return true
    }

    /**
     * 断开蓝牙连接
     */
    fun closeBleConnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt!!.disconnect()
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        /* 连接状态改变 */
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.")
                Log.i(
                    TAG,
                    "Attempting to start service discovery:" + bluetoothGatt!!.discoverServices()
                )
                /* 连接成功 */broadcastUpdate(BleAttributes.ACTION_GATT_CONNECTED)
                connectionState = STATE_CONNECTED
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                /* 断开连接 */
                Log.i(TAG, "Disconnected from GATT server.")
                close()
                broadcastUpdate(BleAttributes.ACTION_GATT_DISCONNECTED)
            }
        }

        /* 发现新的服务 */
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.w(TAG, "New services discovered ")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                initCharacteristic()
            }
        }

        /* 特征值改变回掉，即收到数据 */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            handleReceiveData(characteristic.value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.d(TAG, "onDescriptorWrite: " + "设置成功")
            /* Descriptor初始化完成，发送完成初始化完成的广播 */
            broadcastUpdate(BleAttributes.ACTION_BLE_INIT_FINISH)
            /* 保存蓝牙设备的名字 */
            getSharedPreferences(SpAttributes.BLE_SP_NAME, Context.MODE_PRIVATE).edit()
                .putString(SpAttributes.BLE_DEVICE_NAME, gatt.device.name).apply()
        }
    }

    /**
     * 初始化Characteristic
     */
    @Synchronized
    fun initCharacteristic() {
        if (bluetoothGatt == null) throw NullPointerException()
        /* 根据serviceUuid得到service */
        val service = bluetoothGatt!!.getService(BleAttributes.uuidServer)
        /* 根据Characteristic 的uuid得到 读characteristicRead和写characteristicWrite */
        characteristicRead = service.getCharacteristic(BleAttributes.uuidCharRead)
        characteristicWrite = service.getCharacteristic(BleAttributes.uuidCharWrite)
        characteristicWrite!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        if (characteristicRead == null) throw NullPointerException()
        if (characteristicWrite == null) throw NullPointerException()
        /* 设置Characteristic，远程设备设置Characteristic改变，会回掉onCharacteristicChanged()方法 */
        bluetoothGatt!!.setCharacteristicNotification(
            characteristicRead,
            true
        )
        val descriptor = characteristicRead!!.getDescriptor(BleAttributes.uuidDescriptor)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothGatt!!.writeDescriptor(descriptor)
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    /**
     * 发送数据
     *
     * @param value 数据
     */
    fun sendValue(value: ByteArray) {
        if (bluetoothGatt != null) {
            /* 发送数据 */
            characteristicWrite!!.value = value
            val b = bluetoothGatt!!.writeCharacteristic(characteristicWrite)
            byteToHex("writeCharacteristic: " + b + "发送数据：", value)
        }
    }

    /**
     * 收到数据
     *
     * @param receiveData 收到的数据
     */
    private fun handleReceiveData(receiveData: ByteArray) {
        val message = Message.obtain()
        message.obj = receiveData
        handler.sendMessage(message)
    }

    fun close() {
        if (bluetoothGatt != null) {
            Log.d(TAG, "mBluetoothGatt closed")
            bluetoothGatt!!.close()
            bluetoothGatt = null
        }
    }

    /**
     * 返回蓝牙是否是连接状态
     *
     * @return true 已连接，false 未连接
     */
    val connectState: Boolean
        get() = connectionState == STATE_CONNECTED

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        if (connectState) {     //如果是连接状态，则断开连接
            closeBleConnect()
        }
    }

    /**
     * byte转16进制字符串，在控制台打印日志，方便调试
     *
     * @param title title
     * @param value 要转换的byte数组
     */
    private fun byteToHex(title: String, value: ByteArray) {
        val builder = StringBuilder()
        for (aValue in value) {
            val a = Integer.toHexString((aValue and 0xFF.toByte()).toInt()).toUpperCase(Locale.ROOT)
            if (a.length == 1) {
                builder.append("0")
            }
            builder.append(a)
            builder.append(" ")
        }
        Log.d(TAG, title + builder.toString())
    }

    companion object {
        private const val STATE_DISCONNECTED = 0 //断开连接
        private const val STATE_CONNECTING = 1 //连接中
        private const val STATE_CONNECTED = 2 //已连接
    }
}