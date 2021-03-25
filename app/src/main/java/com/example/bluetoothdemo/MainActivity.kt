package com.example.bluetoothdemo

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import com.tianfy.blelib.BaseBluetoothActivity
import com.tianfy.blelib.BluetoothSearch

class MainActivity : BaseBluetoothActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchBluetooth()
    }
    private fun searchBluetooth() {
        val bluetoothSearch = BluetoothSearch.Builder(this)
            .setScanTime(1000)
            .setOnBluetoothSearchListener(object : BluetoothSearch.OnBluetoothSearchListener {
                override fun onScanStart() {
                    TODO("Not yet implemented")
                }

                override fun onScanStop() {
                    TODO("Not yet implemented")
                }

                override fun onScanResult(device: BluetoothDevice) {
                    TODO("Not yet implemented")
                }
            }).builder()
        lifecycle.addObserver(bluetoothSearch)
    }



}