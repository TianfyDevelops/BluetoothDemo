package com.example.bluetoothdemo

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycle.addObserver(BluetoothSearch(this,object :BluetoothSearch.OnBluetoothSearchListener{
            override fun onScanStart() {
                TODO("Not yet implemented")
            }

            override fun onScanStop() {
                TODO("Not yet implemented")
            }

            override fun onScanResult(device: BluetoothDevice) {
                TODO("Not yet implemented")
            }

        }))
    }

}