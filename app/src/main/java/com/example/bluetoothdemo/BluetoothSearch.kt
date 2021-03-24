package com.example.bluetoothdemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 *
 * @ProjectName:    BluetoothDemo
 * @Package:        com.example.bluetoothdemo
 * @ClassName:      BluetoothSearch
 * @Description:     java类作用描述
 * @Author:         tianfy
 * @CreateDate:     2021/3/24 20:47
 * @UpdateUser:     更新者：
 * @UpdateDate:     2021/3/24 20:47
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */
class BluetoothSearch(
    val activity: AppCompatActivity,
    val onBluetoothSearchListener: OnBluetoothSearchListener
) : LifecycleObserver,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private val PERMISSION_REQUEST_CODE = 101
    private var tipsDialog: AlertDialog
    private val handler = Handler(activity.mainLooper)

    private var bluetoothAdapter: BluetoothAdapter? = null

    private var isScan = false

    init {
        tipsDialog = AlertDialog.Builder(activity)
            .setTitle("权限申请").setMessage("使用该功能需要打开该权限，点击确认前往设置页面打开该权限")
            .setPositiveButton("确认", { dialog, which ->
                dialog.dismiss()

            }).setNegativeButton("取消", { dialog, which ->
                dialog.dismiss()
            }).create()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            checkPermissions()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestory() {
        scanLeDevice(false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                checkLocationServiceEnable()
            }
        } else {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                checkLocationServiceEnable()

            }
        }
    }

    /**
     * 检查位置服务是否可用
     */
    private fun checkLocationServiceEnable() {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (networkProvider || gpsProvider) {
            checkBluetoothEnable()
        } else {
            val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivity(
                locationIntent,
            )
        }
    }

    /**
     * 检查蓝牙是否可用
     */
    private fun checkBluetoothEnable() {

        /* 初始化蓝牙管理类 */
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter
        if (bluetoothManager?.adapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivity(enableBtIntent)
        } else {
            scanLeDevice(true)
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        val scanner = bluetoothAdapter?.getBluetoothLeScanner()
        if (enable) {
            onBluetoothSearchListener.onScanStart()
            isScan = true
            val SCAN_PERIOD = 10000 //扫描周期10秒，开始扫描10秒后停止扫描
            handler.postDelayed({
                scanner?.stopScan(scanCallback)
                onBluetoothSearchListener.onScanStop()
            }, SCAN_PERIOD.toLong())
            scanner?.startScan(scanCallback)
        } else {
            onBluetoothSearchListener.onScanStop()
            handler.removeCallbacksAndMessages(null)
            scanner?.stopScan(scanCallback)
            isScan = false
        }

    }

    /**
     * 蓝牙搜索结果回调
     */
    var scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            onBluetoothSearchListener.onScanResult(result.device)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermission(permission: String) {
        activity.requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    checkLocationServiceEnable()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    tipsDialog.show()
                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


   interface OnBluetoothSearchListener {
        //扫描开始，可以开始执行一些动画或者显示提示信息
        fun onScanStart()
        //扫描停止，可以停止动画或者显示提示信息
        fun onScanStop()
        //扫描结果
        fun onScanResult(device: BluetoothDevice)
    }


}