package com.tianfy.blelib

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
import org.jetbrains.annotations.NotNull

/**
 *
 * @ProjectName:    BluetoothDemo
 * @Package:        com.example.bluetoothdemo
 * @ClassName:      BluetoothSearch
 * @Description:     蓝牙搜索封装类，实现LifecycleObserver接口，封装了权限申请，蓝牙搜索等相关代码逻辑
 *                  通过builder建造者模式，实现搜索开始，搜索停止，搜索结果的回调
 * @Author:         tianfy
 * @CreateDate:     2021/3/24 20:47
 * @UpdateUser:     更新者：
 * @UpdateDate:     2021/3/24 20:47
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */
class BluetoothSearch(
    builder: Builder
) : LifecycleObserver,
    ActivityCompat.OnRequestPermissionsResultCallback {

    /* 请求位置权限的requestCode */
    private val PERMISSION_REQUEST_CODE = 101

    /* 提示用户为什么需要位置权限的dialog */
    private var tipsDialog: AlertDialog

    /* 当前activity */
    private var activity: AppCompatActivity

    /* 蓝牙扫描时间定义 默认1000毫秒=10s*/
    private var blueScanTime: Long

    /* 主线程Handler */
    private val handler = Handler(builder.getActivity().mainLooper)

    /* bluetoothAdapter */
    private var bluetoothAdapter: BluetoothAdapter? = null

    /* 是否正在扫描的标志位 */
    private var isScan = false

    /* 蓝牙扫描监听 */
    private var onBluetoothSearchListener: OnBluetoothSearchListener

    init {
        activity = builder.getActivity()
        blueScanTime = builder.getScanTime()
        onBluetoothSearchListener = builder.getOnBluetoothSearchListener()
        tipsDialog = AlertDialog.Builder(activity)
            .setTitle("权限申请").setMessage("使用该功能需要打开该权限，点击确认前往设置页面打开该权限")
            .setPositiveButton("确认") { _, _ ->
                goToLocationSettingActivity()
            }.setNegativeButton("取消") { _, _ ->
                activity.finish()
            }.create()
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

    /**
     * 对外暴露重新扫描的方法
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun reScan() {
        //先停止扫描
        scanLeDevice(false)
        //重新扫描
        checkPermissions()
    }

    /**
     * 检查是否有位置权限
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        //如果在android9以上，需要申请Manifest.permission.ACCESS_FINE_LOCATION权限
        //android9以下，只需要申请Manifest.permission.ACCESS_COARSE_LOCATION权限
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
            tipsDialog.show()
        }
    }

    /**
     * 前往位置服务设置页面
     */
    private fun goToLocationSettingActivity() {
        val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activity.startActivity(
            locationIntent,
        )
    }

    /**
     * 检查蓝牙是否可用
     */
    private fun checkBluetoothEnable() {

        /* 初始化蓝牙管理类 */
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivity(enableBtIntent)
        } else {
            scanLeDevice(true)
        }
    }

    /**
     * 扫描设备
     * @param enable 扫描停止和开始的标志位
     */
    private fun scanLeDevice(enable: Boolean) {
        val scanner = bluetoothAdapter?.getBluetoothLeScanner()
        if (enable) {
            onBluetoothSearchListener.onScanStart()
            isScan = true
            handler.postDelayed({
                scanner?.stopScan(scanCallback)
                onBluetoothSearchListener.onScanStop()
            }, blueScanTime)
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

    /**
     * 请求位置权限
     * @param permission 权限名称
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermission(permission: String) {
        activity.requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
    }

    /**
     * 权限申请结果回调
     */
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

    /**
     * 蓝牙扫描监听
     */
    interface OnBluetoothSearchListener {
        /**
         * 扫描开始，可以开始执行一些动画或者显示提示信息
         */
        fun onScanStart()

        /**
         *扫描停止，可以停止动画或者显示提示信息
         */
        fun onScanStop()

        /**
         * 扫描结果
         */
        fun onScanResult(device: BluetoothDevice)
    }

    class Builder(private val activity: AppCompatActivity) {

        private lateinit var onBluetoothSearchListener: OnBluetoothSearchListener
        private var blueScanTime: Long = 1000

        fun getActivity(): AppCompatActivity {
            return activity
        }

        /**
         * 设置蓝牙扫描时间
         * @param time 毫秒值
         */
        fun setScanTime(time: Long): Builder {
            this.blueScanTime = time
            return this
        }

        fun getScanTime(): Long {
            return blueScanTime
        }

        /**
         * 设置蓝牙扫描监听
         * @param onBluetoothSearchListener 蓝牙扫描监听
         */
        fun setOnBluetoothSearchListener(@NotNull onBluetoothSearchListener: OnBluetoothSearchListener): Builder {
            this.onBluetoothSearchListener = onBluetoothSearchListener
            return this
        }

        fun getOnBluetoothSearchListener(): OnBluetoothSearchListener {
            return onBluetoothSearchListener
        }

        fun builder(): BluetoothSearch {
            return BluetoothSearch(this)
        }


    }


}