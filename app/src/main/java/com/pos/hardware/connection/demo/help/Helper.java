package com.pos.hardware.connection.demo.help;


import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author: Dadong
 * @date: 2024/11/22
 */

public class Helper {
    // 打开一个 Activity
    public static <T extends Activity> void openActivity(Activity activity, Class<T> targetActivityClass) {
        Intent intent = new Intent(activity, targetActivityClass);
        activity.startActivity(intent);
    }

    // 安全执行某个 block
    public static <T> void anyExecute(T receiver, ReceiverBlock<T> block) {
        if (receiver == null) {
            Log.e("ktx", "The depend on call is null");
        } else {
            try {
                block.execute(receiver);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 字符串扩展功能：是否为空
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // 字符串扩展功能：是否有效
    public static boolean isValid(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // 定义一个函数式接口来模拟 Kotlin 的扩展函数
    @FunctionalInterface
    public interface ReceiverBlock<T> {
        void execute(T receiver) throws RemoteException;
    }
}
