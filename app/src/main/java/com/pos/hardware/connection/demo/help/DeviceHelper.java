package com.pos.hardware.connection.demo.help;
import android.os.Build;
/**
 * @author: Dadong
 * @date: 2024/11/21
 */

public class DeviceHelper {

    private DeviceHelper() {
        // 私有构造函数防止实例化
    }

    public static boolean isDesktop() {
        String model = getDeviceModel().toUpperCase();
        return model.contains("T1") || model.contains("T2") || model.contains("D2") ||
                model.contains("K1") || model.contains("K2");
    }

    public static String getDeviceModel() {
        return Build.MODEL;
    }
}
