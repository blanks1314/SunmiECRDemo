package com.pos.hardware.connection.demo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.pos.hardware.connection.demo.help.DeviceHelper;


/**
 * @author: Dadong
 * @date: 2024/11/21
 */

public class App extends Application {

    public static final String TAG = "ECRDemo";

    @SuppressLint("StaticFieldLeak")
    private static App appInstance;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static boolean server = false;
    public static int connected = -1;

    public static App getInstance() {
        return appInstance;
    }

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appInstance = this;
        context = getApplicationContext();
        server = DeviceHelper.isDesktop();
    }
}
