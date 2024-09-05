package com.pos.hardware.connection.demo.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.pos.connection.bridge.binder.ECRConstant
import com.pos.connection.bridge.binder.ECRParameters
import com.pos.hardware.connection.demo.App
import com.pos.hardware.connection.demo.R
import com.pos.hardware.connection.demo.ecr.ECRHelper
import com.pos.hardware.connection.demo.help.Logger
import kotlinx.android.synthetic.main.activity_vspstatus.*
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ExtendedTestActivity : BaseAppCompatActivity() {


    /**status desc
     *
     * -2:Initial state, not bound to ECR service
     * -1:Binding ECR service successful, physical channel not ready
     * 0:Binding ECR service successful, Physical channels are ready
     * 1: VSP is connected
     */
    private var status = -2


    private val lock = CyclicBarrier(2)
    private var isHandshakeCheck = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vspstatus)
        init()
    }

    fun onClick(view: View) {
        when (view) {
            btn_init -> {
                connect(
                    sp_mode.adapter.getItem(sp_mode.selectedItemPosition) as String,
                    sp_type.adapter.getItem(sp_type.selectedItemPosition) as String
                )
            }

            btn_getVSPStatus -> {
                getStatus()
            }

            btn_close -> {
                close()
            }

            btn_sendLargeData -> {
                testSendLargeData()
            }
        }

    }


    private fun testSendLargeData() {
        val number = et_number.text.toString().toInt()
        val testData = ByteArray(number)
        Arrays.fill(testData, 0x11)
        ECRHelper.send(testData)
    }

    private fun init() {
        ECRHelper.onBindSuccess = {
            ECRHelper.registerECRListener()
            status = -1
            runOnUiThread {
                tv_msg.text = "Binding ECR service successful, physical channel not ready"
            }

        }
        ECRHelper.onBindFailure = {
            showToast(R.string.message_bind_ecr_service)
            status = -2
            runOnUiThread { tv_msg.text = "Initial state, not bound to ECR service" }

        }
        ECRHelper.onECRConnected = {
            status = 0
            runOnUiThread { tv_msg.text = "Physical channels are ready" }

        }
        ECRHelper.onECRDisconnected = { code, message ->
            status = -1
            showToast("$message ($code)")
            runOnUiThread {
                tv_msg.text = "Binding ECR service successful, physical channel not ready"
            }
        }
        ECRHelper.onSendSuccess = {
            // TODO send data success
            runOnUiThread {
                tv_msg.text = "Send Data  Success"
            }
        }
        ECRHelper.onSendFailure = { code, message ->
            showToast("$message ($code)")
        }
        ECRHelper.onECRReceive = { bytes ->
            val text = String(bytes, StandardCharsets.UTF_8)
            if (text == "BOOM") {
                if (isHandshakeCheck) {
                    // A handshake request answer is received and the VSP channel is considered to have established a connection
                    runOnUiThread { tv_msg.text = "VSP Connected" }
                    status = 1
                    lock.await()
                } else {
                    //Receive handshake request, send "BOOM" in response to handshake request
                    ECRHelper.send("BOOM".toByteArray(StandardCharsets.UTF_8))
                }
            } else {
                runOnUiThread { tv_msg.text = "receiver data:${text}" }
            }
        }
        ECRHelper.bindECRService()
    }

    fun connect(mode: String, type: String = ECRConstant.Type.MASTER) {
        if (mode == ECRConstant.Mode.Bluetooth && type == ECRConstant.Type.SLAVE) {
            switchBluetooth(mode, type)
        } else if (mode == ECRConstant.Mode.WIFI) {
            showWifiParamDialog(mode, type)
        } else {
            val bundle = Bundle()
            bundle.putString(ECRParameters.MODE, mode)
            bundle.putString(ECRParameters.TYPE, type)
            ECRHelper.connect(bundle)
        }
    }

    private fun showWifiParamDialog(mode: String, type: String = ECRConstant.Type.MASTER) {
        val view = View.inflate(baseContext, R.layout.dialog_content, null)
        val ip = view.findViewById<EditText>(R.id.ip)
        val port = view.findViewById<EditText>(R.id.port)
        if (App.server) {
            ip.visibility = View.GONE
        }
        AlertDialog.Builder(this).setTitle("Input Data").setView(view).setPositiveButton(
            "Confirm"
        ) { _, _ ->
            if (App.server) {
                if (TextUtils.isEmpty(port.text.toString())) {
                    showToast("Please input port")
                    return@setPositiveButton
                }
            } else {
                if (TextUtils.isEmpty(port.text.toString()) || TextUtils.isEmpty(ip.text.toString())) {
                    showToast("Please input port and ip")
                    return@setPositiveButton
                }
            }
            val bundle = Bundle()
            bundle.putString(ECRParameters.MODE, mode)
            bundle.putString(ECRParameters.TYPE, type)
            bundle.putInt(ECRParameters.WIFI_PORT, port.text.toString().toInt())
            bundle.putString(ECRParameters.WIFI_ADDRESS, ip.text.toString())
            ECRHelper.connect(bundle)
        }.show()
    }

    private val bluetoothList = HashMap<String, String>()

    @SuppressLint("MissingPermission")
    private fun switchBluetooth(mode: String, type: String = ECRConstant.Type.MASTER) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled) {
                bluetoothList.clear()
                val bondedDevices = bluetoothAdapter.bondedDevices
                if (bondedDevices != null && bondedDevices.size > 0) {
                    for (bluetoothDevice in bondedDevices) {
                        val name = bluetoothDevice.name
                        val address = bluetoothDevice.address
                        Logger.e(App.TAG, "name: $name")
                        Logger.e(App.TAG, "address: $address")
                        if (address == "00:11:22:33:44:55") continue
                        bluetoothList["$name - $address"] = address
                        val bundle = Bundle()
                        bundle.putString(ECRParameters.MODE, mode)
                        bundle.putString(ECRParameters.TYPE, type)
                        bundle.putString(ECRParameters.BLUETOOTH_MAC_ADDRESS, address)
                        ECRHelper.connect(bundle)
                    }
                }
                if (bluetoothList.size > 0) {
                    showBluetoothDialog()
                } else {
                    showToast(R.string.message_bluetooth_not_find_bonded)
                }
            } else {
                showToast(R.string.message_bluetooth_disable)
            }
        } else {
            showToast(R.string.message_bluetooth_not_support)
        }
    }

    private fun showBluetoothDialog() {
        val arrays = bluetoothList.keys.toList()
        MaterialDialog(this).show {
            setCancelable(false)
            message(R.string.dialog_select_bluetooth)
            setCanceledOnTouchOutside(false)
            listItems(items = arrays) { _, index, text ->
                Logger.e(App.TAG, "index: $index text: $text")
                val bluetoothAddress = bluetoothList[text] ?: ""

            }
            positiveButton(R.string.ok)
            lifecycleOwner(this@ExtendedTestActivity)
        }
    }

    private fun getStatus() {
        when (status) {
            0 -> {
                ECRHelper.send("BOOM".toByteArray(StandardCharsets.UTF_8))
                isHandshakeCheck = true
                try {
                    lock.await(1000, TimeUnit.MILLISECONDS)
                } catch (e: TimeoutException) {
                    e.printStackTrace()
                    lock.reset()
                }
            }

            1 -> {
                tv_msg.text = "VSP Connected"
            }

            else -> {
                tv_msg.text = "Physical channel not ready or Failed to bind ECR service，Pls check "
            }
        }
    }

    private fun close() {
        ECRHelper.disconnect()
        status = -1;
    }

}