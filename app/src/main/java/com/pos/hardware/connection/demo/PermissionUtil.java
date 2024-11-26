package com.pos.hardware.connection.demo;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
/**
 * @Desc
 * @Author blanks
 * @Date 2024/11/13 14:45
 */
public class PermissionUtil {
    private static final String TAG = "PermissionUtil";

    /**
     * 判断是否权限都已授权，存在未授权则进行申请授权
     * @param activity
     * @param permissions 权限列表
     * @param requestCode
     * @return
     */
    public static boolean checkPermission(Activity activity, String[] permissions, int requestCode) {

        // 默认已经授权
        int check = PackageManager.PERMISSION_GRANTED;

        // 判断安卓版本是否是大于6.0（6.0及以上才需要申请权限）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // 循环处理
            for (String permission : permissions) {
                // 验证单个权限是否授权-【核心】
                check = ContextCompat.checkSelfPermission(activity, permission);
                // 判断发现存在某一个未授权
                if (check != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
            }
        }

        // 存在某一个未授权，重新进行申请权限
        if (check != PackageManager.PERMISSION_GRANTED) {
            // 执行权限列表的申请-【核心】
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
            return false;
        }

        return true;
    }


    /**
     * 判断权限集合是否授权结果里是否存在未授权的情况，如果有返回false，否则反之
     * @param grantResults
     * @return
     */
    public static boolean checkGrant(int[] grantResults) {

        // 判断是否存在结果集合，没有直接返回false
        if (grantResults != null) {
            // 循环判断
            for (int grant : grantResults) {
                // 判断是否有未授权
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}