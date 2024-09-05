package com.pos.hardware.connection.demo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.pos.hardware.connection.demo.help.DeviceHelper

@SuppressLint("StaticFieldLeak")
class App : Application() {

    companion object {
        const val TAG = "ECRDemo"

        lateinit var app: App
        lateinit var context: Context

        var server = false
        var connected: Int = -1
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        context = applicationContext
        server = DeviceHelper.isDesktop
    }

}