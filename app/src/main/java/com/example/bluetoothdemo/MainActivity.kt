package com.example.bluetoothdemo

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tianfy.blelib.BaseBluetoothActivity
import com.tianfy.blelib.BluetoothSearch

class MainActivity : BaseBluetoothActivity() {


    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var bluetoothSearch: BluetoothSearch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initToolbar()
        initRecyclerView()
        searchBluetooth()
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = "蓝牙"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.addSubMenu("重扫")
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        bluetoothSearch.reScan()
        return true
    }

    private fun initRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bluetoothAdapter = BluetoothAdapter()
        bluetoothAdapter.setOnAdapterItemClickListener {
            connectBle(it)
        }
        recyclerView.adapter = bluetoothAdapter
    }

    private fun searchBluetooth() {
        bluetoothSearch = BluetoothSearch.Builder(this)
            .setScanTime(1000)
            .setOnBluetoothSearchListener(object : BluetoothSearch.OnBluetoothSearchListener {
                override fun onScanStart() {
                    Toast.makeText(this@MainActivity, "开始扫描", Toast.LENGTH_LONG).show()
                    bluetoothAdapter.clearData()
                }

                override fun onScanStop() {
                    Toast.makeText(this@MainActivity, "停止扫描", Toast.LENGTH_LONG).show()
                }

                override fun onScanResult(device: BluetoothDevice) {
                    if (device.name != null) {
                        Log.d(MainActivity::class.java.simpleName, "device name=${device.name}")
                        bluetoothAdapter.setData(device.name)
                    }

                }
            }).builder()
        lifecycle.addObserver(bluetoothSearch)
    }


}