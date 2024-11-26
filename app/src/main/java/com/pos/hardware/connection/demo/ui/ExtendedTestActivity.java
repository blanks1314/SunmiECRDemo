package com.pos.hardware.connection.demo.ui;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;

import com.pos.connection.bridge.binder.ECRConstant;
import com.pos.connection.bridge.binder.ECRParameters;
import com.pos.hardware.connection.demo.App;
import com.pos.hardware.connection.demo.R;
import com.pos.hardware.connection.demo.ecr.ECRHelper;
import com.pos.hardware.connection.demo.help.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;


/**
 * @author: Dadong
 * @date: 2024/11/21
 */


public class ExtendedTestActivity extends BaseAppCompatActivity {

    private int status = -2;  // status desc: -2:Initial state, not bound to ECR service, -1:Binding ECR service successful, physical channel not ready, 0:Binding ECR service successful, Physical channels are ready, 1: VSP is connected

    private final CyclicBarrier lock = new CyclicBarrier(2);
    private boolean isHandshakeCheck = false;
    private AppCompatSpinner spMode;
    private AppCompatSpinner spType;
    private EditText etNumber;
    private TextView tvMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vspstatus);
        init();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_init:
                connect(
                        (String) spMode.getAdapter().getItem(spMode.getSelectedItemPosition()),
                        (String) spType.getAdapter().getItem(spType.getSelectedItemPosition())
                );
                break;
            case R.id.btn_getVSPStatus:
                getStatus();
                break;
            case R.id.btn_close:
                close();
                break;
            case R.id.btn_sendLargeData:
                testSendLargeData();
                break;
        }
    }

    private void testSendLargeData() {
        int number = Integer.parseInt(etNumber.getText().toString());
        byte[] testData = new byte[number];
        Arrays.fill(testData, (byte) 0x11);
        ECRHelper.send(testData);
    }

    private void init() {
        etNumber = findViewById(R.id.et_number);
        tvMsg = findViewById(R.id.tv_msg);
        spMode = findViewById(R.id.sp_mode);
        spType = findViewById(R.id.sp_type);
        ECRHelper.onBindSuccess = () -> {
            ECRHelper.registerECRListener();
            status = -1;
            runOnUiThread(() -> tvMsg.setText("Binding ECR service successful, physical channel not ready"));
        };

        ECRHelper.onBindFailure = () -> {
            showToast(R.string.message_bind_ecr_service);
            status = -2;
            runOnUiThread(() -> tvMsg.setText("Initial state, not bound to ECR service"));
        };

        ECRHelper.onECRConnected = () -> {
            status = 0;
            runOnUiThread(() -> tvMsg.setText("Physical channels are ready"));
        };

        ECRHelper.onECRDisconnected = (code, message) -> {
            status = -1;
            showToast(message + " (" + code + ")");
            runOnUiThread(() -> tvMsg.setText("Binding ECR service successful, physical channel not ready"));
        };

        ECRHelper.onSendSuccess = () -> {
            runOnUiThread(() -> tvMsg.setText("Send Data Success"));
        };

        ECRHelper.onSendFailure = (code, message) -> {
            showToast(message + " (" + code + ")");
        };

        ECRHelper.onECRReceive = bytes -> {
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.equals("BOOM")) {
                if (isHandshakeCheck) {
                    // A handshake request answer is received and the VSP channel is considered to have established a connection
                    runOnUiThread(() -> tvMsg.setText("VSP Connected"));
                    status = 1;
                    try {
                        lock.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // Receive handshake request, send "BOOM" in response to handshake request
                    ECRHelper.send("BOOM".getBytes(StandardCharsets.UTF_8));
                }
            } else {
                runOnUiThread(() -> tvMsg.setText("receiver data: " + text));
            }
        };

        ECRHelper.bindECRService();
    }

    public void connect(String mode, String type) {
        if (mode.equals(ECRConstant.Mode.Bluetooth) && type.equals(ECRConstant.Type.SLAVE)) {
            switchBluetooth(mode, type);
        } else if (mode.equals(ECRConstant.Mode.WIFI)) {
            showWifiParamDialog(mode, type);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString(ECRParameters.MODE, mode);
            bundle.putString(ECRParameters.TYPE, type);
            ECRHelper.connect(bundle);
        }
    }

    private void showWifiParamDialog(String mode, String type) {
        View view = View.inflate(this, R.layout.dialog_content, null);
        EditText ip = view.findViewById(R.id.ip);
        EditText port = view.findViewById(R.id.port);
        if (App.server) {
            ip.setVisibility(View.GONE);
        }
        new AlertDialog.Builder(this)
                .setTitle("Input Data")
                .setView(view)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    if (App.server) {
                        if (TextUtils.isEmpty(port.getText().toString())) {
                            showToast("Please input port");
                            return;
                        }
                    } else {
                        if (TextUtils.isEmpty(port.getText().toString()) || TextUtils.isEmpty(ip.getText().toString())) {
                            showToast("Please input port and ip");
                            return;
                        }
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString(ECRParameters.MODE, mode);
                    bundle.putString(ECRParameters.TYPE, type);
                    bundle.putInt(ECRParameters.WIFI_PORT, Integer.parseInt(port.getText().toString()));
                    bundle.putString(ECRParameters.WIFI_ADDRESS, ip.getText().toString());
                    ECRHelper.connect(bundle);
                })
                .show();
    }

    private final HashMap<String, String> bluetoothList = new HashMap<>();

    @SuppressLint("MissingPermission")
    private void switchBluetooth(String mode, String type) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothList.clear();
                for (BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()) {
                    String name = bluetoothDevice.getName();
                    String address = bluetoothDevice.getAddress();
                    Logger.e(App.TAG, "name: " + name);
                    Logger.e(App.TAG, "address: " + address);
                    if (address.equals("00:11:22:33:44:55")) continue;
                    bluetoothList.put(name + " - " + address, address);
                    Bundle bundle = new Bundle();
                    bundle.putString(ECRParameters.MODE, mode);
                    bundle.putString(ECRParameters.TYPE, type);
                    bundle.putString(ECRParameters.BLUETOOTH_MAC_ADDRESS, address);
                    ECRHelper.connect(bundle);
                }
                if (!bluetoothList.isEmpty()) {
                    showBluetoothDialog();
                } else {
                    showToast(R.string.message_bluetooth_not_find_bonded);
                }
            } else {
                showToast(R.string.message_bluetooth_disable);
            }
        } else {
            showToast(R.string.message_bluetooth_not_support);
        }
    }

    private void showBluetoothDialog() {
        List<String> arrays = new ArrayList<>(bluetoothList.keySet());
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AlertDialog_Base);
        String[] items = arrays.toArray(new String[0]);
        builder.setTitle(R.string.dialog_select_bluetooth)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setItems(items, (dialog, which) -> {
                    String item = arrays.get(which);
                    String bluetoothAddress = bluetoothList.get(item);
                })
                .show();
    }


    private void getStatus() {
        switch (status) {
            case 0:
                ECRHelper.send("BOOM".getBytes(StandardCharsets.UTF_8));
                isHandshakeCheck = true;
                try {
                    lock.await(1000, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                    lock.reset();
                }
                break;
            case 1:
                tvMsg.setText("VSP Connected");
                break;
            default:
                tvMsg.setText("Physical channel not ready or Failed to bind ECR service，Pls check");
                break;
        }
    }

    private void close() {
        ECRHelper.disconnect();
        ;
        status = -1;
    }
}
