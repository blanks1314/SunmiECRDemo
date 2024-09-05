package com.pos.hardware.connection.demo.ecr

import android.os.Bundle
import com.pos.connection.bridge.ECRConnection
import com.pos.connection.bridge.ECRListener
import com.pos.connection.bridge.ECRRequestCallback
import com.pos.hardware.connection.demo.App
import com.pos.hardware.connection.demo.help.Logger
import com.pos.hardware.connection.demo.help.anyExecute
import com.pos.hardware.connection.library.ECRService
import com.pos.hardware.connection.library.ECRServiceKernel
import java.nio.charset.StandardCharsets

object ECRHelper {

    private var ecrService: ECRService? = null

    var onBindSuccess: () -> Unit = { }
    var onBindFailure: () -> Unit = { }
    var onBindError: (Int, String) -> Unit = { _, _ -> }


    var onECRConnected: () -> Unit = { }
    var onECRDisconnected: (Int, String) -> Unit = { _, _ -> }

    var onSendSuccess: () -> Unit = { }
    var onSendFailure: (Int, String) -> Unit = { _, _ -> }

    var onECRReceive: (bytes: ByteArray) -> Unit = { }

    var onWaitConnect: () -> Unit = {}

    fun connect(bundle: Bundle) {
        call { anyExecute(ecrService) { connect(bundle, ecrConnection) } }
    }

    fun disconnect() {
        call { anyExecute(ecrService) { disconnect() } }
    }

    fun registerECRListener() {
        call { anyExecute(ecrService) { register(ecrListener) } }
    }

    fun unregisterECRListener() {
        call { anyExecute(ecrService) { unregister(ecrListener) } }
    }

    fun extensionMethod(bundle: Bundle) {
        call { anyExecute(ecrService) { extensionMethod(bundle) } }
    }

    fun stop() {
        call { anyExecute(ecrService) { stop() } }
    }

    private val ecrListener = object : ECRListener.Stub() {

        override fun onReceive(byteArray: ByteArray?) {
            if (byteArray != null) {
                val string = String(byteArray, StandardCharsets.UTF_8)
                Logger.e(App.TAG, "onReceive string: $string")
                onECRReceive(byteArray)
            }
        }

    }

    fun send(bytes: ByteArray) {
        Logger.e("wl", "调用发送数据")
        try {
            val bridgeService = ecrService
            if (bridgeService != null) {
                bridgeService.send(bytes, requestCallback)
            } else {
                onECRDisconnected(-100, "The bind ECRService failure")
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            onSendFailure(-200, ex.localizedMessage ?: "")
        }
    }

    private val requestCallback = object : ECRRequestCallback.Stub() {

        override fun onSuccess() {
            Logger.e(App.TAG, "onSuccess")
            onSendSuccess()
        }

        override fun onFailure(code: Int, massage: String ? ) {
            Logger.e(App.TAG, "onFailure code: $code massage: $massage")
            onSendFailure(code, massage ?: "failure")
        }

    }

    private val ecrConnection = object : ECRConnection.Stub() {

        override fun onConnected() {
            Logger.e(App.TAG, "onConnected")
            App.connected = 0
            onECRConnected()
        }

        override fun onDisconnected(code: Int, massage: String?) {
            Logger.e(App.TAG, "onDisconnected code: $code massage: $massage")
            App.connected = -1
            onECRDisconnected(code, massage ?: "failure")
        }

        override fun onWaitingConnect() {
            App.connected = 1
            onWaitConnect()
        }

    }

    fun bindECRService() {
        ECRServiceKernel.getInstance().bindService(App.context, connectionCallback)
    }

    private val connectionCallback = object : ECRServiceKernel.ConnectionCallback {

        override fun onServiceConnected() {
            Logger.e(App.TAG, "onServiceConnected")
            ecrService = ECRServiceKernel.getInstance().ecrService
            onBindSuccess()
        }

        override fun onServiceDisconnected() {
            Logger.e(App.TAG, "onServiceDisconnected")
            App.connected = -1
            ecrService = null
            onBindFailure()
        }

        override fun onError(code: Int, message: String?) {
            Logger.e(App.TAG, "onError")
            App.connected = -1
            ecrService = null
            message?.let { onBindError(code, it) }
        }

    }

    private fun call(block: () -> Unit) {
        try {
            val bridgeService = ecrService
            if (bridgeService != null) {
                block()
            } else {
                onECRDisconnected(-100, "The bind ECRService failure")
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            onECRDisconnected(-200, ex.localizedMessage ?: "")
        }
    }

}