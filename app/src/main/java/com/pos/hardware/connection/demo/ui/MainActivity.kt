package com.pos.hardware.connection.demo.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.shape.MaterialShapeDrawable
import com.pos.connection.bridge.binder.ECRConstant
import com.pos.connection.bridge.binder.ECRParameters
import com.pos.hardware.connection.demo.App
import com.pos.hardware.connection.demo.R
import com.pos.hardware.connection.demo.databinding.ActivityMainBinding
import com.pos.hardware.connection.demo.ecr.ECRHelper
import com.pos.hardware.connection.demo.help.Logger
import com.pos.hardware.connection.demo.help.openActivity
import com.pos.hardware.connection.demo.ui.comm.ChatAdapter
import com.pos.hardware.connection.demo.ui.comm.ChatBean
import com.pos.hardware.connection.library.ECRServiceKernel
import kotlinx.android.synthetic.main.dialog_serial_port_param.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

@SuppressLint("CheckResult")
class MainActivity : BaseAppCompatActivity() {

    private val chatAdapter = ChatAdapter()
    private val chatList = mutableListOf<ChatBean>()
    private val bluetoothList = HashMap<String, String>()
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var mode = ""
    private var bluetoothAddress = ""

    private var vspCableCheckState = ECRConstant.VSPCableCheckState.ENABLE_CHECK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initECR()
        showDeviceTypeDialog()
    }

    private fun initView() {
        mode = ECRConstant.Mode.RS232
        val greenColor = Color.parseColor("#1DD959")
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val purpleColor = ContextCompat.getColor(this, R.color.purple_700)
        val btnDrawable = MaterialShapeDrawable().apply {
            setCornerSize(36f)
            setTint(purpleColor)
        }
        binding.contentEdit.background = MaterialShapeDrawable().apply {
            setCornerSize(6f)
            setTint(whiteColor)
        }
        binding.sendText.background = MaterialShapeDrawable().apply {
            setCornerSize(6f)
            setTint(greenColor)
        }
        binding.connectText.background = btnDrawable
        binding.stopSendText.background = btnDrawable
        binding.disconnectText.background = btnDrawable
        binding.contentEdit.addTextChangedListener { checkSendButton() }
        binding.disconnectText.setOnClickListener {
            Logger.e("wl", "调用断开连接")
            ECRHelper.disconnect()
        }
        binding.stopSendText.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("method", "iBeaconStopTransmit")
            ECRHelper.extensionMethod(bundle)
            ECRServiceKernel.getInstance().ecrService.stop()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = chatAdapter
    }

    private fun initECR() {
        ECRHelper.onBindSuccess = {
            ECRHelper.registerECRListener()
        }
        ECRHelper.onBindFailure = {
            runOnUiThread { changeConnectStatus() }
            showToast(R.string.message_bind_ecr_service)
        }
        ECRHelper.onECRConnected = {
            runOnUiThread { changeConnectStatus() }
        }
        ECRHelper.onECRDisconnected = { code, message ->
            when (code) {
                -7006 -> {

                }
            }
            runOnUiThread { changeConnectStatus() }
            showToast("$message ($code)")
        }
        ECRHelper.onSendSuccess = {
            // TODO send data success
            showToast("data send success")
        }
        ECRHelper.onSendFailure = { code, message ->
            showToast("$message ($code)")
            runOnUiThread { changeConnectStatus() }
        }
        ECRHelper.onECRReceive = { bytes ->
            val text = String(bytes, StandardCharsets.UTF_8)
            val chat = ChatBean(
                content = text,
                sender = if (App.server) "Client" else "Server",
                receiver = if (App.server) "Server" else "Client"
            )
            chatList.add(chat)
            refreshAdapter()
        }

        ECRHelper.onWaitConnect = {
            runOnUiThread { changeConnectStatus() }
        }
        ECRHelper.onBindError = { code, message ->
            runOnUiThread {
                runOnUiThread { changeConnectStatus() }
                showToast("$message ($code)")
            }
        }

        ECRHelper.bindECRService()
    }

    private fun send() {
        Executors.newCachedThreadPool().execute {
            val text = binding.contentEdit.text.toString()
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            val chat = ChatBean(
                content = text,
                sender = if (App.server) "Server" else "Client",
                receiver = if (App.server) "Client" else "Server"
            )
            val size = bytes.size
            Logger.e(App.TAG, "size: $size")
            ECRHelper.send(bytes)
            chatList.add(chat)
            refreshAdapter()
        }
    }

    private fun connect() {
        val bundle = Bundle()
        bundle.putString(ECRParameters.MODE, mode)
        bundle.putString(
            ECRParameters.TYPE, if (App.server) ECRConstant.Type.MASTER else ECRConstant.Type.SLAVE
        )
        when (mode) {
            ECRConstant.Mode.Bluetooth -> {
                Logger.e(App.TAG, "bluetoothAddress: $bluetoothAddress")
                bundle.putString(ECRParameters.BLUETOOTH_MAC_ADDRESS, bluetoothAddress)
            }

            ECRConstant.Mode.WIFI -> {
                showDialog(bundle)
                return
            }

            ECRConstant.Mode.VSP -> {
                bundle.putInt(ECRParameters.ENABLE_VSP_CABLE_CHECK, vspCableCheckState)
            }

            ECRConstant.Mode.RS232 -> {
                showSerialPortDialog(bundle)
                return
            }
        }
        binding.connectText.text = getString(R.string.connecting)
        binding.connectText.setOnClickListener(null)
        binding.connectText.alpha = 0.5f
        ECRHelper.connect(bundle)
    }

    override fun onResume() {
        super.onResume()
        refreshAdapter()
        changeConnectStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        ECRHelper.disconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (App.connected != -1) {
            showToast(R.string.message_select_connect)
            return super.onOptionsItemSelected(item)
        }
        when (item.itemId) {
            R.id.item_vsp -> {
                mode = ECRConstant.Mode.VSP
                showVSPCableCheckStateDialog()
            }

            R.id.item_usb -> {
                mode = ECRConstant.Mode.USB
            }

            R.id.item_beacon -> {
                mode = ECRConstant.Mode.iBeacon
            }

            R.id.item_rs232 -> {
                mode = ECRConstant.Mode.RS232
            }

            R.id.item_bluetooth -> {
                mode = ECRConstant.Mode.Bluetooth
                if (!App.server) {
                    switchBluetooth()
                }
            }

            R.id.item_wifi -> {
                mode = ECRConstant.Mode.WIFI
            }

            R.id.item_extended_test -> {
                openActivity<ExtendedTestActivity>()
            }
        }
        showConnectStatus()
        return super.onOptionsItemSelected(item)
    }

    private fun refreshAdapter() {
        runOnUiThread {
            chatAdapter.setData(chatList)
            if (chatAdapter.itemCount >= 1) {
                binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    @Synchronized
    private fun changeConnectStatus() {
        when (App.connected) {
            -1 -> {
                binding.connectText.alpha = 1f
                binding.connectText.setOnClickListener { connect() }
                binding.connectText.text = getString(R.string.connect)
            }

            0 -> {
                binding.connectText.alpha = 1f
                binding.connectText.setOnClickListener { connect() }
                binding.connectText.text = getString(R.string.connect)
            }

            1 -> {
                binding.connectText.alpha = 1f
                binding.connectText.setOnClickListener { connect() }
                binding.connectText.text = getString(R.string.wait_connect)
            }

        }
        checkSendButton()
        showConnectStatus()
    }

    private fun checkSendButton() {
        val bool = binding.contentEdit.text.toString().isNotBlank()
        if (bool && App.connected == 0) {
            binding.sendText.alpha = 1f
            binding.sendText.setOnClickListener { send() }
        } else {
            binding.sendText.alpha = 0.5f
            binding.sendText.setOnClickListener(null)
        }
    }

    private fun showConnectStatus() {
        var text = ""
        when (App.connected) {
            -1 -> text = getString(R.string.disconnected)
            0 -> text = getString(R.string.connected)
            1 -> text = getString(R.string.wait_connect)
        }
        text = when (mode) {
            ECRConstant.Mode.VSP -> getString(R.string.type_vsp) + " $text"
            ECRConstant.Mode.USB -> getString(R.string.type_usb) + " $text"
            ECRConstant.Mode.RS232 -> getString(R.string.type_rs232) + " $text"
            ECRConstant.Mode.iBeacon -> getString(R.string.type_beacon) + " $text"
            ECRConstant.Mode.Bluetooth -> getString(R.string.type_bluetooth) + " $text"
            ECRConstant.Mode.WIFI -> getString(R.string.type_wifi) + " $text"

            else -> ""
        }
        text = if (App.server) "Server $text" else "Client $text"
        binding.statusText.text = text
        if (mode == ECRConstant.Mode.iBeacon) {
            binding.stopSendText.visibility = View.VISIBLE
        } else {
            binding.stopSendText.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun switchBluetooth() {
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

    private fun showDeviceTypeDialog() {
        val view = View.inflate(baseContext, R.layout.dialog_devices_type, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.rg_type)
        val dialog = AlertDialog.Builder(this).setTitle(R.string.dialog_device_type).setView(view)
            .setCancelable(false).show()
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            App.server = checkedId == R.id.mr_server
            showConnectStatus()
            dialog.dismiss()
        }
    }

    private fun showDialog(bundle: Bundle) {
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
            bundle.putInt(
                ECRParameters.WIFI_PORT, port.text.toString().toInt()
            )
            bundle.putString(ECRParameters.WIFI_ADDRESS, ip.text.toString())
            binding.connectText.text = getString(R.string.connecting)
            binding.connectText.setOnClickListener(null)
            binding.connectText.alpha = 0.5f
            ECRHelper.connect(bundle)
        }.show()

    }

    private fun showSerialPortDialog(bundle: Bundle) {
        val view = View.inflate(baseContext, R.layout.dialog_serial_port_param, null)
        AlertDialog.Builder(this).setTitle("Input Data").setView(view).setPositiveButton(
            "Confirm"
        ) { _, _ ->
            val baudRate = view.findViewById<EditText>(R.id.et_baud_rate)
            val spDataBits = view.findViewById<Spinner>(R.id.sp_data_bits)
            val spParity = view.findViewById<Spinner>(R.id.sp_parity)
            val spStopBits = view.findViewById<Spinner>(R.id.sp_stop_bits)
            if (TextUtils.isEmpty(baudRate.text.toString())) {
                showToast("Please input baud rate")
                return@setPositiveButton
            }
            try {
                bundle.putInt(ECRParameters.BAUD_RATE, baudRate.text.toString().toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Please input correct baud rate")
                return@setPositiveButton
            }
            val dataBits = spDataBits.selectedItemPosition + 5
            val parity = spParity.selectedItemPosition
            val stopBits = spStopBits.selectedItemPosition + 1
            bundle.putInt(ECRParameters.DATA_BITS, dataBits)
            bundle.putInt(ECRParameters.PARITY, parity)
            bundle.putInt(ECRParameters.STOP_BITS, stopBits)

            binding.connectText.text = getString(R.string.connecting)
            binding.connectText.setOnClickListener(null)
            binding.connectText.alpha = 0.5f
            ECRHelper.connect(bundle)
        }.show()
        view.findViewById<Spinner>(R.id.sp_data_bits).setSelection(3)

    }


    private fun showBluetoothDialog() {
        val arrays = bluetoothList.keys.toList()
        MaterialDialog(this).show {
            setCancelable(false)
            message(R.string.dialog_select_bluetooth)
            setCanceledOnTouchOutside(false)
            listItems(items = arrays) { _, index, text ->
                Logger.e(App.TAG, "index: $index text: $text")
                bluetoothAddress = bluetoothList[text] ?: ""
            }
            positiveButton(R.string.ok)
            lifecycleOwner(this@MainActivity)
        }
    }

    private fun showVSPCableCheckStateDialog() {
        val arrays = arrayListOf("ENABLE_CHECK", "DISABLE_CHECK")
        MaterialDialog(this).show {
            setCancelable(false)
            message(R.string.dialog_select_vsp_state)
            setCanceledOnTouchOutside(false)
            listItems(items = arrays) { _, index, text ->
                Logger.e(App.TAG, "index: $index text: $text")
                if (index == 0) {
                    vspCableCheckState = ECRConstant.VSPCableCheckState.ENABLE_CHECK
                } else {
                    vspCableCheckState = ECRConstant.VSPCableCheckState.DISABLE_CHECK
                }
            }
            positiveButton(R.string.ok)
            lifecycleOwner(this@MainActivity)
        }
    }
}