package com.pos.hardware.connection.demo.ui

import android.os.Bundle
import android.view.View
import com.pos.connection.bridge.binder.ECRConstant
import com.pos.connection.bridge.binder.ECRParameters
import com.pos.hardware.connection.demo.R
import com.pos.hardware.connection.demo.ecr.ECRHelper
import kotlinx.android.synthetic.main.activity_vspstatus.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class VSPStatusActivity : BaseAppCompatActivity() {


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
                connect()
            }

            btn_getVSPStatus -> {
                getStatus()
            }

            btn_close -> {
                close()
            }
        }

    }


    private fun init() {
        ECRHelper.onBindSuccess = {
            ECRHelper.registerECRListener()
            status = -1
            runOnUiThread { tv_msg.text = "Binding ECR service successful, physical channel not ready" }

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
            runOnUiThread { tv_msg.text = "Binding ECR service successful, physical channel not ready" }
        }
        ECRHelper.onSendSuccess = {
            // TODO send data success
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

    fun connect() {
        val bundle = Bundle()
        bundle.putString(ECRParameters.MODE, ECRConstant.Mode.VSP)
        ECRHelper.connect(bundle)
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