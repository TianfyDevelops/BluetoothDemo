package com.tianfy.blelib

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity

/**
 *
 * @ProjectName:    BluetoothDemo
 * @Package:        com.example.bluetoothdemo
 * @ClassName:      BaseBluetoothActivity
 * @Description:     连接蓝牙服务的基类
 * @Author:         tianfy
 * @CreateDate:     2021/3/25 14:56
 * @UpdateUser:     更新者：
 * @UpdateDate:     2021/3/25 14:56
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */
open class BaseBluetoothActivity : AppCompatActivity() {

    var bleService: BleService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val bind: BleService.BleServiceBind = service as BleService.BleServiceBind
            bleService = bind.service
            if (bleService != null) {
                val receiverData = bleService!!.handler.obtainMessage() as ByteArray
                handlerReceiverData(receiverData)
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleService = null
        }

    }

    /**
     * 处理接收数据的逻辑
     * @param receiverData 接收的数据
     */
    private fun handlerReceiverData(receiverData: ByteArray) {

    }

    /**
     * 发送数据
     * @param sendData 发送的数据
     */
    fun sendData(sendData: ByteArray) {
        if (bleService != null) {
            bleService!!.sendValue(sendData)
        }
    }

    /**
     * 连接到蓝牙设备
     * @param address 蓝牙设备地址
     */
    fun connectBle(address: String) {
        if (bleService != null) {
            bleService!!.connect(address)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        unBindService()
    }

    /**
     * 绑定服务
     */
    private fun bindService() {
        val intent = Intent(this, BleService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    /**
     * 解绑服务
     */
    private fun unBindService() {
        if (bleService != null) {
            unbindService(serviceConnection)
        }
    }
}