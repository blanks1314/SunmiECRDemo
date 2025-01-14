package com.pos.hardware.connection.demo.ui.comm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

/**
 * @Desc
 * @Author blanks
 * @Date 2022/12/13 3:05 PM
 */
public class BatteryUtils {
    private static final String TAG = "BatteryUtils";

    /**
     * 主动获取当前电池是否在充电 , 即数据线是否插在手机上
     *
     * @return
     */
    public static boolean isBatteryCharging(Context context) {
        boolean isBatteryCharging = false;
        // 主动发送包含是否正在充电状态的广播 , 该广播会持续发送
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        // 注册广播接受者
        Intent intent = context.registerReceiver(null, intentFilter);

        // 获取充电状态
        int batteryChargeState = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        Log.e(TAG, "当前充电状态:" + batteryChargeState);

//        // 判定是否是 AC 交流电充电
//        boolean isAc = batteryChargeState == BatteryManager.BATTERY_PLUGGED_AC;
        // 判断是否是 USB 充电
        boolean isUsb = batteryChargeState == BatteryManager.BATTERY_PLUGGED_USB;

        // 如何上述任意一种为 true , 说明当前正在充电
        isBatteryCharging = isUsb;

        return isBatteryCharging;
    }


}
