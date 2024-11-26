package com.pos.hardware.connection.demo.ecr;

import android.os.Bundle;
import android.os.RemoteException;

import com.pos.connection.bridge.ECRConnection;
import com.pos.connection.bridge.ECRListener;
import com.pos.connection.bridge.ECRRequestCallback;
import com.pos.hardware.connection.demo.App;
import com.pos.hardware.connection.demo.help.Helper;
import com.pos.hardware.connection.demo.help.Logger;
import com.pos.hardware.connection.library.ECRService;
import com.pos.hardware.connection.library.ECRServiceKernel;

import java.nio.charset.StandardCharsets;

public class ECRHelper {

    private static ECRService ecrService;

    public static Runnable onBindSuccess = () -> {
    };
    public static Runnable onBindFailure = () -> {
    };
    public static BiConsumer<Integer, String> onBindError = (code, message) -> {
    };

    public static Runnable onECRConnected = () -> {
    };
    public static BiConsumer<Integer, String> onECRDisconnected = (code, message) -> {
    };

    public static Runnable onSendSuccess = () -> {
    };
    public static BiConsumer<Integer, String> onSendFailure = (code, message) -> {
    };

    public static Consumer<byte[]> onECRReceive = bytes -> {
    };
    public static Runnable onWaitConnect = () -> {
    };

    public static void connect(Bundle bundle) {
        call(() -> Helper.anyExecute(ecrService, service -> {
            try {
                service.connect(bundle, ecrConnection);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static void disconnect() {
        call(() -> {
            Helper.anyExecute(ecrService, ECRService::disconnect);
        });
    }

    public static void registerECRListener() {
        call(() -> Helper.anyExecute(ecrService, service -> service.register(ecrListener)));
    }

    public static void unregisterECRListener() {
        call(() -> Helper.anyExecute(ecrService, service -> service.unregister(ecrListener)));
    }

    public static void extensionMethod(Bundle bundle) {
        call(() -> Helper.anyExecute(ecrService, service -> service.extensionMethod(bundle)));
    }

    public static void stop() {
        call(() -> Helper.anyExecute(ecrService, ECRService::stop));
    }

    private static final ECRListener ecrListener = new ECRListener.Stub() {
        @Override
        public void onReceive(byte[] byteArray) {
            if (byteArray != null) {
                String string = new String(byteArray, StandardCharsets.UTF_8);
                Logger.e(App.TAG, "onReceive string: " + string);
                onECRReceive.accept(byteArray);
            }
        }
    };

    public static void send(byte[] bytes) {
        Logger.e("wl", "调用发送数据");
        try {
            ECRService bridgeService = ecrService;
            if (bridgeService != null) {
                bridgeService.send(bytes, requestCallback);
            } else {
                onECRDisconnected.accept(-100, "The bind ECRService failure");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            onSendFailure.accept(-200, ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "");
        }
    }

    private static final ECRRequestCallback requestCallback = new ECRRequestCallback.Stub() {
        @Override
        public void onSuccess() {
            Logger.e(App.TAG, "onSuccess");
            onSendSuccess.run();
        }

        @Override
        public void onFailure(int code, String message) {
            Logger.e(App.TAG, "onFailure code: " + code + " message: " + message);
            onSendFailure.accept(code, message != null ? message : "failure");
        }
    };

    private static final ECRConnection ecrConnection = new ECRConnection.Stub() {
        @Override
        public void onConnected() {
            Logger.e(App.TAG, "onConnected");
            App.connected = 0;
            onECRConnected.run();
        }

        @Override
        public void onDisconnected(int code, String message) {
            Logger.e(App.TAG, "onDisconnected code: " + code + " message: " + message);
            App.connected = -1;
            onECRDisconnected.accept(code, message != null ? message : "failure");
        }

        @Override
        public void onWaitingConnect() {
            App.connected = 1;
            onWaitConnect.run();
        }
    };

    public static void bindECRService() {
        ECRServiceKernel.getInstance().bindService(App.getContext(), connectionCallback);
    }

    private static final ECRServiceKernel.ConnectionCallback connectionCallback = new ECRServiceKernel.ConnectionCallback() {
        @Override
        public void onServiceConnected() {
            Logger.e(App.TAG, "onServiceConnected");
            ecrService = ECRServiceKernel.getInstance().ecrService;
            onBindSuccess.run();
        }

        @Override
        public void onServiceDisconnected() {
            Logger.e(App.TAG, "onServiceDisconnected");
            App.connected = -1;
            ecrService = null;
            onBindFailure.run();
        }

        @Override
        public void onError(int code, String message) {
            Logger.e(App.TAG, "onError");
            App.connected = -1;
            ecrService = null;
            if (message != null) {
                onBindError.accept(code, message);
            }
        }
    };

    private static void call(Runnable block) {
        try {
            ECRService bridgeService = ecrService;
            if (bridgeService != null) {
                block.run();
            } else {
                onECRDisconnected.accept(-100, "The bind ECRService failure");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            onECRDisconnected.accept(-200, ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "");
        }
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }

    @FunctionalInterface
    public interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}

